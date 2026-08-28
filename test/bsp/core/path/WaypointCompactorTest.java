package bsp.core.path;

import bsp.core.model.GridPoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class WaypointCompactorTest{

    private static List<GridPoint> line(int n, boolean horizontal){
        List<GridPoint> p = new ArrayList<GridPoint>();
        for(int i = 0; i < n; i++) p.add(horizontal ? new GridPoint(i, 0) : new GridPoint(0, i));
        return p;
    }

    @Test
    public void straightLineBecomesTwoWaypoints(){
        List<GridPoint> out = WaypointCompactor.compact(line(25, true), 12, WaypointCompactor.DEFAULT_TOLERANCE);
        assertEquals(2, out.size());
        assertEquals(new GridPoint(0, 0), out.get(0));
        assertEquals(new GridPoint(24, 0), out.get(1));
    }

    @Test
    public void elbowKeepsCorner(){
        List<GridPoint> p = new ArrayList<GridPoint>();
        for(int x = 0; x <= 10; x++) p.add(new GridPoint(x, 0));
        for(int y = 1; y <= 10; y++) p.add(new GridPoint(10, y));
        List<GridPoint> out = WaypointCompactor.compact(p, 12, WaypointCompactor.DEFAULT_TOLERANCE);
        assertEquals(3, out.size());
        assertEquals(new GridPoint(10, 0), out.get(1));
    }

    @Test
    public void capEnforcedAndEndpointsKept(){
        List<GridPoint> zigzag = new ArrayList<GridPoint>();
        for(int i = 0; i < 40; i++) zigzag.add(new GridPoint(i, (i % 2) * 3));
        List<GridPoint> out = WaypointCompactor.compact(zigzag, 5, 0.1);
        assertTrue(out.size() <= 5);
        assertEquals(zigzag.get(0), out.get(0));
        assertEquals(zigzag.get(zigzag.size() - 1), out.get(out.size() - 1));
        // all waypoints must come from the original path
        assertTrue(zigzag.containsAll(out));
    }

    @Test
    public void shortPathReturnedAsIs(){
        List<GridPoint> p = line(2, true);
        assertEquals(2, WaypointCompactor.compact(p, 12, 0.8).size());
        assertNull(WaypointCompactor.compact(null, 12, 0.8));
    }

    @Test
    public void gentleCurveKeepsFewerPointsThanTiles(){
        List<GridPoint> arc = new ArrayList<GridPoint>();
        for(int i = 0; i <= 60; i++) arc.add(new GridPoint(i, (int)Math.round(Math.sin(i * 0.2) * 8)));
        List<GridPoint> out = WaypointCompactor.compact(arc, 60, 1.2);
        assertTrue(out.size() < arc.size() / 2);
    }
}
