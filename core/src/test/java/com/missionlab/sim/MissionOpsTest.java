package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.model.Design;
import com.missionlab.sim.MissionOps.Command;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mission Control without graphics: passes, command queue, battery and the score against the perfect autopilot. */
class MissionOpsTest {

    private static void runToEnd(MissionOps ops) {
        while (!ops.finished()) ops.step(30);
    }

    @Test
    void perfectControlGetsTheWholePictureWithAGoodDesign() {
        assertEquals(1.0, MissionOps.autopilotReceived(SimulatorTest.goodAmazon(), Catalog.AMAZON_FIRE, 1), 1e-6);
        assertEquals(1.0, MissionOps.autopilotReceived(SimulatorTest.goodGreenland(), Catalog.GREENLAND, 1), 1e-6);
    }

    @Test
    void doingNothingGetsNothing() {
        MissionOps ops = new MissionOps(SimulatorTest.goodGreenland(), Catalog.GREENLAND, 1);
        runToEnd(ops);
        assertEquals(0, ops.received(), 1e-9);
        assertEquals(0, ops.shareOf(MissionOps.autopilotReceived(SimulatorTest.goodGreenland(), Catalog.GREENLAND, 1)), 1e-9);
    }

    @Test
    void commandsQueuedEarlyRunWhenTheirMomentComes() {
        MissionOps ops = new MissionOps(SimulatorTest.goodGreenland(), Catalog.GREENLAND, 1);
        ops.send(Command.IMAGE);
        for (int i = 0; i < 4; i++) ops.send(Command.DOWNLINK);
        assertEquals(5, ops.queue().size());
        runToEnd(ops);
        assertEquals(1.0, ops.coverage(), 1e-9);
        assertEquals(1.0, ops.received(), 1e-6); // the X-band radio brings it down in a single contact
        assertTrue(ops.elapsedS() < MissionOps.DURATION_S, "operations end early once the picture is home");
    }

    @Test
    void theQueueIsFirstInFirstOut() {
        // DOWNLINK first: the image must wait until the contact has passed, even if there are passes before it.
        MissionOps ops = new MissionOps(SimulatorTest.goodGreenland(), Catalog.GREENLAND, 1);
        ops.send(Command.DOWNLINK);
        ops.send(Command.IMAGE);
        MissionOps.Window firstTarget = ops.next(MissionOps.Window.Type.TARGET);
        MissionOps.Window firstContact = ops.next(MissionOps.Window.Type.CONTACT);
        assertTrue(firstTarget.start < firstContact.start, "the first target pass comes before the first contact");
        ops.step(firstTarget.end + 20);
        assertEquals(0, ops.coverage(), 1e-9, "IMAGE waited behind DOWNLINK");
        assertEquals(Command.DOWNLINK, ops.queue().peek());
    }

    @Test
    void fullQueueRefusesMoreCommands() {
        MissionOps ops = new MissionOps(SimulatorTest.goodGreenland(), Catalog.GREENLAND, 1);
        for (int i = 0; i < MissionOps.MAX_QUEUE; i++) assertTrue(ops.send(Command.DOWNLINK));
        assertFalse(ops.send(Command.IMAGE));
    }

    @Test
    void orbitThatMissesTheTargetHasNoPassesAndAFairShare() {
        Design d = SimulatorTest.goodGreenland();
        d.orbit = Catalog.ISS_ORBIT;
        d.route = Catalog.route(Catalog.ISS_ORBIT, "iss_deploy");
        MissionOps ops = new MissionOps(d, Catalog.GREENLAND, 1);
        assertEquals(0, ops.targetPasses());
        assertEquals(1, ops.shareOf(MissionOps.autopilotReceived(d, Catalog.GREENLAND, 1)), 1e-9);
    }

    @Test
    void weakBatteryFallsIntoSafeModeAtNight() {
        // Reaction wheels always on, small battery and little solar panel: it cannot last through the night.
        Design d = SimulatorTest.goodAmazon();
        d.install(Catalog.part("batt_small"));
        d.install(Catalog.part("point_wheels"));
        d.install(Catalog.part("panels_body"));
        MissionOps ops = new MissionOps(d, Catalog.AMAZON_FIRE, 1);
        ops.send(Command.IMAGE);
        boolean safe = false;
        while (!ops.finished() && !safe) {
            ops.step(30);
            safe = ops.safeMode();
        }
        assertTrue(safe);
    }

    @Test
    void lateImageCommandCoversOnlyPartOfTheTarget() {
        MissionOps ops = new MissionOps(SimulatorTest.goodGreenland(), Catalog.GREENLAND, 1);
        MissionOps.Window pass = ops.next(MissionOps.Window.Type.TARGET);
        ops.step(pass.start + (pass.end - pass.start) / 2 + 10); // halfway through the pass
        ops.send(Command.IMAGE);
        ops.step(pass.end - ops.elapsedS() + 5);
        assertTrue(ops.coverage() > 0.2 && ops.coverage() < 0.7, "coverage " + ops.coverage());
    }

    @Test
    void usefulPassesDependOnOrbitAndLight() {
        assertEquals(0, MissionOps.usefulPassesPerDay(Catalog.ISS_ORBIT, Catalog.GREENLAND));
        assertTrue(MissionOps.usefulPassesPerDay(Catalog.SSO_550, Catalog.GREENLAND) >= 5);
        assertTrue(MissionOps.usefulPassesPerDay(Catalog.SSO_550, Catalog.AMAZON_FIRE) >= 1);
    }

    @Test
    void powerProfileShowsTheBatteryThroughEachOrbit() {
        PowerProfile good = PowerProfile.of(SimulatorTest.goodAmazon(), Catalog.AMAZON_FIRE, 2);
        assertTrue(good.healthy());
        Design weak = SimulatorTest.goodAmazon();
        weak.install(Catalog.part("batt_small"));
        weak.install(Catalog.part("point_wheels"));
        assertFalse(PowerProfile.of(weak, Catalog.AMAZON_FIRE, 2).healthy());
    }
}
