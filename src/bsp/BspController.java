package bsp;

import arc.Core;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import bsp.command.MoveDispatcher;
import bsp.core.cluster.ClusterSplitter;
import bsp.core.geo.GridUtils;
import bsp.core.model.Domain;
import bsp.core.model.FormationProfile;
import bsp.core.model.GridPoint;
import bsp.core.model.RouteResult;
import bsp.core.model.TileEnv;
import bsp.core.model.ThreatSource;
import bsp.core.path.CostModel;
import bsp.core.path.LiquidPolicy;
import bsp.core.path.PathPlanner;
import bsp.core.path.WaypointCompactor;
import bsp.core.power.PowerClusterFinder;
import bsp.core.threat.ThreatGrid;
import bsp.input.BspKeys;
import bsp.settings.BspSettings;
import bsp.settings.FilterMode;
import bsp.settings.TargetMode;
import bsp.state.ShownRoute;
import bsp.ui.Toasts;
import bsp.world.WorldScanner;
import mindustry.Vars;
import mindustry.gen.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side controller: key handling, plan scheduling, route bookkeeping
 * and feeding the move dispatcher. All game state flows in, decisions and
 * routes flow out to rendering/commands.
 */
public final class BspController{
    public TargetMode mode = TargetMode.NEAREST_CORE;
    public FilterMode filter = FilterMode.AUTO;
    public boolean autoMouse = false;
    public boolean autoChat = false;
    public boolean heatmap = false;
    public int[] chatTarget;

    public final List<ShownRoute> routes = new ArrayList<ShownRoute>();
    public final List<MoveDispatcher> dispatchers = new ArrayList<MoveDispatcher>();

    private final WorldScanner scanner = new WorldScanner();
    private ThreatGrid threatGrid;
    private long threatStamp = -1;
    private float nextManualPlan;
    private float nextAutoPlan;
    private boolean firstUseGuideShown;
    private AutoPlanRun spreadRun;

    private static final int MAX_CLUSTER_ROUTES = 6;

    public WorldScanner scanner(){ return scanner; }
    public ThreatGrid threatGrid(){ return threatGrid; }

    public void resetWorld(){
        routes.clear();
        for(MoveDispatcher d : dispatchers) d.clear();
        dispatchers.clear();
        scanner.reset();
        threatGrid = null;
        threatStamp = -1;
        chatTarget = null;
        autoMouse = false;
        autoChat = false;
        spreadRun = null;
    }

