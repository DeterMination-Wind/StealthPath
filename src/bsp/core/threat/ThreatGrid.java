package bsp.core.threat;

import bsp.core.model.FormationProfile;
import bsp.core.model.GridPoint;
import bsp.core.model.ThreatSource;

import java.util.List;

/**
 * Threat field over the world grid. One float per tile, whole-formation HP/s.
 * Reused across plans; cleared and rebuilt when the threat source set changes.
 */
public final class ThreatGrid{
    public final int width, height;
    private final float[] cells;

    public ThreatGrid(int width, int height){
        this.width = width;
        this.height = height;
        this.cells = new float[width * height];
    }

    public void clear(){
        java.util.Arrays.fill(cells, 0f);
    }

    public int index(int x, int y){
        return y * width + x;
    }

    public boolean inBounds(int x, int y){
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public float get(int x, int y){
        return cells[index(x, y)];
    }

    public float get(int idx){
        return cells[idx];
    }

    public void add(int x, int y, float v){
        cells[index(x, y)] += v;
    }

    public float max(){
        float m = 0f;
        for(float v : cells) if(v > m) m = v;
        return m;
    }

    /** Stamps one threat source onto the grid, sampled for the given formation. */
    public void accumulate(ThreatSource s, FormationProfile f){
        float team = ThreatModel.teamThreatPerTile(s, f);
        if(team <= 0f) return;

        int r = (int)Math.ceil(s.rangeTiles);
        int x0 = Math.max(0, (int)Math.floor(s.tx) - r);
        int x1 = Math.min(width - 1, (int)Math.ceil(s.tx) + r);
        int y0 = Math.max(0, (int)Math.floor(s.ty) - r);
        int y1 = Math.min(height - 1, (int)Math.ceil(s.ty) + r);
        float minSq = s.minRangeTiles * s.minRangeTiles;
        float rangeSq = s.rangeTiles * s.rangeTiles;

        for(int y = y0; y <= y1; y++){
            for(int x = x0; x <= x1; x++){
                double dx = x + 0.5 - s.tx;
                double dy = y + 0.5 - s.ty;
                double dSq = dx * dx + dy * dy;
                if(dSq > rangeSq) continue;
                if(dSq < minSq) continue; // inner dead zone
                float dist = (float)Math.sqrt(dSq);
                float t = s.rangeTiles <= 0f ? 0f : dist / s.rangeTiles;
                cells[index(x, y)] += team * ThreatModel.spatialFactor(s.shape, t);
            }
        }
    }

    /** Per-source breakdown at one tile, for hover details. */
    public static float sourceThreatAt(ThreatSource s, FormationProfile f, GridPoint tile){
        double dx = tile.x + 0.5 - s.tx;
        double dy = tile.y + 0.5 - s.ty;
        return ThreatModel.cellThreat(s, f, (float)Math.hypot(dx, dy));
    }

    /**
     * Auto-mode avoidance mask: every tile some source can engage the formation
     * on, dilated by {@code marginTiles} (the threatExpand setting). Inner dead
     * zones (closer than minRange) stay open — they are the safest tiles.
     * Callers treat masked tiles as extra-blocked; when no route exists they
     * relax the whole margin, so the setting bounds how far avoidance may be
     * stretched back before falling back to pure cost weighting.
     */
    public static boolean[] avoidanceMask(List<ThreatSource> sources, FormationProfile f,
                                          int width, int height, float marginTiles){
        boolean[] mask = new boolean[width * height];
        float margin = Math.max(0f, marginTiles);
        for(ThreatSource s : sources){
            if(ThreatModel.teamThreatPerTile(s, f) <= 0f) continue;
            float outer = s.rangeTiles + margin;
            int r = (int)Math.ceil(outer);
            int x0 = Math.max(0, (int)Math.floor(s.tx) - r);
            int x1 = Math.min(width - 1, (int)Math.ceil(s.tx) + r);
            int y0 = Math.max(0, (int)Math.floor(s.ty) - r);
            int y1 = Math.min(height - 1, (int)Math.ceil(s.ty) + r);
            float minSq = s.minRangeTiles * s.minRangeTiles;
            float outerSq = outer * outer;
            for(int y = y0; y <= y1; y++){
                for(int x = x0; x <= x1; x++){
                    double dx = x + 0.5 - s.tx;
                    double dy = y + 0.5 - s.ty;
                    double dSq = dx * dx + dy * dy;
                    if(dSq > outerSq) continue;
                    if(dSq < minSq) continue; // dead zone stays open
                    mask[y * width + x] = true;
                }
            }
        }
        return mask;
    }
}
