package bsp.core.power;

import bsp.core.model.GridPoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PowerClusterFinderTest{

    private static List<GridPoint> at(int... xy){
        List<GridPoint> l = new ArrayList<GridPoint>();
        for(int i = 0; i < xy.length; i += 2) l.add(new GridPoint(xy[i], xy[i + 1]));
        return l;
    }

    @Test
    public void clustersByProximityAndMinSize(){
        List<GridPoint> gens = at(
            10, 10, 12, 10, 14, 11,          // cluster of 3
            50, 50, 51, 51,                  // cluster of 2
            80, 80                            // lone generator, below min size
        );
        List<PowerClusterFinder.Cluster> found = PowerClusterFinder.find(gens, 4, 2, 10);
        assertEquals(2, found.size());
        assertEquals(3, found.get(0).size());
        assertEquals(2, found.get(1).size());
    }

    @Test
    public void topNLimit(){
        List<GridPoint> gens = new ArrayList<GridPoint>();
        for(int c = 0; c < 5; c++){
            for(int i = 0; i < c + 2; i++) gens.add(new GridPoint(c * 100 + i, 0));
        }
        List<PowerClusterFinder.Cluster> found = PowerClusterFinder.find(gens, 4, 2, 3);
        assertEquals(3, found.size());
        // sorted by value descending
        assertTrue(found.get(0).size() >= found.get(1).size());
        assertTrue(found.get(1).size() >= found.get(2).size());
    }

    @Test
    public void approachPointOnTurretSide(){
        PowerClusterFinder.Cluster c = new PowerClusterFinder.Cluster(
            at(20, 20, 22, 20, 21, 22), new GridPoint(21, 21));
        List<GridPoint> turrets = at(100, 21); // nearest and only turret, to the east
        GridPoint ap = PowerClusterFinder.approachPoint(c, false, new GridPoint(0, 0), turrets, 10);
        // approach sits 10 tiles from the centroid towards the turret
        assertEquals(31, ap.x);
        assertEquals(21, ap.y);
    }

    @Test
    public void fromPlayerReturnsPlayer(){
        PowerClusterFinder.Cluster c = new PowerClusterFinder.Cluster(at(20, 20), new GridPoint(20, 20));
        GridPoint ap = PowerClusterFinder.approachPoint(c, true, new GridPoint(3, 4), null, 10);
        assertEquals(new GridPoint(3, 4), ap);
    }
}
