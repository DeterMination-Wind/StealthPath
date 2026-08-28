package bsp.settings;

/** Route destination modes, cycled by the mode key. */
public enum TargetMode{
    NEAREST_CORE,
    NEAREST_BUILDING,
    SPECIFIED_BUILDING,
    POWER_CLUSTERS,
    MOUSE;

    public TargetMode next(){
        TargetMode[] v = values();
        return v[(ordinal() + 1) % v.length];
    }
}
