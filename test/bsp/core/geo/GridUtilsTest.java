package bsp.core.geo;

import bsp.core.model.GridPoint;
import org.junit.Test;

import static org.junit.Assert.*;

public class GridUtilsTest{

    @Test
    public void worldTileRoundTrip(){
        assertEquals(5, GridUtils.worldToTile(5 * 8f + 3.9f));
        assertEquals(5, GridUtils.worldToTile(5 * 8f));
        assertEquals(-1, GridUtils.worldToTile(-0.1f));
        assertEquals(44f, GridUtils.tileToWorldCenter(5), 1e-6);
    }

    @Test
    public void clamping(){
        assertEquals(0, GridUtils.clamp(-5, 0, 10));
        assertEquals(10, GridUtils.clamp(15, 0, 10));
        assertEquals(199, GridUtils.clampToMap(500, 200));
        assertEquals(0, GridUtils.clampToMap(-1, 200));
    }

    @Test
    public void spiralFindsNearestPassable(){
        // blocked 3x3 area around start; first free tiles sit on ring 2
        GridPoint found = GridUtils.spiralNearest(50, 50, new GridPoint(25, 25), 10,
            (x, y) -> !(x >= 24 && x <= 26 && y >= 24 && y <= 26));
        assertNotNull(found);
        assertEquals(2, Math.max(Math.abs(found.x - 25), Math.abs(found.y - 25)));
    }

    @Test
    public void spiralReturnsNullWhenNothingMatches(){
        assertNull(GridUtils.spiralNearest(10, 10, new GridPoint(5, 5), 20, (x, y) -> false));
    }
}
