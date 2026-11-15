package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.ds.LinkedQueue;
import com.missionlab.model.BuildEvent;
import com.missionlab.model.Design;
import com.missionlab.model.EventEffect;
import com.missionlab.model.Mission;
import com.missionlab.model.MissionRun;
import com.missionlab.model.Role;
import com.missionlab.model.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de las reglas. Correr con: ./gradlew :core:test */
class SimulatorTest {
    /** Con esta semilla los paneles desplegables no se atascan. */
    static final long NO_JAM_SEED = 1;

    /** Mission 1: cámara térmica, órbita de la ISS con el viaje gratis de NASA. */
    static Design goodAmazon() {
        Design d = parts("cam_thermal", "panels_mid", "batt_large", "point_mag", "radio_s_fast", "frame_3u");
        d.orbit = Catalog.ISS_ORBIT;
        d.route = Catalog.route(Catalog.ISS_ORBIT, "csli");
        d.station = Catalog.station("gs_uni");
        return d;
    }

    /** Mission 2: cámara de alta resolución, ruedas de reacción, banda X, órbita heliosíncrona. */
    static Design goodGreenland() {
        Design d = parts("cam_sharp", "panels_mid", "batt_small", "point_wheels", "radio_x", "frame_6u");
        d.orbit = Catalog.SSO_550;
        d.route = Catalog.route(Catalog.SSO_550, "ride_sso");
        d.station = Catalog.station("gs_uni");
        return d;
    }

    static Design parts(String... ids) {
        Design d = new Design();
        for (String id : ids) d.install(Catalog.part(id));
        return d;
    }

    private static SimResult run(Mission m, Design d) {
        return Simulator.run(new MissionRun(m, d), NO_JAM_SEED);
    }

    private static boolean explained(SimResult r, String words) {
        for (Finding f : r.findings) if (f.text.contains(words)) return true;
        return false;
    }

    // ---------------------------------------------------------------- se puede ganar

    @Test
    void goodDesignsReachFullSuccessInBothMissions() {
        for (Object[] c : new Object[][] {{Catalog.AMAZON_FIRE, goodAmazon()}, {Catalog.GREENLAND, goodGreenland()}}) {
            SimResult r = run((Mission) c[0], (Design) c[1]);
            assertTrue(r.launched);
            assertEquals(4, r.stagesPassed);
            assertNull(r.failedAt);
            assertTrue(r.tier.atLeast(SuccessTier.FULL), "tier was " + r.tier);
            assertTrue(r.score.total() >= 80 && r.score.total() < 100, "score was " + r.score.total());
            assertTrue(r.goals.get(Role.SCIENTIST));
        }
    }

    @Test
    void cheapDesignStillLaunchesAndGetsAMedal() {
        // La pieza más barata de cada hueco: se puede lanzar, y en Greenland ya da el éxito mínimo.
        Design cheap = Balance.cheapestDesign(Catalog.GREENLAND);
        assertTrue(Readiness.of(cheap, Catalog.GREENLAND).go());
        SimResult r = run(Catalog.GREENLAND, cheap);
        assertTrue(r.launched);
        assertEquals(SuccessTier.MINIMUM, r.tier);
    }

    // ---------------------------------------------------------------- ciencia

    @Test
    void normalCameraSeesNothingAtNight() {
        Design d = goodAmazon();
        d.install(Catalog.part("cam_basic"));
        SimResult r = run(Catalog.AMAZON_FIRE, d);
        assertTrue(r.launched);
        assertFalse(r.photo.lit);
        assertEquals(0, r.score.science);
        assertEquals(SuccessTier.NONE, r.tier);
    }

    @Test
    void lowerOrbitGivesFinerPixels() {
        Design d = goodGreenland();
        double[] gsd = new double[3];
        for (int i = 0; i < 3; i++) {
            d.orbit = Catalog.ORBITS.get(i);
            gsd[i] = Budgets.of(d, Catalog.GREENLAND).gsdM;
        }
        assertTrue(gsd[0] < gsd[1] && gsd[1] < gsd[2]);
        assertEquals(550 * 8 / 1000.0, gsd[1], 1e-9); // altura x IFOV
    }

    @Test
    void wideCameraIsTooCoarseForGlacierFronts() {
        Design d = goodGreenland();
        d.install(Catalog.part("cam_basic"));
        SimResult r = run(Catalog.GREENLAND, d);
        assertTrue(r.photo.resolution < 1);
        assertEquals(SuccessTier.MINIMUM, r.tier);
        assertTrue(explained(r, "scientists needed 15 m"));
    }

