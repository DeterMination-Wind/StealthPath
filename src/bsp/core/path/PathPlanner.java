package bsp.core.path;

import bsp.core.geo.GridUtils;
import bsp.core.model.FormationProfile;
import bsp.core.model.GridPoint;
import bsp.core.model.RouteResult;
import bsp.core.model.TileEnv;
import bsp.core.threat.ThreatGrid;

import java.util.ArrayList;
import java.util.List;

/**
 * Route planner orchestration. Liquid handling follows the spec: drownable
 * liquids are first avoided outright; when no route exists the planner
 * inspects actual crossing segments of liquid-permissive paths, blocks
 * unsurvivable segments and re-searches (a few rounds), and only then
 * substitutes the goal or forces a crossing.
 */
public final class PathPlanner{
    private final int width, height;
    private final TileEnv[] envs;
    private final ThreatGrid threat;
    private final CostModel costs;
    private final LiquidPolicy liquids;
    private final FormationProfile formation;
    private final int candidateRadius;
    private final int maxBlockRounds = 3;

    public PathPlanner(int width, int height, TileEnv[] envs, ThreatGrid threat,
                       CostModel costs, LiquidPolicy liquids, FormationProfile formation,
                       int candidateRadius){
        this.width = width;
        this.height = height;
        this.envs = envs;
        this.threat = threat;
        this.costs = costs;
        this.liquids = liquids;
        this.formation = formation;
        this.candidateRadius = candidateRadius;
    }

    private TileEnv envAt(int x, int y){
        TileEnv e = envs[y * width + x];
        return e == null ? TileEnv.EMPTY : e;
    }

    private boolean basePassable(int x, int y, boolean[] extraBlocked){
        if(x < 0 || y < 0 || x >= width || y >= height) return false;
        if(extraBlocked != null && extraBlocked[y * width + x]) return false;
        return !envAt(x, y).solid;
    }

    /**
     * Plans one route from start to goal. Returns a failed RouteResult
     * (cells == null) when nothing works, never throws.
     */
    public RouteResult plan(GridPoint start, GridPoint goal, boolean[] extraBlocked){
        GridPoint s = new GridPoint(GridUtils.clamp(start.x, 0, width - 1), GridUtils.clamp(start.y, 0, height - 1));
        GridPoint g = new GridPoint(GridUtils.clamp(goal.x, 0, width - 1), GridUtils.clamp(goal.y, 0, height - 1));

        if(!basePassable(g.x, g.y, extraBlocked)){
            GridPoint alt = GridUtils.spiralNearest(width, height, g, candidateRadius,
                (x, y) -> basePassable(x, y, extraBlocked));
            if(alt == null) return new RouteResult(null, g, 0, 0);
            g = alt;
        }

        if(!formation.anyDrownable){
            return searchWithHarm(s, g, extraBlocked, null, 0);
        }

        // Round 0: avoid every drownable liquid tile outright.
        RouteResult strict = searchWithHarm(s, g, extraBlocked, (x, y) -> envAt(x, y).drownable(), 0);
        if(strict != null) return strict;

        // Rounds 1.x: allow liquid, inspect actual crossings, block the
        // unsurvivable ones and re-search a few times.
        boolean[] blocked = new boolean[width * height];
        for(int round = 0; round < maxBlockRounds; round++){
            RouteResult r = searchWithHarm(s, g, extraBlocked,
                (x, y) -> envAt(x, y).drownable() && blocked[y * width + x], 1);
            if(r == null) break; // no detour around the blocked segments either

            List<List<GridPoint>> segments = liquidSegments(r.cells);
            boolean allSurvivable = true;
            boolean blockedAny = false;
            for(List<GridPoint> seg : segments){
                if(segmentSurvivable(seg)){
                    continue;
                }
                allSurvivable = false;
                for(GridPoint p : seg) blocked[p.y * width + p.x] = true;
                blockedAny = true;
            }
            if(allSurvivable) return r;
            if(!blockedAny) break; // nothing new to block, give up on round 1
        }

        // Round 2: goal substitution — the goal may sit inside sealed liquid.
        GridPoint alt = GridUtils.spiralNearest(width, height, g, candidateRadius,
            (x, y) -> basePassable(x, y, extraBlocked) && !envAt(x, y).drownable());
        if(alt != null && !alt.equals(g)){
            RouteResult substituted = searchWithHarm(s, alt, extraBlocked, (x, y) -> envAt(x, y).drownable(), 0);
            if(substituted != null) return substituted;
        }

        // Round 3: forced crossing — plan anyway, flagged via liquidRound 2.
        RouteResult forced = searchWithHarm(s, g, extraBlocked, null, 0);
        if(forced != null) return new RouteResult(forced.cells, forced.goalUsed, forced.totalHarm, 2);
        return new RouteResult(null, g, 0, 3);
    }

    private RouteResult searchWithHarm(GridPoint s, GridPoint g, boolean[] extraBlocked,
                                       java.util.function.BiPredicate<Integer, Integer> liquidBlocked, int roundTag){
        AStar.Passability pass = (x, y) -> basePassable(x, y, extraBlocked)
            && !(liquidBlocked != null && liquidBlocked.test(x, y));
        AStar astar = new AStar(width, height, pass,
            (x, y) -> (float)costs.cellCost(envAt(x, y), threat.get(x, y)));
        List<GridPoint> path = astar.findPath(s, g);
        if(path == null) return null;
        double harm = 0;
        for(GridPoint p : path){
            harm += costs.cellHarm(envAt(p.x, p.y), threat.get(p.x, p.y));
        }
        return new RouteResult(path, g, harm, roundTag);
    }

    /** Splits a path into its maximal contiguous drownable-liquid runs. */
    List<List<GridPoint>> liquidSegments(List<GridPoint> path){
        List<List<GridPoint>> out = new ArrayList<List<GridPoint>>();
        List<GridPoint> cur = null;
        for(GridPoint p : path){
            if(envAt(p.x, p.y).drownable()){
                if(cur == null){
                    cur = new ArrayList<GridPoint>();
                    out.add(cur);
                }
                cur.add(p);
            }else{
                cur = null;
            }
        }
        return out;
    }

    private boolean segmentSurvivable(List<GridPoint> seg){
        for(GridPoint p : seg){
            TileEnv env = envAt(p.x, p.y);
            if(!liquids.canSurvive(env, true, seg.size(), formation.slowestSpeedTilesPerSec,
                formation.minHitSizeTiles, formation.drownTimeMultiplier, costs.floorSlowdownEnabled)){
                return false;
            }
        }
        return true;
    }
}