    /** Called every frame from Trigger.update. */
    public void update(){
        if(!BspSettings.enabled() || Vars.mobile) return;
        if(Vars.state == null || Vars.state.isMenu() || Vars.player == null) return;

        BspKeys keys = BspKeys.get();
        if(keys.cycleModeTap()){
            mode = mode.next();
            Toasts.show("bsp.toast.mode." + mode.name());
        }
        if(keys.cycleFilterTap()){
            filter = filter.next();
            Toasts.show("bsp.toast.filter." + filter.name());
        }
        if(keys.heatmapTap()){
            heatmap = !heatmap;
            Toasts.show(heatmap ? "bsp.toast.heatmap.on" : "bsp.toast.heatmap.off");
        }
        if(keys.autoMouseTap()){
            autoMouse = !autoMouse;
            autoChat = autoMouse ? false : autoChat;
            Toasts.show(autoMouse ? "bsp.toast.auto.on" : "bsp.toast.auto.off");
            if(autoMouse) nextAutoPlan = 0f; else cancelSpread();
        }
        if(keys.autoChatTap()){
            if(!autoChat && chatTarget == null){
                Toasts.show("bsp.toast.chat.empty");
            }
            autoChat = !autoChat;
            autoMouse = autoChat ? false : autoMouse;
            Toasts.show(autoChat ? "bsp.toast.auto.on" : "bsp.toast.auto.off");
            if(autoChat && chatTarget != null) nextAutoPlan = 0f; else if(!autoChat) cancelSpread();
        }
        if(keys.autoMoveTap() && (autoMouse || autoChat)){
            for(MoveDispatcher d : dispatchers) d.forceResend();
        }

        // expire shown routes
        float now = Time.time;
        for(int i = routes.size() - 1; i >= 0; i--){
            if(routes.get(i).expired(now)) routes.remove(i);
        }

        // manual preview: tap plans once, hold keeps replanning
        boolean hold = keys.anyPreviewDown();
        if(keys.previewTurretsTap() || keys.previewUnitsTap()){
            guideOnce();
            plan(keys.previewUnitsTap());
            nextManualPlan = now + BspSettings.previewInterval();
        }else if(hold && now >= nextManualPlan){
            guideOnce();
            plan(keys.previewUnitsDown());
            nextManualPlan = now + BspSettings.previewInterval();
        }

        // auto modes: continuous planning with idle slow-down; a pass may span
        // several ticks (spreadTicks) — drain it before scheduling the next one
        if(autoMouse || autoChat){
            if(spreadRun != null){
                drainSpread();
            }
            if(spreadRun == null && now >= nextAutoPlan){
                String sig = autoPlanSignature();
                boolean idle = sig.equals(lastAutoSig);
                lastAutoSig = sig;
                idleMult = idle ? BspSettings.idleSlow() : 1f;
                if(BspSettings.debugLog()){
                    // debug log category "auto": every replan and whether the
                    // formation is considered idle
                    Log.info("[bsp] auto: replan (idle=@, sig=@)", idle, sig);
                }
                plan(true);
                if(spreadRun == null){ // completed synchronously
                    float interval = Mathf.clamp(BspSettings.previewInterval() * idleMult, 0.05f, 2f);
                    nextAutoPlan = Time.time + interval;
                }
            }
        }

        for(MoveDispatcher d : dispatchers) d.update();
    }

    private String lastAutoSig = "";
    /** Idle multiplier captured when an auto pass is scheduled; used when a spread run finishes. */
    private float idleMult = 1f;

    /**
     * Signature of everything that would change an auto plan: formation
     * centroid/goal, filter, mode, threat-source count. Unchanged signature
     * means the formation is stationary and the scene is stable -> idle.
     */
    private String autoPlanSignature(){
        List<Unit> sel = selectedUnits();
        long cx = 0, cy = 0;
        for(Unit u : sel){
            cx += u.tileX();
            cy += u.tileY();
        }
        StringBuilder sb = new StringBuilder(48);
        sb.append(sel.size()).append(':')
          .append(cx / Math.max(1, sel.size())).append(',')
          .append(cy / Math.max(1, sel.size())).append(':')
          .append(filter.ordinal()).append(':').append(mode.ordinal()).append(':')
          .append(autoChat ? chatTarget == null ? -1 : chatTarget[0] * 1000 + chatTarget[1] : 0).append(':')
          .append(scanner.threatSources(true).size());
        return sb.toString();
    }

    private void guideOnce(){
        if(firstUseGuideShown) return;
        firstUseGuideShown = true;
        Toasts.show("bsp.toast.firstuse");
    }

    private void cancelSpread(){
        spreadRun = null;
    }

    /** Whether auto move is allowed right now. */
    public boolean autoMoveActive(){
        return BspSettings.autoMove() && (autoMouse || autoChat);
    }

