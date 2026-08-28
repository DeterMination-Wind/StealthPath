package bsp.core.path;

import bsp.core.model.TileEnv;
import org.junit.Test;

import static org.junit.Assert.*;

public class LiquidPolicyTest{

    private static final TileEnv DEEP_WATER = new TileEnv(false, true, 0f, 0.5f, 2.5f);
    private static final TileEnv SHALLOW = new TileEnv(false, true, 0f, 0.7f, 0f);

    @Test
    public void survivalMirrorsGameFormula(){
        // hitSize 2 tiles, multiplier 1, drownTime 2.5 -> 5 seconds
        assertEquals(5f, LiquidPolicy.survivalSeconds(DEEP_WATER, 2f, 1f), 1e-4);
        // non-drownable floors never limit
        assertEquals(Float.MAX_VALUE, LiquidPolicy.survivalSeconds(SHALLOW, 2f, 1f), 0f);
    }

    @Test
    public void crossingTimeAccountsForLiquidSlowdown(){
        // 2 tiles at 2 tiles/s halved to 1 tile/s by water = 2s; without slowdown 1s
        assertEquals(2f, LiquidPolicy.crossingSeconds(2, 2f, DEEP_WATER, true), 1e-4);
        assertEquals(1f, LiquidPolicy.crossingSeconds(2, 2f, DEEP_WATER, false), 1e-4);
    }

    @Test
    public void reserveTimeMakesMarginalCrossingUnsafe(){
        LiquidPolicy p = new LiquidPolicy(1.5f, true);
        // 4 water tiles at halved speed = 4s; survival for 2-tile units = 5s;
        // 4 + 1.5 > 5 -> unsafe despite raw numbers fitting
        assertFalse(p.canSurvive(DEEP_WATER, true, 4, 2f, 2f, 1f, true));
        assertTrue(4f <= 5f); // without the reserve it would fit
        // slightly bigger units survive comfortably: 3 tiles -> 7.5s window
        assertTrue(p.canSurvive(DEEP_WATER, true, 4, 2f, 3f, 1f, true));
    }

    @Test
    public void disabledSurvivableCrossingBlocksEverything(){
        LiquidPolicy p = new LiquidPolicy(1.5f, false);
        assertFalse(p.canSurvive(DEEP_WATER, true, 1, 10f, 10f, 1f, true));
        assertTrue(p.mustBlock(DEEP_WATER, true));
        // air formations never blocked
        assertFalse(p.mustBlock(DEEP_WATER, false));
    }
}
