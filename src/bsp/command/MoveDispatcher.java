package bsp.command;

import arc.util.Time;
import bsp.core.geo.GridUtils;
import bsp.core.model.GridPoint;
import bsp.settings.BspSettings;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Unit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sends native RTS move commands along planned waypoints through the game's
 * own command channel (equivalent to the player right-click commanding their
 * own selected units — nothing the server must trust). Dual throttling:
 * full-route resends and per-packet spacing; large formations are batched
 * with big units first.
 */
public final class MoveDispatcher{
    private List<Unit> units = new ArrayList<Unit>();
    private List<GridPoint> waypoints;
    private int wpIndex;
    private String lastSignature = "";
    private float lastFullSend = -999f;
    private boolean active;

    public boolean active(){ return active; }

    public void clear(){
        units.clear();
        waypoints = null;
        wpIndex = 0;
        lastSignature = "";
        active = false;
        anyDrownableChecked = false;
        anyDrownableFlag = false;
    }

    /** Replaces the current route; commands go out via update(). */
    public void feed(List<Unit> newUnits, List<GridPoint> newWaypoints){
        if(newUnits == null || newUnits.isEmpty() || newWaypoints == null || newWaypoints.isEmpty()){
            clear();
            return;
        }
        this.units = new ArrayList<Unit>(newUnits);
        this.waypoints = new ArrayList<GridPoint>(newWaypoints);
        this.wpIndex = 0;
        this.active = true;
        this.anyDrownableChecked = false;
    }

    /** Fallback: no safe route found — plain move to the goal. */
    public void direct(List<Unit> newUnits, GridPoint goal){
        List<GridPoint> single = new ArrayList<GridPoint>(1);
        single.add(goal);
        feed(newUnits, single);
    }

    /** Per-frame: advance waypoints, resend when the route materially changed. */
    public void update(){
        if(!active) return;

        pruneDead();
        if(units.isEmpty() || waypoints == null || wpIndex >= waypoints.size()){
            clear();
            return;
        }

        // advance when the leading unit is close enough to the current waypoint.
        // Before a waypoint that enters a drownable liquid segment the arrival
        // is strict (0.75 tiles) so units are not released early into the
        // water — corner-cutting there is what drowns formations at the shore.
        float arrive = BspSettings.arriveRadius() * 8f;
        GridPoint wp = waypoints.get(wpIndex);
        GridPoint next = wpIndex + 1 < waypoints.size() ? waypoints.get(wpIndex + 1) : null;
        if(next != null && anyDrownable() && drownableAt(next)){
            arrive = Math.min(arrive, 6f);
        }
        float wx = GridUtils.tileToWorldCenter(wp.x), wy = GridUtils.tileToWorldCenter(wp.y);
        for(Unit u : units){
            if(Math.abs(u.x - wx) <= arrive && Math.abs(u.y - wy) <= arrive){
                wpIndex++;
                break;
            }
        }
        if(wpIndex >= waypoints.size()) return;

        String sig = signature();
        if(!sig.equals(lastSignature)){
            sendFull(sig);
        }
    }

    private boolean anyDrownableFlag;
    private boolean anyDrownableChecked;

    /** True when any unit of this stream is a drownable ground unit. */
    private boolean anyDrownable(){
        if(!anyDrownableChecked){
            anyDrownableFlag = false;
            for(Unit u : units){
                if(u != null && !u.dead && !u.type.flying && u.type.canDrown){
                    anyDrownableFlag = true;
                    break;
                }
            }
            anyDrownableChecked = true;
        }
        return anyDrownableFlag;
    }

    /** Whether the tile at the given point is a drownable liquid floor. */
    private static boolean drownableAt(GridPoint p){
        if(Vars.world == null) return false;
        mindustry.world.Tile t = Vars.world.tile(p.x, p.y);
        return t != null && t.floor() != null && t.floor().isLiquid && t.floor().drownTime > 0f;
    }

    private void pruneDead(){
        List<Unit> alive = new ArrayList<Unit>(units.size());
        for(Unit u : units){
            if(u != null && !u.dead && u.team == Vars.player.team()) alive.add(u);
        }
        units = alive;
    }

    private String signature(){
        StringBuilder sb = new StringBuilder();
        sb.append(units.size()).append(':').append(wpIndex).append(':');
        for(int i = wpIndex; i < waypoints.size(); i++){
            GridPoint p = waypoints.get(i);
            sb.append(p.x).append(',').append(p.y).append(';');
        }
        return sb.toString();
    }

    /** Immediate dispatch of the remaining waypoints, honoring both throttles. */
    private void sendFull(String sig){
        float now = Time.time;
        if(now - lastFullSend < BspSettings.resendInterval()) return;
        lastFullSend = now;
        lastSignature = sig;

        List<Unit> ordered = new ArrayList<Unit>(units);
        ordered.sort(Comparator.comparingDouble(u -> -u.type.hitSize)); // big units first

        int[] ids = new int[ordered.size()];
        for(int i = 0; i < ids.length; i++) ids[i] = ordered.get(i).id;

        List<int[]> batches = batches(ids);
        float batchDelay = Math.max(0f, BspSettings.batchDelayMult() * 0.05f);
        float packetGap = BspSettings.packetInterval();
        final int base = wpIndex;
        if(BspSettings.debugLog()){
            // debug log category "dispatch": what goes out on the command channel
            arc.util.Log.info("[bsp] dispatch: @ units, @ waypoints from @, @ batches",
                ids.length, waypoints.size() - base, base, batches.size());
        }

        for(int bi = 0; bi < batches.size(); bi++){
            final int[] batch = batches.get(bi);
            final boolean lastBatch = bi == batches.size() - 1;
            Time.run(bi * batchDelay * 60f, () -> {
                for(int wi = base; wi < waypoints.size(); wi++){
                    final int w = wi;
                    final boolean lastPacket = lastBatch && wi == waypoints.size() - 1;
                    Time.run((wi - base) * packetGap * 60f, () -> {
                        if(Vars.player == null || Vars.state == null || Vars.state.isMenu()) return;
                        GridPoint p = waypoints.get(w);
                        Call.commandUnits(Vars.player, batch, null, null,
                            new arc.math.geom.Vec2(GridUtils.tileToWorldCenter(p.x), GridUtils.tileToWorldCenter(p.y)),
                            w > base, lastPacket);
                    });
                }
            });
        }
    }

    private List<int[]> batches(int[] ids){
        int threshold = 16;
        int size = Math.max(2, (int)(8 * Math.max(0.5f, BspSettings.batchSizeMult())));
        List<int[]> out = new ArrayList<int[]>();
        if(!BspSettings.batchEnabled() || ids.length < threshold){
            out.add(ids);
            return out;
        }
        for(int i = 0; i < ids.length; i += size){
            int end = Math.min(i + size, ids.length);
            int[] part = new int[end - i];
            System.arraycopy(ids, i, part, 0, part.length);
            out.add(part);
        }
        return out;
    }

    /** Force an immediate resend (hotkey override), bypassing the change check. */
    public void forceResend(){
        if(!active || waypoints == null) return;
        lastSignature = "";
        update();
    }
}