    /**
     * One planning pass; includeUnits controls whether hostile units count as
     * threats. Auto passes with several clusters may be spread over multiple
     * ticks (spreadTicks) and finish asynchronously.
     */
    public void plan(boolean includeUnits){
        if(!BspSettings.enabled() || Vars.state == null || Vars.state.isMenu()) return;
        if(!scanner.ready()){
            return;
        }

        List<Unit> selected = selectedUnits();
        if(selected.isEmpty()){
            Toasts.show("bsp.toast.nounits");
            routes.removeIf(r -> true);
            return;
        }

        FormationProfile formation = WorldScanner.formation(selected, BspSettings.slowestBaseline());
        Domain domain = resolveDomain(formation);

        List<WorldScanner.ShapedSource> sources = scanner.threatSources(includeUnits);
        ThreatGrid grid = threatFor(sources, domain, formation);
        if(grid == null) return;

        boolean[] shieldBlocked = scanner.shieldBlocked(scanner.shieldCircles());
        TileEnv[] envs = scanner.envs();
        CostModel costs = new CostModel(
            CostModel.riskWeightFromSlider(BspSettings.caution()) * Math.max(1f, BspSettings.formationInflate()),
            formation.slowestSpeedTilesPerSec, BspSettings.floorSlowdown(), BspSettings.shortestOnly());
        LiquidPolicy liquids = new LiquidPolicy(BspSettings.deepReserve(), BspSettings.survivableLiquid());
        boolean auto = autoMouse || autoChat;

        // Auto mode keeps a clearance margin around threat envelopes; when no
        // route exists the margin is fully relaxed (bounded by the setting).
        boolean[] avoidMerged = null;
        if(auto && BspSettings.threatExpand() > 0f){
            List<ThreatSource> domainSources = new ArrayList<ThreatSource>();
            for(WorldScanner.ShapedSource s : sources){
                if(s.covers(domain)) domainSources.add(s.source);
            }
            boolean[] avoid = ThreatGrid.avoidanceMask(domainSources, inflate(formation),
                scanner.width(), scanner.height(), BspSettings.threatExpand());
            avoidMerged = or(shieldBlocked, avoid);
        }

        // clusters: each gets its own route and command stream
        List<GridPoint> positions = new ArrayList<GridPoint>(selected.size());
        for(Unit u : selected) positions.add(new GridPoint(u.tileX(), u.tileY()));
        List<List<GridPoint>> clusters = ClusterSplitter.split(positions, BspSettings.clusterDist());
        if(clusters.size() > MAX_CLUSTER_ROUTES){
            clusters = clusters.subList(0, MAX_CLUSTER_ROUTES);
        }

        float keep = BspSettings.keepSeconds();

        // Manual power-cluster mode draws one infiltration route per valuable
        // generator cluster (approach point -> centroid), not per formation.
        if(!auto && mode == TargetMode.POWER_CLUSTERS){
            List<ShownRoute> powerRoutes = new ArrayList<ShownRoute>();
            boolean anyGoal = planPowerRoutes(powerRoutes, selected, envs, grid, shieldBlocked,
                costs, liquids, formation, keep, Time.time);
            finalizePlan(powerRoutes, new ArrayList<MoveDispatcher>(), false, anyGoal);
            return;
        }

        int spread = (auto && clusters.size() > 1) ? Math.max(1, BspSettings.spreadTicks()) : 1;
        if(spread > 1){
            spreadRun = new AutoPlanRun(clusters, selected, envs, grid, shieldBlocked, avoidMerged,
                costs, liquids, formation, keep, mode);
            drainSpread();
            return;
        }

        // synchronous pass (manual, or auto with a single cluster / no spread)
        AutoPlanRun run = new AutoPlanRun(clusters, selected, envs, grid, shieldBlocked, avoidMerged,
            costs, liquids, formation, keep, mode);
        while(run.index < run.clusters.size()){
            planCluster(run);
            run.index++;
        }
        finishRun(run);
    }

