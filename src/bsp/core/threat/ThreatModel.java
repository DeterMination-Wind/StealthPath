package bsp.core.threat;

import bsp.core.model.FormationProfile;
import bsp.core.model.ThreatShape;
import bsp.core.model.ThreatSource;

/**
 * Formation-aware threat sampling formulas. All values are "whole formation
 * HP per second" — the external contract stays one threat value per tile, the
 * derivation accounts for formation size, projectile shape, hit probability,
 * multi-target caps and flat armor mitigation.
 *
 * Model summary:
 *  - engaged: how many formation units this source can hurt simultaneously,
 *    capped by the source's simultaneous/splash/pierce limits and by how many
 *    units it can actually target (air/ground).
 *  - armor: flat mitigation per hit, floored at 25% of raw DPS.
 *  - spatial: hit-probability falloff towards max range, per shape.
 */
public final class ThreatModel{
    private ThreatModel(){}

    /** Armor mitigation floor: never reduce below 25% of raw DPS. */
    public static final float ARMOR_FLOOR = 0.25f;

    /** Whole-formation HP/s this source deals when the formation sits in range. */
    public static float teamThreatPerTile(ThreatSource s, FormationProfile f){
        int targetable = (s.targetsAir ? f.airCount : 0) + (s.targetsGround ? f.groundCount : 0);
        if(targetable <= 0) return 0f;

        double dps = s.baseDps + s.statusDps * 0.5;
        if(dps <= 0) return 0f;

        if(!s.armorPierce && s.shotsPerSecond > 0 && f.avgArmor > 0){
            dps = Math.max(dps * ARMOR_FLOOR, dps - (double)f.avgArmor * s.shotsPerSecond);
        }

        int engaged = engagedTargets(s, f, targetable);
        if(engaged <= 0) return 0f;
        return (float)(dps * engaged);
    }

    /** How many units of the formation one burst of this source engages. */
    static int engagedTargets(ThreatSource s, FormationProfile f, int targetable){
        switch(s.shape){
            case SPLASH:{
                // A tight formation eats full splash; a spread one only partially.
                float denom = Math.max(f.formationRadiusTiles, 0.5f);
                float coverage = clamp(s.splashRadiusTiles / denom, 0f, 1f);
                int bySplash = (int)Math.ceil(coverage * targetable);
                return Math.min(targetable, Math.max(s.simultaneousTargets, bySplash));
            }
            case LINE:{
                return Math.min(targetable, Math.max(s.simultaneousTargets, s.pierce));
            }
            case CONTINUOUS:{
                int sim = Math.max(1, s.simultaneousTargets);
                return Math.min(targetable, sim);
            }
            case DIRECT:
            default:{
                return Math.min(targetable, Math.max(1, s.simultaneousTargets));
            }
        }
    }

    /** Spatial hit-probability factor in [0,1]; t = dist / range in [0,1]. */
    static float spatialFactor(ThreatShape shape, float t){
        switch(shape){
            case DIRECT: return 1f - 0.40f * t * t;
            case SPLASH: return 1f - 0.20f * t * t;
            case LINE:
            case CONTINUOUS:
            default: return 1f - 0.10f * t * t;
        }
    }

    /**
     * Threat contribution of source s to the tile whose center is dist tiles
     * away from the emitter, for formation f. Zero outside [minRange, range].
     */
    public static float cellThreat(ThreatSource s, FormationProfile f, float dist){
        if(dist > s.rangeTiles || dist < s.minRangeTiles) return 0f;
        float team = teamThreatPerTile(s, f);
        if(team <= 0f) return 0f;
        float t = s.rangeTiles <= 0f ? 0f : dist / s.rangeTiles;
        return team * spatialFactor(s.shape, t);
    }

    static float clamp(float v, float min, float max){
        return v < min ? min : Math.min(v, max);
    }
}
