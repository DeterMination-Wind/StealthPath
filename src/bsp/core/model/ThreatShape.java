package bsp.core.model;

/**
 * Projectile/burst shape class of a threat source. The shape drives how the
 * threat is sampled in space and how it scales against a formation.
 */
public enum ThreatShape{
    /** Plain shots that hit one target at a time. */
    DIRECT,
    /** Piercing beams/bullets that punch through several units in a line. */
    LINE,
    /** Explosive shots with area damage. */
    SPLASH,
    /** Continuous beams (laser/tractor) locked onto targets. */
    CONTINUOUS
}