    /** Plans one cluster of a run (shared by synchronous and spread passes). */
    private void planCluster(AutoPlanRun run){
        List<GridPoint> cluster = run.clusters.get(run.index);
        GridPoint start = centroid(cluster);
        List<Unit> clusterUnits = unitsNear(run.selected, cluster, BspSettings.clusterDist());
        FormationProfile clusterFormation = clusterUnits.isEmpty() ? run.formation
            : WorldScanner.formation(clusterUnits, BspSettings.slowestBaseline());

        List<GridPoint> goals = goalsFor(start);
        if(goals.isEmpty()){
            return; // no goal for this cluster; "notarget" toast only if none had any
        }
        run.anyGoals = true;

        PathPlanner planner = new PathPlanner(scanner.width(), scanner.height(), run.envs, run.grid,
            run.costs, run.liquids, clusterFormation, BspSettings.candidateRadius());

        ShownRoute.Kind kind = routeKind(run.auto());
        double bestHarm = Double.MAX_VALUE;
        RouteResult best = null;
        GridPoint bestGoal = null;
        for(GridPoint goal : goals){
            RouteResult r = planner.plan(start, goal, run.avoidMerged);
            if(r != null && r.found() && !r.goalUsed.equals(goal) && run.avoidMerged != null){
                // the strict margin forced a substitute goal — relax and retry
                RouteResult relaxed = planner.plan(start, goal, run.shieldBlocked);
                if(relaxed != null && relaxed.found()) r = relaxed;
            }
            if(r != null && r.found()){
                if(r.totalHarm < bestHarm){
                    bestHarm = r.totalHarm;
                    best = r;
                    bestGoal = goal;
                }
                // multi-core mode draws every planned core route
                if(run.mode == TargetMode.NEAREST_CORE && goals.size() > 1 && !run.auto()){
                    run.newRoutes.add(mount(r, goal, start, kind, clusterFormation, run.keep, Time.time, false));
                }
            }
        }

        if(best == null){
            if(run.auto() && BspSettings.autoMove()){
                MoveDispatcher fallback = new MoveDispatcher();
                fallback.direct(clusterUnits, goals.get(0));
                run.newDispatchers.add(fallback);
            }
            return;
        }

        run.newRoutes.add(mount(best, bestGoal, start, kind, clusterFormation, run.keep, Time.time, run.auto()));
        if(run.summary.length() > 0) run.summary.append(" / ");
        run.summary.append((int)Math.round(bestHarm));

        if(run.auto() && BspSettings.autoMove()){
            List<GridPoint> wps = WaypointCompactor.compact(best.cells, BspSettings.waypointCap(), WaypointCompactor.DEFAULT_TOLERANCE);
            MoveDispatcher d = new MoveDispatcher();
            d.feed(clusterUnits, wps);
            run.newDispatchers.add(d);
        }
    }

    /**
     * Manual power-cluster infiltration routes: one route per valuable cluster,
     * from its approach point (player side or turret side) to its centroid.
     */
    private boolean planPowerRoutes(List<ShownRoute> out, List<Unit> selected, TileEnv[] envs,
                                    ThreatGrid grid, boolean[] shieldBlocked, CostModel costs,
                                    LiquidPolicy liquids, FormationProfile formation,
                                    float keep, float now){
        List<PowerClusterFinder.Cluster> clusters = PowerClusterFinder.find(
            scanner.enemyGenerators(), BspSettings.powerLinkDist(),
            BspSettings.powerMinSize(), BspSettings.powerMaxRoutes());
        if(clusters.isEmpty()) return false;

        GridPoint playerPos = new GridPoint(selected.get(0).tileX(), selected.get(0).tileY());
        PathPlanner planner = new PathPlanner(scanner.width(), scanner.height(), envs, grid,
            costs, liquids, formation, BspSettings.candidateRadius());

        for(PowerClusterFinder.Cluster c : clusters){
            GridPoint from = PowerClusterFinder.approachPoint(c, BspSettings.powerFromPlayer(),
                playerPos, scanner.enemyTurrets(), BspSettings.powerNearTurret());
            RouteResult r = planner.plan(from, c.centroid, shieldBlocked);
            if(r == null || !r.found()) continue;
            List<GridPoint> wps = WaypointCompactor.compact(r.cells,
                Math.max(2, BspSettings.waypointCap()), WaypointCompactor.DEFAULT_TOLERANCE);
            RouteResult.RiskBand band = RouteResult.riskBand(r.totalHarm, formation.count,
                formation.weakestMaxHealth, BspSettings.safeThreshold());
            out.add(new ShownRoute(ShownRoute.Kind.POWER, wps, from, r.goalUsed, r.totalHarm,
                band, r.liquidRound, keep <= 0f ? 0f : now + keep));
        }
        return true;
    }

