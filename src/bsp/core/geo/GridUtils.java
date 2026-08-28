package bsp.core.geo;

import bsp.core.model.GridPoint;

/**
 * Tile/world coordinate conversions and map-edge clamping. Mindustry uses
 * 8 world units per tile; tile centers sit at tile*8 + 4.
 */
public final class GridUtils{
    public static final float TILE_SIZE = 8f;

    private GridUtils(){}

    public static int worldToTile(float world){
        return (int)Math.floor(world / TILE_SIZE);
    }

    public static float tileToWorldCenter(int tile){
        return tile * TILE_SIZE + TILE_SIZE / 2f;
    }

    public static int clamp(int v, int min, int max){
        return v < min ? min : Math.min(v, max);
    }

    public static int clampToMap(int v, int size){
        return clamp(v, 0, Math.max(0, size - 1));
    }

    /**
     * Spiral search for the nearest tile satisfying the predicate within
     * maxRadius rings (Chebyshev). Returns the start itself when it already
     * satisfies, or null when nothing matched.
     */
    public static GridPoint spiralNearest(int width, int height, GridPoint start, int maxRadius, java.util.function.BiPredicate<Integer, Integer> passable){
        int sx = clamp(start.x, 0, width - 1), sy = clamp(start.y, 0, height - 1);
        if(passable.test(sx, sy)) return new GridPoint(sx, sy);
        int maxR = Math.min(maxRadius, Math.max(width, height));
        for(int r = 1; r <= maxR; r++){
            for(int dy = -r; dy <= r; dy++){
                for(int dx = -r; dx <= r; dx++){
                    if(Math.max(Math.abs(dx), Math.abs(dy)) != r) continue; // ring only
                    int x = sx + dx, y = sy + dy;
                    if(x < 0 || y < 0 || x >= width || y >= height) continue;
                    if(passable.test(x, y)) return new GridPoint(x, y);
                }
            }
        }
        return null;
    }
}
