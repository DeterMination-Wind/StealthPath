package bsp.core.path;

import bsp.core.model.FormationProfile;
import bsp.core.model.GridPoint;
import bsp.core.model.RouteResult;
import bsp.core.model.TileEnv;
import bsp.core.threat.ThreatGrid;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Liquid / deep-water boundary regressions. These scenarios are the
 * historical weak spot of the route-assistant genre and must stay green.
 */
public class PathPlannerTest{

    private static final int W = 40, H = 40;

    private static TileEnv[] envs(Function3 envFn){
        TileEnv[] e = new TileEnv[W * H];
        for(int y = 0; y < H; y++)
            for(int x = 0; x < W; x++)
                e[y * W + x] = envFn.apply(x, y);
        return e;
    }

    private interface Function3{
        TileEnv apply(int x, int y);
    }

    private static FormationProfile swimmers(int count){
        // ground units that can drown, 2 tiles/s, hitSize 1 tile, frail 60 hp
        return new FormationProfile(count, count, 0, 2f, 60f, 0f, 2f, 1f, true, 1f);
    }

    private static PathPlanner planner(TileEnv[] envs, FormationProfile f, LiquidPolicy lp){
        CostModel cm = new CostModel(1.0, f.slowestSpeedTilesPerSec, true, false);
        return new PathPlanner(W, H, envs, new ThreatGrid(W, H), cm, lp, f, 24);
    }

    /**
     * Historical v5.2.2-style failure: a slag band followed by deep water
     * between the units and the goal must still produce a route.
     */
    @Test
    public void slagPlusDeepWaterBandStillPlans(){
        TileEnv[] envs = envs((x, y) -> {
            if(x >= 15 && x < 17) return new TileEnv(false, true, 10f, 1f, 0f); // slag
            if(x >= 17 && x < 22) return new TileEnv(false, true, 0f, 0.5f, 2.5f); // deep water
            return TileEnv.EMPTY;
        });
        // deep water band is 5 tiles; crossing 5 tiles at 1 tile/s (water slow)
        // takes 5s > survival 2.5s -> not survivable; planner must fall back to forced
        PathPlanner p = planner(envs, swimmers(5), new LiquidPolicy(1.5f, true));
        RouteResult r = p.plan(new GridPoint(5, 20), new GridPoint(30, 20), null);
        assertNotNull("route across slag+water must exist (forced round)", r.cells);
        assertEquals(2, r.liquidRound); // forced crossing flagged
        assertTrue(r.totalHarm > 0); // slag damage accounted
    }

    /**
     * A narrow survivable water crossing should be used instead of forcing:
     * 2 water tiles at 0.5 speed -> 4s + 1.5 reserve <= 2.5 * survival factor.
     */
    @Test
    public void survivableCrossingUsedBeforeForcing(){
        TileEnv[] envs = envs((x, y) -> {
            if(x >= 17 && x < 19) return new TileEnv(false, true, 0f, 0.5f, 30f); // wide survival window
            return TileEnv.EMPTY;
        });
        PathPlanner p = planner(envs, swimmers(5), new LiquidPolicy(1.5f, true));
        RouteResult r = p.plan(new GridPoint(5, 20), new GridPoint(30, 20), null);
        assertNotNull(r.cells);
        // direct crossing through water is far shorter than any detour: none exists
        assertEquals(1, r.liquidRound); // survivable round used
    }

    @Test
    public void strictRoundAvoidsWaterWhenDetourExists(){
        TileEnv[] envs = envs((x, y) -> {
            if(x >= 17 && x < 22 && y >= 5 && y <= 35) return new TileEnv(false, true, 0f, 0.5f, 0.5f);
            return TileEnv.EMPTY;
        });
        PathPlanner p = planner(envs, swimmers(5), new LiquidPolicy(1.5f, true));
        RouteResult r = p.plan(new GridPoint(5, 20), new GridPoint(30, 20), null);
        assertNotNull(r.cells);
        assertEquals(0, r.liquidRound); // went around
        for(GridPoint c : r.cells){
            assertFalse("strict round must avoid drownable water",
                c.x >= 17 && c.x < 22 && c.y >= 5 && c.y <= 35);
        }
    }

    @Test
    public void noSurvivableCrossingWhenDisabled(){
        TileEnv[] envs = envs((x, y) -> {
            if(x >= 17 && x < 19) return new TileEnv(false, true, 0f, 0.5f, 30f);
            return TileEnv.EMPTY;
        });
        PathPlanner p = planner(envs, swimmers(5), new LiquidPolicy(1.5f, false));
        RouteResult r = p.plan(new GridPoint(5, 20), new GridPoint(30, 20), null);
        assertNotNull(r.cells);
        assertEquals(2, r.liquidRound); // forced since survivable crossing disabled
    }

    /**
     * Goal sealed behind deep water with no detour: candidate radius must find
     * a substitute goal on the near shore instead of failing outright.
     */
    @Test
    public void liquidSealedGoalGetsSubstitute(){
        TileEnv[] envs = envs((x, y) -> {
            if(x >= 20 && x < 26 && y >= 0 && y <= 39) return new TileEnv(false, true, 0f, 0.5f, 0.1f);
            return TileEnv.EMPTY;
        });
        PathPlanner p = planner(envs, swimmers(5), new LiquidPolicy(1.5f, false));
        RouteResult r = p.plan(new GridPoint(5, 20), new GridPoint(30, 20), null);
        assertNotNull("substitute goal expected", r.cells);
        // forced round would cross anyway (round 2); accept substitute-or-forced
        assertTrue(r.goalUsed.x < 20 || r.liquidRound == 2);
    }

    @Test
    public void airFormationIgnoresDrownableLiquid(){
        TileEnv[] envs = envs((x, y) -> {
            if(x >= 17 && x < 22) return new TileEnv(false, true, 0f, 0.5f, 0.5f);
            return TileEnv.EMPTY;
        });
        FormationProfile fliers = new FormationProfile(5, 0, 5, 3f, 60f, 0f, 4f, 1f, false, 1f);
        PathPlanner p = planner(envs, fliers, new LiquidPolicy(1.5f, false));
        RouteResult r = p.plan(new GridPoint(5, 20), new GridPoint(30, 20), null);
        assertNotNull(r.cells);
        assertEquals(0, r.liquidRound);
        // straight-ish: 26+ tiles long path without water penalty detours
        assertTrue(r.length() <= 30);
    }

    @Test
    public void solidWallWithNoGapFails(){
        TileEnv[] envs = envs((x, y) -> x == 20 ? new TileEnv(true, false, 0f, 1f, 0f) : TileEnv.EMPTY);
        PathPlanner p = planner(envs, swimmers(5), new LiquidPolicy(1.5f, true));
        RouteResult r = p.plan(new GridPoint(5, 20), new GridPoint(30, 20), null);
        assertNull(r.cells);
    }

    @Test
    public void riskBandThresholds(){
        assertEquals(RouteResult.RiskBand.SAFE, RouteResult.riskBand(5, 10, 60f, 10));
        assertEquals(RouteResult.RiskBand.WARNING, RouteResult.riskBand(50, 10, 60f, 10));
        assertEquals(RouteResult.RiskBand.FATAL, RouteResult.riskBand(700, 10, 60f, 10));
    }
}