    /** Drains the pending spread run, one time-slice per call. */
    private void drainSpread(){
        if(spreadRun == null) return;
        int perTick = Math.max(1, (int)Math.ceil(
            (double)spreadRun.clusters.size() / Math.max(1, BspSettings.spreadTicks())));
        for(int i = 0; i < perTick && spreadRun.index < spreadRun.clusters.size(); i++){
            planCluster(spreadRun);
            spreadRun.index++;
        }
        if(spreadRun.index >= spreadRun.clusters.size()){
            AutoPlanRun done = spreadRun;
            spreadRun = null;
            finishRun(done);
            float interval = Mathf.clamp(BspSettings.previewInterval() * idleMult, 0.05f, 2f);
            nextAutoPlan = Time.time + interval;
        }
    }

    /** Publishes a finished run: routes, dispatchers and the summary toast. */
    private void finishRun(AutoPlanRun run){
        finalizePlan(run.newRoutes, run.newDispatchers, run.auto(), run.anyGoals);
    }

    private void finalizePlan(List<ShownRoute> newRoutes, List<MoveDispatcher> newDispatchers,
                              boolean auto, boolean anyGoals){
        // replace same-kind routes (keep=0 means "until next calculation")
        routes.removeIf(r -> auto ? r.kind == ShownRoute.Kind.AUTO : r.kind != ShownRoute.Kind.AUTO);
        routes.addAll(newRoutes);
        dispatchers.clear();
        dispatchers.addAll(newDispatchers);

        if(BspSettings.debugLog()){
            // debug log category "plan": what the planner produced this pass
            Log.info("[bsp] plan: mode=@ filter=@ routes=@ dispatchers=@",
                mode.name(), filter.name(), newRoutes.size(), newDispatchers.size());
            for(ShownRoute r : newRoutes){
                if(r.liquidRound >= 2){
                    // debug log category "drown": forced/unsurvivable liquid crossing
                    Log.info("[bsp] drown: route @->@ forced liquid crossing (round @), harm @",
                        r.start, r.goal, r.liquidRound, (int)Math.round(r.harm));
                }
            }
        }

        if(!newRoutes.isEmpty()){
            if(mode == TargetMode.NEAREST_CORE && !auto){
                Toasts.show("bsp.toast.cores", newRoutes.size(), minHarm(newRoutes));
            }else if(mode == TargetMode.POWER_CLUSTERS){
                Toasts.show("bsp.toast.power", newRoutes.size(), minHarm(newRoutes));
            }else if(!auto){
                Toasts.show("bsp.toast.path", newRoutes.get(0).waypoints.size(), Math.round(minHarm(newRoutes)));
            }
        }else if(anyGoals){
            Toasts.show("bsp.toast.nopath");
        }else{
            Toasts.show("bsp.toast.notarget");
        }
    }

    /** State of one planning pass; auto passes with spreadTicks > 1 span ticks. */
    private final class AutoPlanRun{
        final List<List<GridPoint>> clusters;
        final List<Unit> selected;
        final TileEnv[] envs;
        final ThreatGrid grid;
        final boolean[] shieldBlocked;
        final boolean[] avoidMerged;
        final CostModel costs;
        final LiquidPolicy liquids;
        final FormationProfile formation;
        final float keep;
        final TargetMode mode;
        final List<ShownRoute> newRoutes = new ArrayList<ShownRoute>();
        final List<MoveDispatcher> newDispatchers = new ArrayList<MoveDispatcher>();
        final StringBuilder summary = new StringBuilder();
        boolean anyGoals;
        int index;

