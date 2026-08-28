package bsp.core.model;

/**
 * Formation-aware profile of the units a route is planned for. Everything the
 * threat model and path cost model need to know about "who is walking".
 */
public final class FormationProfile{
    public final int count;
    public final int groundCount;
    public final int airCount;
    /** Actual spread radius of the formation in tiles. */
    public final float formationRadiusTiles;
    /** Max health of the frailest unit type in the formation. */
    public final float weakestMaxHealth;
    public final float avgArmor;
    /** Speed of the slowest unit, tiles per second. */
    public final float slowestSpeedTilesPerSec;
    /** Hit size of the smallest unit, in tiles (game hitSize / 8). */
    public final float minHitSizeTiles;
    /** True when any selected ground unit can drown. */
    public final boolean anyDrownable;
    /** Drown time multiplier of the frailest unit (game type.drownTimeMultiplier). */
    public final float drownTimeMultiplier;

    public FormationProfile(int count, int groundCount, int airCount,
                            float formationRadiusTiles, float weakestMaxHealth,
                            float avgArmor, float slowestSpeedTilesPerSec,
                            float minHitSizeTiles, boolean anyDrownable,
                            float drownTimeMultiplier){
        this.count = count;
        this.groundCount = groundCount;
        this.airCount = airCount;
        this.formationRadiusTiles = formationRadiusTiles;
        this.weakestMaxHealth = weakestMaxHealth;
        this.avgArmor = avgArmor;
        this.slowestSpeedTilesPerSec = slowestSpeedTilesPerSec;
        this.minHitSizeTiles = minHitSizeTiles;
        this.anyDrownable = anyDrownable;
        this.drownTimeMultiplier = drownTimeMultiplier;
    }

    public Domain domain(){
        return Domain.infer(groundCount, airCount);
    }

    /** Single-unit profile used by manual preview when nothing is selected. */
    public static FormationProfile single(boolean flying, float speedTilesPerSec){
        return new FormationProfile(1, flying ? 0 : 1, flying ? 1 : 0,
            0.6f, 100f, 0f, Math.max(0.05f, speedTilesPerSec), 1f, !flying, 1f);
    }
}
