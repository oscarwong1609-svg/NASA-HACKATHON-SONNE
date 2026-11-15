package com.missionlab.game;

import com.missionlab.data.Catalog;
import com.missionlab.model.BuildEvent;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La campaña de pruebas del hangar es una cola: cada problema sale una vez y en orden. */
class SessionTest {

    @Test
    void everyTestComesOutOnceAndHingeWaitsForItsPanels() {
        Session s = new Session();
        s.newMission();
        Set<String> seen = new HashSet<>();
        for (BuildEvent e = s.nextEvent(); e != null; e = s.nextEvent()) {
            assertTrue(seen.add(e.id), e.id + " came out twice");
            assertNotEquals("ev_hinge", e.id, "no deployable panels yet");
        }
        assertEquals(Catalog.EVENTS.size() - 1, seen.size());

        // La prueba de la bisagra esperaba al final de la cola; ahora sí tiene sentido.
        s.design.install(Catalog.part("panels_mid"));
        assertEquals("ev_hinge", s.nextEvent().id);
        assertNull(s.nextEvent());
    }

    @Test
    void aNewAttemptSchedulesTheWholeCampaignAgain() {
        Session s = new Session();
        s.newMission();
        s.design.install(Catalog.part("panels_fold"));
        while (s.nextEvent() != null) { /* vaciar */ }
        s.newRun();
        int n = 0;
        while (s.nextEvent() != null) n++;
        assertEquals(Catalog.EVENTS.size(), n);
    }

    @Test
    void campaignRemembersTheBestResultPerMission() {
        Session s = new Session();
        assertEquals(Catalog.MISSIONS.get(0), s.mission, "the campaign starts at Mission 1");
        assertEquals(-1, s.bestScore(Catalog.GREENLAND));
        com.missionlab.sim.SimResult good = com.missionlab.sim.Simulator.run(
                new com.missionlab.model.MissionRun(Catalog.GREENLAND, com.missionlab.sim.SimulatorTestAccess.goodGreenland()), 1);
        com.missionlab.sim.SimResult bad = good.withOperations(0, null);
        s.record(Catalog.GREENLAND, good);
        s.record(Catalog.GREENLAND, bad);
        assertEquals(good.tier, s.bestTier(Catalog.GREENLAND));
        assertEquals(good.score.total(), s.bestScore(Catalog.GREENLAND));
        assertEquals(com.missionlab.sim.SuccessTier.NONE, s.bestTier(Catalog.AMAZON_FIRE));
    }
}