        AutoPlanRun(List<List<GridPoint>> clusters, List<Unit> selected, TileEnv[] envs,
                    ThreatGrid grid, boolean[] shieldBlocked, boolean[] avoidMerged,
                    CostModel costs, LiquidPolicy liquids, FormationProfile formation,
                    float keep, TargetMode mode){
            this.clusters = clusters;
            this.selected = selected;
            this.envs = envs;
            this.grid = grid;
            this.shieldBlocked = shieldBlocked;
            this.avoidMerged = avoidMerged;
            this.costs = costs;
            this.liquids = liquids;
            this.formation = formation;
            this.keep = keep;
            this.mode = mode;
        }

        boolean auto(){ return autoMouse || autoChat; }
    }

    private ShownRoute mount(RouteResult r, GridPoint goal, GridPoint start, ShownRoute.Kind kind,
                             FormationProfile f, float keep, float now, boolean auto){
        List<GridPoint> wps = WaypointCompactor.compact(r.cells, Math.max(2, BspSettings.waypointCap()), WaypointCompactor.DEFAULT_TOLERANCE);
        RouteResult.RiskBand band = RouteResult.riskBand(r.totalHarm, f.count, f.weakestMaxHealth, BspSettings.safeThreshold());
        return new ShownRoute(kind, wps, start, r.goalUsed, r.totalHarm, band, r.liquidRound,
            keep <= 0f ? 0f : now + keep);
    }

    private double minHarm(List<ShownRoute> rs){
        double m = Double.MAX_VALUE;
        for(ShownRoute r : rs) m = Math.min(m, r.harm);
        return m;
    }

    private Domain resolveDomain(FormationProfile f){
        switch(filter){
            case GROUND: return Domain.GROUND;
            case AIR: return Domain.AIR;
            case MIXED: return Domain.MIXED;
            default: return f.domain();
        }
    }

    private ThreatGrid threatFor(List<WorldScanner.ShapedSource> sources, Domain domain, FormationProfile formation){
        long stamp = sources.size() * 31 + (long)(Time.time / 0.5f) + domain.ordinal() * 7919
            + formation.count * 131 + Float.floatToIntBits(formation.formationRadiusTiles);
        if(threatGrid == null || stamp != threatStamp || threatGrid.width != scanner.width() || threatGrid.height != scanner.height()){
            threatGrid = new ThreatGrid(scanner.width(), scanner.height());
            FormationProfile inflated = inflate(formation);
            for(WorldScanner.ShapedSource s : sources){
                if(!s.covers(domain)) continue;
                threatGrid.accumulate(s.source, inflated);
            }
            threatStamp = stamp;
        }
        return threatGrid;
    }

    private static FormationProfile inflate(FormationProfile f){
        return new FormationProfile(f.count, f.groundCount, f.airCount,
            f.formationRadiusTiles * Math.max(1f, BspSettings.formationInflate()),
            f.weakestMaxHealth, f.avgArmor, f.slowestSpeedTilesPerSec,
            f.minHitSizeTiles, f.anyDrownable, f.drownTimeMultiplier);
    }

    private static boolean[] or(boolean[] a, boolean[] b){
        if(a == null) return b;
        if(b == null) return a;
        boolean[] out = new boolean[a.length];
        for(int i = 0; i < a.length; i++) out[i] = a[i] || b[i];
        return out;
    }

    private ShownRoute.Kind routeKind(boolean auto){
        if(auto) return ShownRoute.Kind.AUTO;
        switch(mode){
            case POWER_CLUSTERS: return ShownRoute.Kind.POWER;
            case MOUSE: return ShownRoute.Kind.MOUSE;
            default: return ShownRoute.Kind.MANUAL;
        }
    }

