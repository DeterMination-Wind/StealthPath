package bsp.core.path;

import bsp.core.model.GridPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid A* over 8 directions with octile heuristic. Passability and cell cost
 * are supplied by callbacks so the same search serves every liquid-blocking
 * round and target candidate without copying grids.
 */
public final class AStar{
    private static final float SQRT2 = 1.41421356f;
    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DY = {0, 0, 1, -1, 1, -1, 1, -1};

    public interface Passability{
        boolean passable(int x, int y);
    }

    public interface Costs{
        float cost(int x, int y);
    }

    private final int width, height;
    private final Passability pass;
    private final Costs costs;

    public AStar(int width, int height, Passability pass, Costs costs){
        this.width = width;
        this.height = height;
        this.pass = pass;
        this.costs = costs;
    }

    /** Returns the tile path from start (inclusive) to goal (inclusive), or null. */
    public List<GridPoint> findPath(GridPoint start, GridPoint goal){
        if(!inBounds(start) || !inBounds(goal)) return null;
        if(!pass.passable(goal.x, goal.y) || !pass.passable(start.x, start.y)) return null;
        if(start.equals(goal)){
            List<GridPoint> single = new ArrayList<GridPoint>(1);
            single.add(start);
            return single;
        }

        int n = width * height;
        float[] g = new float[n];
        int[] came = new int[n];
        char[] closed = new char[n];
        IntHeap open = new IntHeap(n);
        java.util.Arrays.fill(came, -1);

        int s = start.y * width + start.x;
        int t = goal.y * width + goal.x;
        g[s] = 0f;
        open.push(s, heuristic(start, goal));

        while(!open.isEmpty()){
            int cur = open.pop();
            if(cur == t){
                return reconstruct(came, cur);
            }
            if(closed[cur] != 0) continue;
            closed[cur] = 1;

            int cx = cur % width, cy = cur / width;
            for(int d = 0; d < 8; d++){
                int nx = cx + DX[d], ny = cy + DY[d];
                if(!inBounds(nx, ny)) continue;
                if(!pass.passable(nx, ny)) continue;
                int ni = ny * width + nx;
                if(closed[ni] != 0) continue;

                boolean diag = d >= 4;
                if(diag){
                    // no corner cutting through two blockers
                    if(!pass.passable(cx + DX[d], cy) || !pass.passable(cx, cy + DY[d])) continue;
                }
                float step = (diag ? SQRT2 : 1f) * Math.max(0.01f, costs.cost(nx, ny));
                float ng = g[cur] + step;
                if(came[ni] < 0 || ng < g[ni]){
                    g[ni] = ng;
                    came[ni] = cur;
                    float h = heuristic(nx, ny, goal.x, goal.y);
                    open.push(ni, ng + h);
                }
            }
        }
        return null;
    }

    private List<GridPoint> reconstruct(int[] came, int cur){
        List<GridPoint> out = new ArrayList<GridPoint>();
        while(cur >= 0){
            out.add(new GridPoint(cur % width, cur / width));
            cur = came[cur];
        }
        java.util.Collections.reverse(out);
        return out;
    }

    private boolean inBounds(GridPoint p){
        return p.x >= 0 && p.y >= 0 && p.x < width && p.y < height;
    }

    private boolean inBounds(int x, int y){
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private float heuristic(GridPoint a, GridPoint b){
        return heuristic(a.x, a.y, b.x, b.y);
    }

    private static float heuristic(int x, int y, int gx, int gy){
        int dx = Math.abs(x - gx), dy = Math.abs(y - gy);
        return (dx + dy) + (SQRT2 - 2f) * Math.min(dx, dy);
    }

    /** Minimal binary heap over packed (index, priority) pairs. */
    static final class IntHeap{
        private int[] tree;
        private float[] prio;
        private int size;

        IntHeap(int capacity){
            int cap = Math.max(16, Math.min(capacity, 1 << 21));
            tree = new int[cap];
            prio = new float[cap];
        }

        boolean isEmpty(){
            return size == 0;
        }

        void push(int value, float priority){
            if(size >= tree.length){
                int[] nt = new int[tree.length * 2];
                float[] np = new float[prio.length * 2];
                System.arraycopy(tree, 0, nt, 0, tree.length);
                System.arraycopy(prio, 0, np, 0, prio.length);
                tree = nt;
                prio = np;
            }
            int i = size++;
            tree[i] = value;
            prio[i] = priority;
            while(i > 0){
                int p = (i - 1) >>> 1;
                if(prio[p] <= prio[i]) break;
                swap(p, i);
                i = p;
            }
        }

        int pop(){
            int top = tree[0];
            size--;
            if(size > 0){
                tree[0] = tree[size];
                prio[0] = prio[size];
                int i = 0;
                while(true){
                    int l = 2 * i + 1, r = l + 1, m = i;
                    if(l < size && prio[l] < prio[m]) m = l;
                    if(r < size && prio[r] < prio[m]) m = r;
                    if(m == i) break;
                    swap(i, m);
                    i = m;
                }
            }
            return top;
        }

        private void swap(int a, int b){
            int tv = tree[a];
            tree[a] = tree[b];
            tree[b] = tv;
            float tp = prio[a];
            prio[a] = prio[b];
            prio[b] = tp;
        }
    }
}
