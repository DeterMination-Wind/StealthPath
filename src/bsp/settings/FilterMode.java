package bsp.settings;

/**
 * Threat filter modes. AUTO re-infers the domain from the currently selected
 * formation on every plan (the "better version" default); the others are
 * manual overrides.
 */
public enum FilterMode{
    AUTO,
    GROUND,
    AIR,
    MIXED;

    public FilterMode next(){
        FilterMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
