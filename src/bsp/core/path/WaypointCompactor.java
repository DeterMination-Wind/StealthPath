package bsp.core.path;

import bsp.core.model.GridPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Path-to-waypoint compaction: Ramer-Douglas-Peucker simplification followed
 * by a hard cap. Long lines collapse into two points; only turning points
 * survive; renderers and the move dispatcher share the same output.
 */
public final class WaypointCompactor{
    /** Default simplification tolerance in tiles. */
    public static final double DEFAULT_TOLERANCE = 0.8;

    public static List<GridPoint> compact(List<GridPoint> path, int maxWaypoints, double toleranceTiles){
        if(path == null) return null;
        if(path.size() <= 2 || maxWaypoints >= path.size()) return new ArrayList<GridPoint>(path);

        List<GridPoint> simplified = rdp(path, Math.max(0.1, toleranceTiles));
        if(simplified.size() <= maxWaypoints) return simplified;
        return thin(simplified, maxWaypoints);
    }

    /** Ramer-Douglas-Peucker over grid points treated as polyline vertices. */
    static List<GridPoint> rdp(List<GridPoint> pts, double epsilon){
        boolean[] keep = new boolean[pts.size()];
        keep[0] = true;
        keep[pts.size() - 1] = true;
        rdpRec(pts, 0, pts.size() - 1, epsilon, keep);
        List<GridPoint> out = new ArrayList<GridPoint>();
        for(int i = 0; i < pts.size(); i++){
            if(keep[i]) out.add(pts.get(i));
        }
        return out;
    }

    private static void rdpRec(List<GridPoint> pts, int lo, int hi, double epsilon, boolean[] keep){
        if(hi <= lo + 1) return;
        GridPoint a = pts.get(lo), b = pts.get(hi);
        double maxDist = -1;
        int idx = -1;
        for(int i = lo + 1; i < hi; i++){
            double d = perpendicularDistance(pts.get(i), a, b);
            if(d > maxDist){
                maxDist = d;
                idx = i;
            }
        }
        if(maxDist > epsilon){
            keep[idx] = true;
            rdpRec(pts, lo, idx, epsilon, keep);
            rdpRec(pts, idx, hi, epsilon, keep);
        }
    }

    static double perpendicularDistance(GridPoint p, GridPoint a, GridPoint b){
        double dx = b.x - a.x, dy = b.y - a.y;
        double lenSq = dx * dx + dy * dy;
        if(lenSq < 1e-12){
            return p.dist(a);
        }
        double cross = (p.x - a.x) * dy - (p.y - a.y) * dx;
        return Math.abs(cross) / Math.sqrt(lenSq);
    }

    /** Uniformly thins the list down to at most cap points, keeping first and last. */
    static List<GridPoint> thin(List<GridPoint> pts, int cap){
        if(pts.size() <= cap) return new ArrayList<GridPoint>(pts);
        List<GridPoint> out = new ArrayList<GridPoint>(cap);
        double step = (double)(pts.size() - 1) / (cap - 1);
        for(int i = 0; i < cap; i++){
            out.add(pts.get((int)Math.round(i * step)));
        }
        out.set(cap - 1, pts.get(pts.size() - 1));
        return out;
    }
}
