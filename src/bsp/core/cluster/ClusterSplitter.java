package bsp.core.cluster;

import bsp.core.model.GridPoint;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Splits selected units into formation clusters by proximity so each cluster
 * gets its own route, drawing and command stream (keeps formations coherent).
 */
public final class ClusterSplitter{

    /**
     * Greedy connectivity clustering: two points within maxDist belong to the
     * same cluster. O(n^2) is fine for selection sizes.
     */
    public static List<List<GridPoint>> split(List<GridPoint> points, double maxDist){
        List<List<GridPoint>> out = new ArrayList<List<GridPoint>>();
        if(points == null || points.isEmpty()) return out;

        int n = points.size();
        boolean[] assigned = new boolean[n];
        double maxSq = maxDist * maxDist;
        Deque<Integer> queue = new ArrayDeque<Integer>();

        for(int i = 0; i < n; i++){
            if(assigned[i]) continue;
            List<GridPoint> cluster = new ArrayList<GridPoint>();
            queue.add(i);
            assigned[i] = true;
            while(!queue.isEmpty()){
                int c = queue.poll();
                cluster.add(points.get(c));
                for(int j = 0; j < n; j++){
                    if(assigned[j]) continue;
                    if(points.get(c).distSq(points.get(j)) <= maxSq){
                        assigned[j] = true;
                        queue.add(j);
                    }
                }
            }
            out.add(cluster);
        }
        // largest cluster first for stable ordering
        out.sort((a, b) -> Integer.compare(b.size(), a.size()));
        return out;
    }
}
