package bsp.core.model;

/**
 * A single hostile threat emitter (turret or unit), already reduced to the
 * numbers the pure threat model needs. The runtime layer is responsible for
 * deriving these from game objects; this class stays runtime-free.
 */
public final class ThreatSource{
    /** Emitter position in tile coordinates (may be fractional). */
    public final double tx, ty;
    public final float rangeTiles;
    /** Inner dead zone; closer than this the emitter cannot engage. */
    public final float minRangeTiles;
    /** Single-target DPS including coolant/efficiency corrections. */
    public final float baseDps;
    /** Extra status damage per second (burning etc.), single-target scale. */
    public final float statusDps;
    /** Hits per second, used for flat armor mitigation. */
    public final float shotsPerSecond;
    public final ThreatShape shape;
    /** Units a LINE shot punches through simultaneously. */
    public final int pierce;
    /** Splash radius in tiles for SPLASH shape. */
    public final float splashRadiusTiles;
    public final boolean targetsAir, targetsGround;
    /** Targets one burst can engage simultaneously (swarm-style turrets). */
    public final int simultaneousTargets;
    public final boolean armorPierce;
    /** Display label for hover details; purely informational. */
    public final String label;

    public ThreatSource(double tx, double ty, float rangeTiles, float minRangeTiles,
                        float baseDps, float statusDps, float shotsPerSecond,
                        ThreatShape shape, int pierce, float splashRadiusTiles,
                        boolean targetsAir, boolean targetsGround,
                        int simultaneousTargets, boolean armorPierce, String label){
        this.tx = tx;
        this.ty = ty;
        this.rangeTiles = rangeTiles;
        this.minRangeTiles = minRangeTiles;
        this.baseDps = baseDps;
        this.statusDps = statusDps;
        this.shotsPerSecond = shotsPerSecond;
        this.shape = shape;
        this.pierce = pierce;
        this.splashRadiusTiles = splashRadiusTiles;
        this.targetsAir = targetsAir;
        this.targetsGround = targetsGround;
        this.simultaneousTargets = simultaneousTargets;
        this.armorPierce = armorPierce;
        this.label = label;
    }

    /** Convenience factory for a plain single-target direct turret. */
    public static ThreatSource direct(double tx, double ty, float rangeTiles, float dps, boolean air, boolean ground){
        return new ThreatSource(tx, ty, rangeTiles, 0f, dps, 0f, 1f, ThreatShape.DIRECT, 1, 0f, air, ground, 1, false, "turret");
    }

    public static ThreatSource splash(double tx, double ty, float rangeTiles, float dps, float splashRadiusTiles){
        return new ThreatSource(tx, ty, rangeTiles, 0f, dps, 0f, 0.5f, ThreatShape.SPLASH, 1, splashRadiusTiles, false, true, 1, false, "splash");
    }

    public static ThreatSource continuous(double tx, double ty, float rangeTiles, float dps){
        return new ThreatSource(tx, ty, rangeTiles, 0f, dps, 0f, 12f, ThreatShape.CONTINUOUS, 1, 0f, true, true, 1, false, "beam");
    }
}
