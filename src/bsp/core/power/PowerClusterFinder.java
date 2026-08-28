package bsp.core.power;

import bsp.core.model.GridPoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Groups enemy generators into valuable clusters and picks infiltration
 * approach points. Pure logic over positions; the runtime decides which
 * buildings count as generators.
 */
public final class PowerClusterFinder{

    public static final class Cluster{
        public final List<GridPoint> cells;
        public final GridPoint centroid;

        Cluster(List<GridPoint> cells, GridPoint centroid){
            this.cells = cells;
            this.centroid = centroid;
        }

        public int size(){
            return cells.size();
        }
    }

    /**
     * Clusters generators by proximity (linkDist), keeps clusters of at least
     * minSize and returns them sorted by value (size) descending, at most maxN.
     */
    public static List<Cluster> find(List<GridPoint> generators, double linkDist, int minSize, int maxN){
        List<Cluster> all = new ArrayList<Cluster>();
        if(generators == null || generators.isEmpty() || maxN <= 0) return all;

        int n = generators.size();
        boolean[] visited = new boolean[n];
        double linkSq = linkDist * linkDist;
        Deque<Integer> queue = new ArrayDeque<Integer>();

        for(int i = 0; i < n; i++){
            if(visited[i]) continue;
            List<GridPoint> cells = new ArrayList<GridPoint>();
            long sx = 0, sy = 0;
            queue.add(i);
            visited[i] = true;
            while(!queue.isEmpty()){
                int c = queue.poll();
                GridPoint p = generators.get(c);
                cells.add(p);
                sx += p.x;
                sy += p.y;
                for(int j = 0; j < n; j++){
                    if(visited[j]) continue;
                    if(generators.get(c).distSq(generators.get(j)) <= linkSq){
                        visited[j] = true;
                        queue.add(j);
                    }
                }
            }
            if(cells.size() >= minSize){
                GridPoint centroid = new GridPoint((int)Math.round(sx / (double)cells.size()), (int)Math.round(sy / (double)cells.size()));
                all.add(new Cluster(cells, centroid));
            }
        }
        all.sort((a, b) -> Integer.compare(b.size(), a.size()));
        return all.size() > maxN ? new ArrayList<Cluster>(all.subList(0, maxN)) : all;
    }

    /**
     * Approach point for one cluster. When startFromPlayer the player position
     * is used; otherwise the point sits on the centroid towards the nearest
     * enemy turret at nearTurretDist (the "turret side" of the cluster).
     */
    public static GridPoint approachPoint(Cluster c, boolean startFromPlayer, GridPoint playerPos,
                                          List<GridPoint> enemyTurrets, double nearTurretDist){
        if(startFromPlayer) return playerPos;
        if(enemyTurrets == null || enemyTurrets.isEmpty()) return c.centroid;
        GridPoint nearest = null;
        double best = Double.MAX_VALUE;
        for(GridPoint t : enemyTurrets){
            double d = t.distSq(c.centroid);
            if(d < best){
                best = d;
                nearest = t;
            }
        }
        double dx = nearest.x - c.centroid.x, dy = nearest.y - c.centroid.y;
        double len = Math.hypot(dx, dy);
        if(len < 1e-9) return c.centroid;
        double scale = Math.min(1.0, nearTurretDist / len);
        return new GridPoint(
            (int)Math.round(c.centroid.x + dx * scale),
            (int)Math.round(c.centroid.y + dy * scale));
    }
}
