package bsp.core.threat;

import bsp.core.model.Domain;
import bsp.core.model.FormationProfile;
import bsp.core.model.ThreatShape;
import bsp.core.model.ThreatSource;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThreatModelTest{

    private static FormationProfile formation(int count, boolean flying, float radius){
        return new FormationProfile(count, flying ? 0 : count, flying ? count : 0,
            radius, 100f, 0f, 2f, 1f, !flying, 1f);
    }

    @Test
    public void directTurretDamageDoesNotScaleWithFormationSize(){
        // one target at a time: whole-formation threat stays at single-target dps
        ThreatSource s = ThreatSource.direct(0, 0, 10f, 20f, true, true);
        float small = ThreatModel.teamThreatPerTile(s, formation(1, false, 1f));
        float large = ThreatModel.teamThreatPerTile(s, formation(30, false, 5f));
        assertEquals(small, large, 1e-4);
        assertEquals(20f, large, 1e-4);
    }

    @Test
    public void swarmTurretScalesUpToSimultaneousTargets(){
        ThreatSource s = new ThreatSource(0, 0, 10f, 0f, 20f, 0f, 1f,
            ThreatShape.DIRECT, 1, 0f, true, true, 5, false, "swarm");
        float small = ThreatModel.teamThreatPerTile(s, formation(3, false, 1f));
        float large = ThreatModel.teamThreatPerTile(s, formation(30, false, 5f));
        assertEquals(20f * 3, small, 1e-3); // capped by formation size
        assertEquals(20f * 5, large, 1e-3); // capped by simultaneous targets
    }

    @Test
    public void splashHurtsTightFormationsMoreThanSpreadOnes(){
        ThreatSource s = ThreatSource.splash(0, 0, 12f, 20f, 2f);
        float tight = ThreatModel.teamThreatPerTile(s, formation(20, false, 1f));
        float spread = ThreatModel.teamThreatPerTile(s, formation(20, false, 10f));
        assertTrue("tight formation should take more splash damage, tight=" + tight + " spread=" + spread,
            tight > spread * 1.5f);
        assertTrue(tight <= 20f * 20 + 1e-3); // never exceeds dps * count
    }

    @Test
    public void lineShapeEngagesPierceTargets(){
        ThreatSource s = new ThreatSource(0, 0, 10f, 0f, 20f, 0f, 1f,
            ThreatShape.LINE, 4, 0f, true, true, 1, false, "laser");
        float v = ThreatModel.teamThreatPerTile(s, formation(20, false, 3f));
        assertEquals(20f * 4, v, 1e-3);
    }

    @Test
    public void groundTurretCannotTouchAirFormation(){
        ThreatSource s = ThreatSource.direct(0, 0, 10f, 20f, false, true);
        assertEquals(0f, ThreatModel.teamThreatPerTile(s, formation(10, true, 2f)), 0f);
    }

    @Test
    public void mixedFormationPartiallyEngaged(){
        ThreatSource s = ThreatSource.direct(0, 0, 10f, 20f, false, true); // ground only
        FormationProfile mixed = new FormationProfile(20, 10, 10, 3f, 100f, 0f, 2f, 1f, true, 1f);
        assertEquals(Domain.MIXED, mixed.domain());
        assertEquals(20f, ThreatModel.teamThreatPerTile(s, mixed), 1e-3);
    }

    @Test
    public void armorMitigationHasFloor(){
        // 10 dps at 2 shots/s: 6 armor removes 12 dps -> floored at 25%
        ThreatSource s = new ThreatSource(0, 0, 10f, 0f, 10f, 0f, 2f,
            ThreatShape.DIRECT, 1, 0f, true, true, 1, false, "t");
        FormationProfile armored = new FormationProfile(1, 1, 0, 1f, 100f, 6f, 2f, 1f, true, 1f);
        assertEquals(2.5f, ThreatModel.teamThreatPerTile(s, armored), 1e-3);

        FormationProfile light = new FormationProfile(1, 1, 0, 1f, 100f, 1f, 2f, 1f, true, 1f);
        assertEquals(8f, ThreatModel.teamThreatPerTile(s, light), 1e-3);
    }

    @Test
    public void armorPierceIgnoresArmor(){
        ThreatSource s = new ThreatSource(0, 0, 10f, 0f, 10f, 0f, 2f,
            ThreatShape.DIRECT, 1, 0f, true, true, 1, true, "t");
        FormationProfile armored = new FormationProfile(1, 1, 0, 1f, 100f, 100f, 2f, 1f, true, 1f);
        assertEquals(10f, ThreatModel.teamThreatPerTile(s, armored), 1e-3);
    }

    @Test
    public void minRangeDeadZoneAndRangeCutoff(){
        ThreatSource s = new ThreatSource(0, 0, 10f, 4f, 20f, 0f, 1f,
            ThreatShape.CONTINUOUS, 1, 0f, true, true, 1, false, "beam");
        FormationProfile f = formation(1, false, 1f);
        assertEquals(0f, ThreatModel.cellThreat(s, f, 3.9f), 0f);
        assertTrue(ThreatModel.cellThreat(s, f, 4.5f) > 0f);
        assertEquals(0f, ThreatModel.cellThreat(s, f, 10.5f), 0f);
    }

    @Test
    public void directShotsLoseAccuracyAtRange(){
        ThreatSource s = ThreatSource.direct(0, 0, 10f, 20f, true, true);
        FormationProfile f = formation(1, false, 1f);
        float near = ThreatModel.cellThreat(s, f, 1f);
        float far = ThreatModel.cellThreat(s, f, 9f);
        assertTrue(near > far);
        assertTrue(far > 20f * 0.5f); // at most 40% loss
    }

    @Test
    public void statusDpsAddsHalfWeight(){
        ThreatSource plain = ThreatSource.direct(0, 0, 10f, 10f, true, true);
        ThreatSource burning = new ThreatSource(0, 0, 10f, 0f, 10f, 6f, 1f,
            ThreatShape.DIRECT, 1, 0f, true, true, 1, false, "t");
        FormationProfile f = formation(1, false, 1f);
        assertEquals(13f, ThreatModel.teamThreatPerTile(burning, f), 1e-3);
        assertTrue(ThreatModel.teamThreatPerTile(plain, f) < ThreatModel.teamThreatPerTile(burning, f));
    }
}
