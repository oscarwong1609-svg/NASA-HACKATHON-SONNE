package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.model.Design;
import com.missionlab.model.Mission;
import com.missionlab.model.MissionRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The flight readiness review: what blocks the launch and what the next step is. */
class ReadinessTest {

    private static Readiness.Status status(Readiness r, String label) {
        for (Readiness.Check c : r.checks) if (c.label.equals(label)) return c.status;
        throw new AssertionError(label);
    }

    @Test
    void goodDesignsAreGo() {
        assertTrue(Readiness.of(SimulatorTest.goodAmazon(), Catalog.AMAZON_FIRE).go());
        assertTrue(Readiness.of(SimulatorTest.goodGreenland(), Catalog.GREENLAND).go());
        assertNull(Readiness.of(SimulatorTest.goodGreenland(), Catalog.GREENLAND).firstBlocker());
    }

    @Test
    void emptyDesignIsPendingAndSaysWhatToDoFirst() {
        Readiness r = Readiness.of(new Design(), Catalog.AMAZON_FIRE);
        assertFalse(r.go());
        assertEquals(Readiness.Status.PENDING, status(r, "ASSEMBLY"));
        assertTrue(r.nextStep().contains("INSTRUMENT"), r.nextStep());
    }

    @Test
    void nextStepWalksThroughTheConsoles() {
        Mission m = Catalog.GREENLAND;
        Design d = SimulatorTest.goodGreenland();
        d.orbit = null;
        d.route = null;
        d.station = null;
        assertTrue(Readiness.of(d, m).nextStep().contains("orbit map"));
        d.orbit = Catalog.SSO_550;
        assertTrue(Readiness.of(d, m).nextStep().contains("rocket console"));
        d.route = Catalog.route(Catalog.SSO_550, "ride_sso");
        assertTrue(Readiness.of(d, m).nextStep().contains("ground station desk"));
        d.station = Catalog.station("gs_uni");
        assertTrue(Readiness.of(d, m).nextStep().startsWith("All systems GO"));
    }

    @Test
    void overBudgetIsNoGo() {
        Design d = SimulatorTest.goodGreenland();
        d.install(Catalog.part("panels_fold"));
        d.install(Catalog.part("batt_large"));
        d.station = Catalog.station("gs_global");
        Readiness r = Readiness.of(d, Catalog.GREENLAND);
        assertEquals(Readiness.Status.NO_GO, status(r, "BUDGET"));
        assertFalse(r.go());
        assertTrue(r.nextStep().contains("NO-GO on BUDGET"));
    }

    @Test
    void partsThatDoNotFitAreNoGo() {
        Design d = SimulatorTest.goodAmazon();
        d.install(Catalog.part("frame_2u"));
        assertEquals(Readiness.Status.NO_GO, status(Readiness.of(d, Catalog.AMAZON_FIRE), "STRUCTURE"));
    }

    @Test
    void sciencePitfallsAreWarningsNotBlockers() {
        Design d = SimulatorTest.goodAmazon();
        d.install(Catalog.part("cam_basic"));
        Readiness r = Readiness.of(d, Catalog.AMAZON_FIRE);
        assertEquals(Readiness.Status.WARN, status(r, "LIGHTING"));
        assertTrue(r.go(), "a WARN never blocks the launch");
    }

    @Test
    void testProblemsCountTowardsTheReview() {
        MissionRun run = new MissionRun(Catalog.AMAZON_FIRE, SimulatorTest.goodAmazon());
        assertEquals(Readiness.Status.WARN, status(Readiness.of(run), "SCHEDULE")); // 0 months of slack
        run.decide(Catalog.EVENTS.get(0), true); // fix the antenna: +1 month
        assertEquals(Readiness.Status.NO_GO, status(Readiness.of(run), "SCHEDULE"));
    }
}
