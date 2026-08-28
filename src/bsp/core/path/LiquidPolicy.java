package bsp.core.path;

import bsp.core.model.TileEnv;

/**
 * Drowning / survivable-crossing policy for drownable liquids (deep water and
 * any other floor with drownTime > 0). The planner first blocks such tiles
 * outright; only when no route exists may segments be released if the whole
 * formation can cross them inside their survival window with a reserve.
 */
public final class LiquidPolicy{
    public final float reserveSeconds;
    public final boolean allowSurvivableCrossing;

    public LiquidPolicy(float reserveSeconds, boolean allowSurvivableCrossing){
        this.reserveSeconds = reserveSeconds;
        this.allowSurvivableCrossing = allowSurvivableCrossing;
    }

    /**
     * Seconds the frailest unit of the formation survives on a drownable
     * liquid floor. Mirrors the game formula: hitSize/8 * multiplier * drownTime.
     */
    public static float survivalSeconds(TileEnv env, float minHitSizeTiles, float drownTimeMultiplier){
        if(!env.drownable()) return Float.MAX_VALUE;
        return minHitSizeTiles * Math.max(0.05f, drownTimeMultiplier) * env.drownTime;
    }

    /** Seconds needed to cross a continuous drownable segment. */
    public static float crossingSeconds(int segmentTiles, float speedTilesPerSec, TileEnv env, boolean floorSlowdownEnabled){
        float speed = Math.max(0.05f, speedTilesPerSec);
        if(floorSlowdownEnabled && env.liquid){
            speed *= Math.max(0.05f, env.speedMultiplier);
        }
        return segmentTiles / speed;
    }

    /** True when the segment is blocked outright (no survivable crossing possible). */
    public boolean mustBlock(TileEnv env, boolean anyDrownable){
        return anyDrownable && env.drownable() && !allowSurvivableCrossing;
    }

    /**
     * True when the formation may wade through a continuous drownable segment:
     * crossing time plus the reserve stays within the survival window.
     */
    public boolean canSurvive(TileEnv env, boolean anyDrownable,
                              int segmentTiles, float speedTilesPerSec,
                              float minHitSizeTiles, float drownTimeMultiplier,
                              boolean floorSlowdownEnabled){
        if(!anyDrownable || !env.drownable()) return true;
        if(!allowSurvivableCrossing) return false;
        float cross = crossingSeconds(segmentTiles, speedTilesPerSec, env, floorSlowdownEnabled);
        float survive = survivalSeconds(env, minHitSizeTiles, drownTimeMultiplier);
        return cross + reserveSeconds <= survive;
    }
}
