package bsp.world;

import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.turrets.ReloadTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousBulletType;
import bsp.core.model.Domain;
import bsp.core.model.FormationProfile;
import bsp.core.model.GridPoint;
import bsp.core.model.ThreatShape;
import bsp.core.model.ThreatSource;
import bsp.core.model.TileEnv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Translates live game state into the runtime-free core inputs: static tile
 * environments, threat sources, shield circles, formation profiles and goal
 * candidates. All approximation choices live here so the core stays testable.
 */
public final class WorldScanner{

    private TileEnv[] envs;
    private int width, height;
    private int envsStamp = -1;
    private long lastSignature = -1;
    private List<ShapedSource> lastSources;

    /** Threat source plus the flags needed for domain filtering. */
    public static final class ShapedSource{
        public final ThreatSource source;
        public final boolean targetsAir, targetsGround;

        ShapedSource(ThreatSource source, boolean targetsAir, boolean targetsGround){
            this.source = source;
            this.targetsAir = targetsAir;
            this.targetsGround = targetsGround;
        }

        public boolean covers(Domain d){
            switch(d){
                case GROUND: return targetsGround;
                case AIR: return targetsAir;
                default: return targetsAir || targetsGround;
            }
        }
    }

    public void reset(){
        envs = null;
        envsStamp = -1;
        lastSignature = -1;
        lastSources = null;
    }

    public boolean ready(){
        if(Vars.state != null && !Vars.state.isMenu() && Vars.world != null && Vars.world.tiles != null){
            // passability cache invalidates whenever the building count changes:
            // new walls/buildings turn tiles solid, destruction opens them again
            int stamp = Groups.build.size();
            if(envs == null || stamp != envsStamp
                || width != Vars.world.width() || height != Vars.world.height()){
                rebuildEnvs();
                envsStamp = stamp;
            }
        }
        return envs != null;
    }

    public TileEnv[] envs(){ return envs; }
    public int width(){ return width; }
    public int height(){ return height; }

