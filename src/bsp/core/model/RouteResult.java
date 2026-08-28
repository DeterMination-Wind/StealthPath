package bsp.core.model;

import java.util.List;

/**
 * Result of one planned route. Harm values are whole-formation statistics;
 * per-unit harm feeds the three-color risk bands of auto mode.
 */
public final class RouteResult{
    /** Ordered tile path, null when no route was found. */
    public final List<GridPoint> cells;
    /** The goal actually used (may be a substitute candidate). */
    public final GridPoint goalUsed;
    /** Whole-formation expected HP loss along the route. */
    public final double totalHarm;
    /** Which liquid relaxation round produced this route: 0 strict, 1 survivable, 2 forced. */
    public final int liquidRound;

    public RouteResult(List<GridPoint> cells, GridPoint goalUsed, double totalHarm, int liquidRound){
        this.cells = cells;
        this.goalUsed = goalUsed;
        this.totalHarm = totalHarm;
        this.liquidRound = liquidRound;
    }

    public boolean found(){
        return cells != null;
    }

    public int length(){
        return cells == null ? 0 : cells.size();
    }

    public enum RiskBand{SAFE, WARNING, FATAL}

    /**
     * Formation-aware risk band: SAFE under the threshold, WARNING while the
     * per-unit expected loss stays below the frailest unit's health, FATAL
     * once losses are expected.
     */
    public static RiskBand riskBand(double totalHarm, int count, float weakestMaxHealth, double safeThreshold){
        if(totalHarm < safeThreshold) return RiskBand.SAFE;
        double perUnit = totalHarm / Math.max(1, count);
        return perUnit < weakestMaxHealth ? RiskBand.WARNING : RiskBand.FATAL;
    }
}
