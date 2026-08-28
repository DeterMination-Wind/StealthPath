package bsp.core.threat;

import bsp.core.model.FormationProfile;
import bsp.core.model.GridPoint;
import bsp.core.model.ThreatSource;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThreatGridTest{

    private static FormationProfile single(){
        return FormationProfile.single(false, 2f);
    }

    @Test
    public void accumulateStaysInsideRange(){
        ThreatGrid g = new ThreatGrid(50, 50);
        g.accumulate(ThreatSource.direct(25, 25, 8f, 20f, true, true), single());
        assertTrue(g.get(25, 25) > 0f);
        assertEquals(0f, g.get(25, 34), 0f); // ~9.5 tiles away, outside
        assertTrue(g.get(25, 32) > 0f); // ~7 tiles away, inside
    }

    @Test
    public void accumulateRespectsBlindZone(){
        ThreatGrid g = new ThreatGrid(50, 50);
        ThreatSource s = new ThreatSource(25, 25, 10f, 5f, 20f, 0f, 1f,
            bsp.core.model.ThreatShape.CONTINUOUS, 1, 0f, true, true, 1, false, "b");
        g.accumulate(s, single());
        assertEquals(0f, g.get(26, 25), 0f);
        assertTrue(g.get(31, 25) > 0f);
    }

    @Test
    public void multipleSourcesAddUp(){
        ThreatGrid g = new ThreatGrid(50, 50);
        g.accumulate(ThreatSource.direct(25, 25, 10f, 10f, true, true), single());
        g.accumulate(ThreatSource.direct(25, 24, 10f, 10f, true, true), single());
        float v = g.get(30, 25);
        assertTrue(v > 10f); // both contribute
    }

    @Test
    public void clearResets(){
        ThreatGrid g = new ThreatGrid(10, 10);
        g.accumulate(ThreatSource.direct(5, 5, 5f, 10f, true, true), single());
        assertTrue(g.max() > 0f);
        g.clear();
        assertEquals(0f, g.max(), 0f);
    }

    @Test
    public void hoverBreakdownMatchesGridValue(){
        ThreatGrid g = new ThreatGrid(30, 30);
        ThreatSource s = ThreatSource.direct(15, 15, 6f, 12f, true, true);
        FormationProfile f = single();
        g.accumulate(s, f);
        GridPoint tile = new GridPoint(18, 15);
        assertEquals(ThreatGrid.sourceThreatAt(s, f, tile), g.get(18, 15), 1e-4);
    }

    private static boolean at(boolean[] mask, int width, int x, int y){
        return mask[y * width + x];
    }

    @Test
    public void avoidanceMaskCoversRangeWithoutMargin(){
        // source at (15,15), range 5: cell (19,15) center is ~4.5 tiles away
        // -> masked; (21,15) center is ~6.5 tiles away -> not with margin 0
        boolean[] mask = ThreatGrid.avoidanceMask(
            java.util.Collections.singletonList(ThreatSource.direct(15, 15, 5f, 10f, true, true)),
            single(), 30, 30, 0f);
        assertTrue(at(mask, 30, 19, 15));
        assertTrue(!at(mask, 30, 21, 15));
    }

    @Test
    public void avoidanceMaskDilatesByMarginOnly(){
        ThreatSource s = ThreatSource.direct(15, 15, 5f, 10f, true, true);
        boolean[] mask = ThreatGrid.avoidanceMask(
            java.util.Collections.singletonList(s), single(), 30, 30, 2f);
        // (20,15) center is ~5.5 tiles away: outside range, inside range+margin
        assertTrue(at(mask, 30, 20, 15));
        // (22,15) center is ~7.5 tiles away: outside range+margin (7)
        assertTrue(!at(mask, 30, 22, 15));
    }

    @Test
    public void avoidanceMaskKeepsDeadZoneOpen(){
        ThreatSource s = new ThreatSource(15, 15, 8f, 4f, 10f, 0f, 1f,
            bsp.core.model.ThreatShape.CONTINUOUS, 1, 0f, true, true, 1, false, "b");
        boolean[] mask = ThreatGrid.avoidanceMask(
            java.util.Collections.singletonList(s), single(), 30, 30, 3f);
        assertTrue(!at(mask, 30, 16, 15)); // 1.5 tiles: inside min-range dead zone
        assertTrue(at(mask, 30, 20, 15));  // 5.5 tiles: engageable
    }

    @Test
    public void avoidanceMaskIgnoresSourcesThatCannotEngage(){
        // ground-only source vs an air formation: nothing masked at all
        FormationProfile fliers = new FormationProfile(5, 0, 5, 3f, 60f, 0f, 4f, 1f, false, 1f);
        boolean[] mask = ThreatGrid.avoidanceMask(
            java.util.Collections.singletonList(ThreatSource.direct(15, 15, 5f, 10f, false, true)),
            fliers, 30, 30, 2f);
        assertTrue(!at(mask, 30, 17, 15));
        assertTrue(!at(mask, 30, 20, 15));
    }
}