    private List<GridPoint> goalsFor(GridPoint start){
        // auto modes override the manual target mode
        if(autoChat){
            GridPoint g = chatGoal();
            List<GridPoint> one = new ArrayList<GridPoint>(1);
            if(g != null) one.add(g);
            return one;
        }
        if(autoMouse){
            GridPoint target = mouseTarget();
            List<GridPoint> one = new ArrayList<GridPoint>(1);
            if(target != null) one.add(target);
            return one;
        }
        switch(mode){
            case NEAREST_CORE:
                return scanner.enemyCores(Math.max(1, BspSettings.coreCount()), start);
            case NEAREST_BUILDING:
                return scanner.enemyBuildings(1, start, null);
            case SPECIFIED_BUILDING:
                return scanner.enemyBuildings(1, start, BspSettings.targetBlock());
            case POWER_CLUSTERS:{
                // manual mode handles this up front via planPowerRoutes; auto
                // modes never reach here (mouse/chat override). Centroids serve
                // as a sane fallback for any future caller.
                List<PowerClusterFinder.Cluster> clusters = PowerClusterFinder.find(
                    scanner.enemyGenerators(), BspSettings.powerLinkDist(),
                    BspSettings.powerMinSize(), BspSettings.powerMaxRoutes());
                List<GridPoint> goals = new ArrayList<GridPoint>();
                for(PowerClusterFinder.Cluster c : clusters){
                    goals.add(c.centroid);
                }
                return goals;
            }
            case MOUSE:
            default:{
                GridPoint target = mouseTarget();
                List<GridPoint> one = new ArrayList<GridPoint>(1);
                if(target != null) one.add(target);
                return one;
            }
        }
    }

    private GridPoint mouseTarget(){
        if(!BspSettings.targetFromMouse() && bsp.ui.UiState.targetPoint() != null){
            return bsp.ui.UiState.targetPoint();
        }
        if(Vars.control == null || Vars.control.input == null || Core.input == null) return null;
        arc.math.geom.Vec2 w = Core.input.mouseWorld(Core.input.mouseX(), Core.input.mouseY());
        int x = GridUtils.clampToMap(GridUtils.worldToTile(w.x), scanner.width());
        int y = GridUtils.clampToMap(GridUtils.worldToTile(w.y), scanner.height());
        return new GridPoint(x, y);
    }

    public static List<Unit> selectedUnits(){
        List<Unit> out = new ArrayList<Unit>();
        if(Vars.control != null && Vars.control.input != null && Vars.control.input.selectedUnits.size > 0){
            for(Unit u : Vars.control.input.selectedUnits){
                if(u != null && !u.dead) out.add(u);
            }
            return out;
        }
        if(Vars.player != null && Vars.player.unit() != null && !Vars.player.unit().dead){
            out.add(Vars.player.unit());
        }
        return out;
    }

    private static GridPoint centroid(List<GridPoint> pts){
        long sx = 0, sy = 0;
        for(GridPoint p : pts){
            sx += p.x; sy += p.y;
        }
        return new GridPoint((int)Math.round(sx / (double)pts.size()), (int)Math.round(sy / (double)pts.size()));
    }

    private static List<Unit> unitsNear(List<Unit> units, List<GridPoint> cluster, double radius){
        List<Unit> out = new ArrayList<Unit>();
        for(Unit u : units){
            GridPoint p = new GridPoint(u.tileX(), u.tileY());
            for(GridPoint c : cluster){
                if(p.distSq(c) <= radius * radius){
                    out.add(u);
                    break;
                }
            }
        }
        return out;
    }

    /** Parses a chat coordinate into the auto-chat target. */
    public void onChatMessage(String message){
        if(message == null || Vars.state == null || Vars.world == null) return;
        int[] c = bsp.core.geo.ChatCoordinateParser.parseClamped(message, Vars.world.width(), Vars.world.height());
        if(c != null){
            chatTarget = c;
            if(autoChat){
                Toasts.show("bsp.toast.chat.set", c[0], c[1]);
            }
        }
    }

    /** Goal for auto-chat mode. */
    public GridPoint chatGoal(){
        return autoChat && chatTarget != null ? new GridPoint(chatTarget[0], chatTarget[1]) : null;
    }
}