    private void rebuildEnvs(){
        width = Vars.world.width();
        height = Vars.world.height();
        envs = new TileEnv[width * height];
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                Tile t = Vars.world.tile(x, y);
                if(t == null){
                    envs[y * width + x] = TileEnv.EMPTY;
                    continue;
                }
                envs[y * width + x] = new TileEnv(
                    t.solid(),
                    t.floor().isLiquid,
                    floorDamage(t),
                    t.floor().isLiquid ? t.floor().speedMultiplier : 1f,
                    t.floor().isLiquid ? t.floor().drownTime : 0f
                );
            }
        }
    }

    private static float floorDamage(Tile t){
        float dmg = t.floor().damageTaken;
        if(t.floor().status != null && t.floor().status.damage > 0f){
            dmg += t.floor().status.damage * 60f;
        }
        return dmg;
    }

    /**
     * Collects hostile threat sources. The result is cached against a coarse
     * signature (counts) so mouse-driven replans do not rescan every frame;
     * the caller refreshes it periodically.
     */
    public List<ShapedSource> threatSources(boolean includeUnits){
        long sig = 0;
        for(Building b : Groups.build){
            if(b.team != Vars.player.team()) sig++;
        }
        if(includeUnits){
            for(Unit u : Groups.unit){
                if(u.team != Vars.player.team() && !u.dead) sig++;
            }
        }
        if(lastSources != null && sig == lastSignature) return lastSources;

        List<ShapedSource> out = new ArrayList<ShapedSource>();
        for(Building b : Groups.build){
            if(b.team == Vars.player.team()) continue;
            if(b instanceof Turret.TurretBuild){
                ShapedSource s = turretSource((Turret.TurretBuild)b);
                if(s != null) out.add(s);
            }
        }
        if(includeUnits){
            for(Unit u : Groups.unit){
                if(u.team == Vars.player.team() || u.dead) continue;
                ShapedSource s = unitSource(u);
                if(s != null) out.add(s);
            }
        }
        lastSignature = sig;
        lastSources = out;
        return out;
    }

    private static ShapedSource turretSource(Turret.TurretBuild tb){
        Turret block = (Turret)tb.block;
        float dps = tb.estimateDps();
        if(dps <= 0f) return null;

        BulletType ammo = tb.hasAmmo() ? tb.peekAmmo() : null;
        ThreatShape shape = ThreatShape.DIRECT;
        int pierce = 1;
        float splashR = 0f;
        float statusDps = 0f;
        boolean armorPierce = false;
        if(ammo != null){
            armorPierce = ammo.pierceArmor;
            if(ammo instanceof ContinuousBulletType){
                shape = ThreatShape.CONTINUOUS;
            }else if(ammo.splashDamage > 0f){
                shape = ThreatShape.SPLASH;
                splashR = Math.max(0f, ammo.splashDamageRadius) / 8f;
            }else if(ammo.pierce || ammo.pierceCap > 1){
                shape = ThreatShape.LINE;
                pierce = ammo.pierceCap > 1 ? ammo.pierceCap : 5;
            }
            if(ammo.status != null && ammo.status.damage > 0f){
                statusDps = Math.min(ammo.status.damage * 60f, dps * 0.5f);
            }
        }
        float reload = Math.max(1f, ((ReloadTurret)block).reload);
        float shotsPerSecond = 60f / reload;

        return new ShapedSource(new ThreatSource(
            tb.x / 8d, tb.y / 8d,
            tb.range() / 8f, tb.minRange() / 8f,
            dps, statusDps, shotsPerSecond,
            shape, pierce, splashR,
            block.targetAir, block.targetGround,
            1, armorPierce, block.localizedName),
            block.targetAir, block.targetGround);
    }

    private static ShapedSource unitSource(Unit u){
        float dps = 0f;
        for(mindustry.entities.units.WeaponMount mount : u.mounts()){
            dps += mount.weapon.bullet.damage * 60f / Math.max(1f, mount.weapon.reload);
        }
        if(dps <= 0f || u.range() <= 1f) return null;

        ThreatShape shape = ThreatShape.DIRECT;
        int pierce = 1;
        float splashR = 0f;
        for(mindustry.entities.units.WeaponMount mount : u.mounts()){
            BulletType bt = mount.weapon.bullet;
            if(bt == null) continue;
            if(bt instanceof ContinuousBulletType){ shape = ThreatShape.CONTINUOUS; break; }
            if(bt.splashDamage > 0f){ shape = ThreatShape.SPLASH; splashR = Math.max(0f, bt.splashDamageRadius) / 8f; break; }
            if(bt.pierce || bt.pierceCap > 1){ shape = ThreatShape.LINE; pierce = bt.pierceCap > 1 ? bt.pierceCap : 4; break; }
        }
        return new ShapedSource(new ThreatSource(
            u.x / 8d, u.y / 8d,
            u.range() / 8f, 0f,
            dps, 0f, 1f,
            shape, pierce, splashR,
            u.type.targetAir, u.type.targetGround,
            1, false, u.type.localizedName),
            u.type.targetAir, u.type.targetGround);
    }

    /** Shield circles in tile units; formations must not path through them. */
    public List<float[]> shieldCircles(){
        List<float[]> out = new ArrayList<float[]>();
        for(Building b : Groups.build){
            if(b.team == Vars.player.team()) continue;
            if(b instanceof ForceProjector.ForceBuild){
                ForceProjector.ForceBuild fb = (ForceProjector.ForceBuild)b;
                if(!fb.broken && fb.realRadius() > 8f){
                    out.add(new float[]{b.x / 8f, b.y / 8f, fb.realRadius() / 8f});
                }
            }
        }
        return out;
    }

    public boolean[] shieldBlocked(List<float[]> circles){
        if(circles.isEmpty() || !ready()) return null;
        boolean[] blocked = new boolean[width * height];
        for(float[] c : circles){
            int r = (int)Math.ceil(c[2]);
            int x0 = Math.max(0, (int)c[0] - r), x1 = Math.min(width - 1, (int)c[0] + r);
            int y0 = Math.max(0, (int)c[1] - r), y1 = Math.min(height - 1, (int)c[1] + r);
            float rSq = c[2] * c[2];
            for(int y = y0; y <= y1; y++){
                for(int x = x0; x <= x1; x++){
                    float dx = x + 0.5f - c[0], dy = y + 0.5f - c[1];
                    if(dx * dx + dy * dy < rSq) blocked[y * width + x] = true;
                }
            }
        }
        return blocked;
    }

    /** Formation profile from the actual selected units. */
    public static FormationProfile formation(List<Unit> units, boolean slowestBaseline){
        if(units.isEmpty()) return FormationProfile.single(false, 3f);
        int count = units.size(), ground = 0, air = 0;
        float weakest = Float.MAX_VALUE, minHit = Float.MAX_VALUE, slowest = Float.MAX_VALUE;
        double armorSum = 0, weakArmorMult = 1;
        boolean anyDrown = false;
        double cx = 0, cy = 0;
        for(Unit u : units){
            if(u.type.flying) air++; else ground++;
            weakest = Math.min(weakest, u.type.health);
            minHit = Math.min(minHit, u.type.hitSize / 8f);
            slowest = Math.min(slowest, u.type.speed <= 0.01f ? 3f : u.type.speed);
            armorSum += u.type.armor;
            if(!u.type.flying && u.type.canDrown){
                anyDrown = true;
                if(u.type.health <= weakest * 1.001f) weakArmorMult = u.type.drownTimeMultiplier <= 0 ? 1f : u.type.drownTimeMultiplier;
            }
            cx += u.x; cy += u.y;
        }
        cx /= count; cy /= count;
        double spread = 0;
        float maxRadius = 0.5f;
        for(Unit u : units){
            double dx = u.x - cx, dy = u.y - cy;
            spread += dx * dx + dy * dy;
            maxRadius = Math.max(maxRadius, u.type.hitSize / 16f);
        }
        spread = Math.sqrt(spread / count);
        float radius = Math.max(0.5f, (float)spread + maxRadius);
        if(!slowestBaseline){
            // fastest baseline: average speed instead of slowest
            double avg = 0;
            for(Unit u : units) avg += Math.max(0.05f, u.type.speed);
            slowest = (float)(avg / count);
        }
        return new FormationProfile(count, ground, air, radius, weakest,
            (float)(armorSum / count), Math.max(0.1f, slowest),
            Math.max(0.25f, minHit), anyDrown, (float)Math.max(0.05, weakArmorMult));
    }

    /** Enemy cores, nearest first, at most K. */
    public List<GridPoint> enemyCores(int max, GridPoint from){
        List<GridPoint> out = new ArrayList<GridPoint>();
        for(Building b : Groups.build){
            if(b.team == Vars.player.team()) continue;
            if(b.block instanceof mindustry.world.blocks.storage.CoreBlock){
                out.add(new GridPoint(b.tile.x, b.tile.y));
            }
        }
        sortByDistance(out, from);
        return cap(out, max);
    }

    /** Enemy non-core buildings, nearest first. */
    public List<GridPoint> enemyBuildings(int max, GridPoint from, String nameFilter){
        List<GridPoint> out = new ArrayList<GridPoint>();
        for(Building b : Groups.build){
            if(b.team == Vars.player.team()) continue;
            if(b.block instanceof mindustry.world.blocks.storage.CoreBlock) continue;
            if(nameFilter != null && !nameFilter.isEmpty() && !b.block.localizedName.equals(nameFilter)) continue;
            out.add(new GridPoint(b.tile.x, b.tile.y));
        }
        sortByDistance(out, from);
        return cap(out, Math.max(1, max));
    }

    /** Enemy generator positions (low-value single generators excluded). */
    public List<GridPoint> enemyGenerators(){
        List<GridPoint> out = new ArrayList<GridPoint>();
        for(Building b : Groups.build){
            if(b.team == Vars.player.team()) continue;
            if(!(b.block instanceof PowerGenerator)) continue;
            String n = b.block.getClass().getSimpleName();
            if(n.contains("Combustion") || n.contains("Steam") || n.contains("TurbineCondenser") || n.contains("Solar")) continue;
            out.add(new GridPoint(b.tile.x, b.tile.y));
        }
        return out;
    }

    /** Enemy turret positions, for power cluster approach points. */
    public List<GridPoint> enemyTurrets(){
        List<GridPoint> out = new ArrayList<GridPoint>();
        for(Building b : Groups.build){
            if(b.team == Vars.player.team()) continue;
            if(b instanceof Turret.TurretBuild) out.add(new GridPoint(b.tile.x, b.tile.y));
        }
        return out;
    }

    public static void sortByDistance(List<GridPoint> pts, final GridPoint from){
        pts.sort(Comparator.comparingDouble(p -> p.distSq(from)));
    }

    private static <T> List<T> cap(List<T> list, int max){
        return list.size() > max ? new ArrayList<T>(list.subList(0, max)) : list;
    }
}
