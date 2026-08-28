package bsp.state;

import bsp.core.model.GridPoint;
import bsp.core.model.RouteResult;

import java.util.List;

/** One route currently shown on screen. */
public final class ShownRoute{
    public enum Kind{MANUAL, POWER, MOUSE, AUTO}

    public final Kind kind;
    public final List<GridPoint> waypoints;
    public final GridPoint start, goal;
    public final double harm;
    public final RouteResult.RiskBand band;
    public final int liquidRound;
    /** Expiry in Time.time seconds; 0 = keep until the next calculation. */
    public final float expireAt;

    public ShownRoute(Kind kind, List<GridPoint> waypoints, GridPoint start, GridPoint goal,
                      double harm, RouteResult.RiskBand band, int liquidRound, float expireAt){
        this.kind = kind;
        this.waypoints = waypoints;
        this.start = start;
        this.goal = goal;
        this.harm = harm;
        this.band = band;
        this.liquidRound = liquidRound;
        this.expireAt = expireAt;
    }

    public boolean expired(float now){
        return expireAt > 0f && now >= expireAt;
    }
}
