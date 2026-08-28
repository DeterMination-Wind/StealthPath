package bsp.core.model;

/**
 * Threat filtering domain. AUTO is resolved by the runtime into one of the
 * three concrete domains based on the currently selected formation; the pure
 * core only ever deals with concrete domains.
 */
public enum Domain{
    GROUND,
    AIR,
    MIXED;

    /** Infers the concrete domain from a formation composition. */
    public static Domain infer(int groundCount, int airCount){
        if(groundCount > 0 && airCount > 0) return MIXED;
        if(airCount > 0) return AIR;
        return GROUND;
    }
}