    @Test
    void narrowCameraNeedsPrecisePointing() {
        Design d = goodGreenland();
        d.install(Catalog.part("point_mag"));
        SimResult r = run(Catalog.GREENLAND, d);
        assertTrue(r.photo.completeness < 0.5);
        assertTrue(explained(r, "missed the target"));
    }

    @Test
    void slowLinkLosesMostOfThePicture() {
        Design d = goodGreenland();
        d.install(Catalog.part("radio_s"));
        SimResult r = run(Catalog.GREENLAND, d);
        assertTrue(r.photo.completeness < 0.2);
        assertTrue(explained(r, "never reached Earth"));
    }

    @Test
    void nightImagingNeedsABigBattery() {
        Design d = goodAmazon();
        d.install(Catalog.part("batt_small"));
        Budgets b = Budgets.of(d, Catalog.AMAZON_FIRE);
        assertTrue(b.batteryUsableWh < b.eclipseNeedWh);
        assertTrue(run(Catalog.AMAZON_FIRE, d).photo.completeness < run(Catalog.AMAZON_FIRE, goodAmazon()).photo.completeness);
    }

    @Test
    void wrongOrbitMissesTheTarget() {
        Design d = goodGreenland();
        d.orbit = Catalog.ISS_ORBIT;
        d.route = Catalog.route(Catalog.ISS_ORBIT, "iss_deploy");
        SimResult r = run(Catalog.GREENLAND, d);
        assertTrue(r.launched);
        assertFalse(r.photo.onTarget);
        assertEquals(0, r.score.science);
    }

    // ---------------------------------------------------------------- lo que cancela o rompe la misión

    @Test
    void overBudgetNeverLaunches() {
        Design d = goodGreenland();
        d.install(Catalog.part("panels_fold"));
        d.install(Catalog.part("batt_large"));
        d.station = Catalog.station("gs_global");
        SimResult r = run(Catalog.GREENLAND, d);
        assertFalse(r.launched);
        assertEquals(0, r.score.total());
        assertTrue(r.findings.get(0).text.contains("budget"));
    }

    @Test
    void lateSatelliteMissesItsRide() {
        // La cámara térmica tarda 7 meses + 3 de pruebas = mes 10, justo la entrega del viaje gratis.
        BuildEvent late = null;
        for (BuildEvent e : Catalog.EVENTS) if (e.effect == EventEffect.DELAY) late = e;
        MissionRun run = new MissionRun(Catalog.AMAZON_FIRE, goodAmazon());
        run.decide(late, false); // esperar al proveedor: +2 meses
        SimResult r = Simulator.run(run, NO_JAM_SEED);
        assertFalse(r.launched);
        assertTrue(r.findings.get(0).text.contains("handed over"));
    }

    @Test
    void frameTooSmallBreaksAtLiftoff() {
        Design d = goodAmazon();
        d.install(Catalog.part("frame_2u")); // los componentes necesitan 2.45U
        SimResult r = run(Catalog.AMAZON_FIRE, d);
        assertEquals(Stage.LIFTOFF, r.failedAt);
        assertEquals(0, r.score.total());
    }

    @Test
    void routeMustEndAtTheChosenOrbit() {
        Design d = goodGreenland();
        d.orbit = Catalog.SSO_800; // la ruta elegida termina en 550 km
        SimResult r = run(Catalog.GREENLAND, d);
        assertFalse(r.launched);
        assertTrue(r.findings.get(0).text.contains("route"));
    }

    @Test
    void acceptedAntennaRiskCanKillTheMission() {
        BuildEvent sure = new BuildEvent("t", "t", "t", "fix", "risk", 15, 1, EventEffect.NO_CONTACT, 1.0, 0, null);
        MissionRun run = new MissionRun(Catalog.GREENLAND, goodGreenland());
        run.decide(sure, false);
        SimResult r = Simulator.run(run, NO_JAM_SEED);
        assertEquals(Stage.CONTACT, r.failedAt);
        assertEquals(2, r.stagesPassed);
        assertFalse(r.photo.received);
        assertFalse(r.reachedOperations());
    }

    // ---------------------------------------------------------------- cuentas

