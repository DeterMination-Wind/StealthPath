package bsp.core.cluster;

import bsp.core.model.GridPoint;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ClusterSplitterTest{

    @Test
    public void twoDistantGroupsSplit(){
        List<GridPoint> pts = new java.util.ArrayList<GridPoint>();
        for(int i = 0; i < 4; i++) pts.add(new GridPoint(i, 0));
        for(int i = 0; i < 3; i++) pts.add(new GridPoint(100 + i, 0));
        List<List<GridPoint>> clusters = ClusterSplitter.split(pts, 5);
        assertEquals(2, clusters.size());
        assertEquals(4, clusters.get(0).size());
        assertEquals(3, clusters.get(1).size());
    }

    @Test
    public void chainedClosePointsStayTogether(){
        List<GridPoint> pts = new java.util.ArrayList<GridPoint>();
        for(int i = 0; i < 10; i++) pts.add(new GridPoint(i * 4, 0));
        List<List<GridPoint>> clusters = ClusterSplitter.split(pts, 5);
        assertEquals(1, clusters.size());
        assertEquals(10, clusters.get(0).size());
    }

    @Test
    public void singlePointAndEmpty(){
        List<GridPoint> one = new java.util.ArrayList<GridPoint>();
        one.add(new GridPoint(3, 3));
        assertEquals(1, ClusterSplitter.split(one, 5).size());
        assertTrue(ClusterSplitter.split(new java.util.ArrayList<GridPoint>(), 5).isEmpty());
    }
}
