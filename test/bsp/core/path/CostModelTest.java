package bsp.core.path;

import bsp.core.model.TileEnv;
import org.junit.Test;

import static org.junit.Assert.*;

public class CostModelTest{

    private static final TileEnv PLAIN = new TileEnv(false, false, 0f, 1f, 0f);
    private static final TileEnv WATER = new TileEnv(false, true, 0f, 0.5f, 0f);
    private static final TileEnv SLAG = new TileEnv(false, true, 12f, 1f, 0f);

    @Test
    public void sliderMappingEndpointsAndMonotonicity(){
        assertEquals(0.05, CostModel.riskWeightFromSlider(0), 1e-9);
        assertEquals(1.0, CostModel.riskWeightFromSlider(50), 1e-9);
        assertEquals(8.0, CostModel.riskWeightFromSlider(100), 1e-9);
        double prev = -1;
        for(int s = 0; s <= 100; s++){
            double v = CostModel.riskWeightFromSlider(s);
            assertTrue(v > prev);
            prev = v;
        }
        // out-of-range inputs clamp
        assertEquals(0.05, CostModel.riskWeightFromSlider(-10), 1e-9);
        assertEquals(8.0, CostModel.riskWeightFromSlider(999), 1e-9);
    }

    @Test
    public void threatRaisesCostCautiously(){
        CostModel neutral = new CostModel(1.0, 2f, true, false);
        double safe = neutral.cellCost(PLAIN, 0f);
        double hot = neutral.cellCost(PLAIN, 30f);
        assertEquals(1.0, safe, 1e-9);
        // harm 30 HP/s * 0.5 s/tile * scale 2 = 30 extra
        assertEquals(31.0, hot, 1e-6);
    }

    @Test
    public void recklessWeightIgnoresMostThreat(){
        CostModel reckless = new CostModel(CostModel.riskWeightFromSlider(0), 2f, true, false);
        CostModel neutral = new CostModel(1.0, 2f, true, false);
        // 30 HP/s under reckless planning adds only 1.5 tiles of cost
        assertEquals(2.5, reckless.cellCost(PLAIN, 30f), 1e-6);
        assertTrue(reckless.cellCost(PLAIN, 30f) * 5 < neutral.cellCost(PLAIN, 30f));
    }

    @Test
    public void shortestOnlyIgnoresThreatEntirely(){
        CostModel m = new CostModel(8.0, 2f, true, true);
        assertEquals(1.0, m.cellCost(PLAIN, 1000f), 1e-9);
    }

    @Test
    public void liquidSlowdownAffectsTime(){
        CostModel m = new CostModel(1.0, 2f, true, false);
        assertTrue(m.timePerCell(WATER) > m.timePerCell(PLAIN));
        CostModel noSlow = new CostModel(1.0, 2f, false, false);
        assertEquals(noSlow.timePerCell(WATER), noSlow.timePerCell(PLAIN), 1e-9);
    }

    @Test
    public void floorDamageCountsInHarmRegardlessOfWeight(){
        CostModel reckless = new CostModel(CostModel.riskWeightFromSlider(0), 1f, true, false);
        // slag: 12 dmg/s * 1s = 12 HP expected even for reckless planning
        assertEquals(12.0, reckless.cellHarm(SLAG, 0f), 1e-6);
        assertTrue(reckless.cellCost(SLAG, 0f) > reckless.cellCost(PLAIN, 0f));
    }
}