    @Test
    void fixingAProblemCostsBudgetAndTime() {
        BuildEvent ev = Catalog.EVENTS.get(0);
        MissionRun run = new MissionRun(Catalog.GREENLAND, goodGreenland());
        Budgets before = Budgets.of(run);
        run.decide(ev, true);
        Budgets after = Budgets.of(run);
        assertEquals(before.costK + ev.fixCostK, after.costK, 1e-9);
        assertEquals(before.months + ev.fixMonths, after.months, 1e-9);
    }

    @Test
    void waitingForSupplierCostsMonthsNotBudget() {
        BuildEvent late = null;
        for (BuildEvent e : Catalog.EVENTS) if (e.effect == EventEffect.DELAY) late = e;
        MissionRun run = new MissionRun(Catalog.GREENLAND, goodGreenland());
        Budgets before = Budgets.of(run);
        run.decide(late, false);
        Budgets after = Budgets.of(run);
        assertEquals(before.costK, after.costK, 1e-9);
        assertEquals(before.months + late.delayMonths, after.months, 1e-9);
        assertEquals(0, run.risksTaken());
    }

    @Test
    void budgetsFollowThePartsWithDutyCycledPower() {
        Design d = new Design();
        Budgets empty = Budgets.of(d, Catalog.GREENLAND);
        d.install(Catalog.part("cam_sharp"));
        Budgets one = Budgets.of(d, Catalog.GREENLAND);
        assertEquals(empty.massKg + 1.2, one.massKg, 1e-9);
        assertEquals(empty.volumeU + 1.5, one.volumeU, 1e-9);
        // La cámara gasta 6 W, pero solo el 20% del tiempo.
        assertEquals(empty.drawW + 6 * Catalog.GREENLAND.imagingDuty, one.drawW, 1e-9);
        assertEquals(4.0, one.dataGbPerDay, 1e-9);
        assertFalse(d.isComplete());
    }

    @Test
    void scheduleIsSetByTheSlowestPart() {
        Budgets b = Budgets.of(goodGreenland(), Catalog.GREENLAND);
        assertEquals(Catalog.INTEGRATION_MONTHS + 5, b.months, 1e-9); // cámara y radio X: 5 meses
        assertEquals(Catalog.GREENLAND.monthsToLaunch - Catalog.RIDE_SSO.deliveryMonths, b.deadlineMonths, 1e-9);
    }

    @Test
    void longLivedOrbitNeedsADragSail() {
        Design d = goodGreenland();
        Budgets low = Budgets.of(d, Catalog.GREENLAND);
        d.orbit = Catalog.SSO_800;
        d.route = Catalog.route(Catalog.SSO_800, "ride_sso_high");
        Budgets high = Budgets.of(d, Catalog.GREENLAND);
        assertFalse(low.needsDragSail);
        assertTrue(high.needsDragSail);
        assertEquals(low.massKg + Catalog.DRAG_SAIL_KG, high.massKg, 1e-9);
        assertEquals(Catalog.DRAG_SAIL_K, high.dragSailK, 1e-9);
    }

    @Test
    void installingReplacesThePartInThatSlot() {
        Design d = goodGreenland();
        assertEquals("cam_sharp", d.install(Catalog.part("cam_basic")).id);
        assertEquals("cam_basic", d.get(Slot.INSTRUMENT).id);
    }

    @Test
    void sameSeedGivesSameResult() {
        for (long seed = 0; seed < 50; seed++) {
            SimResult a = Simulator.run(new MissionRun(Catalog.GREENLAND, goodGreenland()), seed);
            SimResult b = Simulator.run(new MissionRun(Catalog.GREENLAND, goodGreenland()), seed);
            assertEquals(a.score.total(), b.score.total());
        }
    }

    // ---------------------------------------------------------------- grafo de vuelo

    @Test
    void launchCostIsTheSumOfTheRouteEdges() {
        Design d = goodGreenland();
        d.orbit = Catalog.SSO_800;
        d.route = Catalog.route(Catalog.SSO_800, "ride_sso", "tug");
        Budgets b = Budgets.of(d, Catalog.GREENLAND);
        double expected = Catalog.RIDE_SSO.costFor(b.massKg) + Catalog.SPACE_TUG.costFor(b.massKg);
        assertEquals(expected, b.launchCostK, 1e-9);
        assertEquals(12, b.launchCapacityKg, 1e-9); // el vehículo más pequeño del camino
    }

