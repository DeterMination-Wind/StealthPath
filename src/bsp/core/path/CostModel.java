package bsp.core.path;

import bsp.core.model.TileEnv;

/**
 * Cell-entering cost model. Combines travel distance, threat exposure time and
 * deterministic floor damage. The player-facing "reckless <-> cautious" slider
 * maps to {@link #riskWeight}; no algorithm names leak into player settings.
 */
public final class CostModel{
    /** One HP of expected harm equals this many tiles of detour at neutral weight. */
    public static final double HARM_TILE_SCALE = 2.0;

    public final double riskWeight;
    public final float slowestSpeedTilesPerSec;
    public final boolean floorSlowdownEnabled;
    public final boolean shortestOnly;

    public CostModel(double riskWeight, float slowestSpeedTilesPerSec,
                     boolean floorSlowdownEnabled, boolean shortestOnly){
        this.riskWeight = riskWeight;
        this.slowestSpeedTilesPerSec = Math.max(0.05f, slowestSpeedTilesPerSec);
        this.floorSlowdownEnabled = floorSlowdownEnabled;
        this.shortestOnly = shortestOnly;
    }

    /**
     * Maps the player slider (0 = reckless, 50 = neutral, 100 = cautious) onto
     * a multiplicative risk weight in [0.05, 8].
     */
    public static double riskWeightFromSlider(int slider){
        int s = Math.max(0, Math.min(100, slider));
        if(s <= 50){
            return 0.05 * Math.pow(20.0, s / 50.0);
        }
        return Math.pow(8.0, (s - 50) / 50.0);
    }

    /** Time in seconds the slowest unit needs to cross one tile. */
    public double timePerCell(TileEnv env){
        float speed = slowestSpeedTilesPerSec;
        if(floorSlowdownEnabled && env.liquid){
            speed *= Math.max(0.05f, env.speedMultiplier);
        }
        return 1.0 / Math.max(0.01f, speed);
    }

    /** Whole-formation HP/s incurred on this tile (firepower + floor damage). */
    public static double harmRate(TileEnv env, float threat){
        return threat + env.floorDamagePerSec;
    }

    /** Cost of entering this tile. */
    public double cellCost(TileEnv env, float threat){
        double base = 1.0;
        if(shortestOnly) return base;
        double harm = harmRate(env, threat) * timePerCell(env);
        return base + harm * HARM_TILE_SCALE * riskWeight;
    }

    /** Expected whole-formation HP loss on this tile (statistics, not cost). */
    public double cellHarm(TileEnv env, float threat){
        return harmRate(env, threat) * timePerCell(env);
    }
}
