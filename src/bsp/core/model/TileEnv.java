package bsp.core.model;

/**
 * Static per-tile environment snapshot, independent of any game runtime.
 * Collected once per world by the runtime layer and cached.
 */
public final class TileEnv{
    /** Static wall/building blocks movement. */
    public final boolean solid;
    /** Floor is a liquid (water, cryo, slag...). */
    public final boolean liquid;
    /** Deterministic floor damage per second (slag etc.), already includes status damage. */
    public final float floorDamagePerSec;
    /** Speed multiplier while on this floor (water/cryo slow-down). */
    public final float speedMultiplier;
    /** Floor drown time in seconds; > 0 means a drownable liquid for ground units that can drown. */
    public final float drownTime;

    public TileEnv(boolean solid, boolean liquid, float floorDamagePerSec, float speedMultiplier, float drownTime){
        this.solid = solid;
        this.liquid = liquid;
        this.floorDamagePerSec = floorDamagePerSec;
        this.speedMultiplier = speedMultiplier;
        this.drownTime = drownTime;
    }

    public static final TileEnv EMPTY = new TileEnv(false, false, 0f, 1f, 0f);

    public boolean drownable(){
        return liquid && drownTime > 0f;
    }
}