    @Test
    void spaceTugRouteReachesTheHighOrbitInTwoLegs() {
        Design d = goodGreenland();
        d.orbit = Catalog.SSO_800;
        d.route = Catalog.route(Catalog.SSO_800, "ride_sso", "tug");
        SimResult r = run(Catalog.GREENLAND, d);
        assertTrue(r.launched);
        assertNull(r.failedAt);
        LinkedQueue<FlightEvent> t = r.timeline();
        assertEquals(FlightEvent.Kind.LIFTOFF, t.dequeue().kind);
        FlightEvent drop = t.dequeue();
        assertEquals(FlightEvent.Kind.TRANSFER, drop.kind);
        assertEquals(Catalog.SSO_550, drop.orbit);
        FlightEvent arrive = t.dequeue();
        assertEquals(FlightEvent.Kind.ORBIT, arrive.kind);
        assertEquals(Catalog.SSO_800, arrive.orbit);
        assertTrue(arrive.hours > 24 * 29, "the tug takes about a month");
    }

    @Test
    void launchLeadGoalNeedsTheCheapestRoute() {
        assertTrue(run(Catalog.AMAZON_FIRE, goodAmazon()).goals.get(Role.LAUNCH)); // viaje gratis de NASA
        Design paid = goodAmazon();
        paid.route = Catalog.route(Catalog.ISS_ORBIT, "iss_deploy");
        SimResult r = run(Catalog.AMAZON_FIRE, paid);
        assertTrue(r.launched);
        assertFalse(r.goals.get(Role.LAUNCH));
        assertTrue(explained(r, "cheapest route was NASA CubeSat Launch Initiative"));
    }

    // ---------------------------------------------------------------- línea de tiempo (cola)

    @Test
    void timelineIsChronologicalAndEndsAtFirstContact() {
        SimResult r = run(Catalog.GREENLAND, goodGreenland());
        LinkedQueue<FlightEvent> t = r.timeline();
        FlightEvent.Kind[] expected = {FlightEvent.Kind.LIFTOFF, FlightEvent.Kind.ORBIT, FlightEvent.Kind.PANELS, FlightEvent.Kind.CONTACT};
        assertEquals(expected.length, t.size());
        double last = -1;
        for (FlightEvent.Kind k : expected) {
            FlightEvent e = t.dequeue();
            assertEquals(k, e.kind);
            assertTrue(e.hours >= last, "events must come out in time order");
            last = e.hours;
        }
        assertTrue(t.isEmpty());
        assertEquals(expected.length, r.timeline().size(), "each call gives a fresh copy");
        assertTrue(r.reachedOperations());
    }

    @Test
    void timelineStopsWhereTheMissionFailed() {
        BuildEvent sure = new BuildEvent("t", "t", "t", "fix", "risk", 15, 1, EventEffect.NO_CONTACT, 1.0, 0, null);
        MissionRun run = new MissionRun(Catalog.GREENLAND, goodGreenland());
        run.decide(sure, false);
        LinkedQueue<FlightEvent> t = Simulator.run(run, NO_JAM_SEED).timeline();
        FlightEvent lastEvent = null;
        while (!t.isEmpty()) lastEvent = t.dequeue();
        assertEquals(FlightEvent.Kind.CONTACT, lastEvent.kind);
        assertTrue(lastEvent.failed());
    }

    @Test
    void cancelledLaunchHasASingleEvent() {
        SimResult r = run(Catalog.GREENLAND, new Design());
        assertEquals(1, r.timeline().size());
        assertEquals(FlightEvent.Kind.CANCELLED, r.timeline().peek().kind);
    }

    // ---------------------------------------------------------------- operaciones y niveles

    @Test
    void operationsScaleThePhotoAndTheTier() {
        SimResult r = run(Catalog.GREENLAND, goodGreenland());
        SimResult half = r.withOperations(0.5, Finding.problem("half", null, 1));
        assertEquals(r.photo.completeness * 0.5, half.photo.completeness, 1e-9);
        assertEquals(Math.round(r.score.science * 0.5), half.score.science);
        assertEquals(SuccessTier.MINIMUM, half.tier);
        assertEquals(0.5, half.operationsShare, 1e-9);
        assertEquals(r.tier, r.withOperations(1, null).tier);
    }

    @Test
    void balanceRulesHoldForBothMissions() {
        java.util.List<Design> all = Balance.allDesigns();
        for (Mission m : Catalog.MISSIONS) {
            Balance.Report rep = Balance.evaluate(m, all);
            assertTrue(rep.passes(), rep.toString());
        }
    }
}
