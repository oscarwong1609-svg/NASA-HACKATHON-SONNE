package com.missionlab.hangar;

import com.missionlab.data.Catalog;
import com.missionlab.game.Session;
import com.missionlab.model.Slot;
import com.missionlab.model.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plays the hangar without graphics: permissions, two-person carrying, installation, votes and the pad. */
class HangarTest {
    private final List<String> toasts = new ArrayList<>();

    private Hangar hangar(int players) {
        Session s = new Session();
        s.playerCount = players;
        s.mission = Catalog.GREENLAND;
        s.newMission();
        return new Hangar(s, new Hangar.Listener() {
            @Override
            public void toast(String message) {
                toasts.add(message);
            }

            @Override
            public void sound(String name) {
            }
        });
    }

    private static Item item(Hangar h, String partId) {
        for (Item it : h.items) if (it.part.id.equals(partId)) return it;
        throw new AssertionError(partId);
    }

    private static void standAt(Player p, float x, float y) {
        p.x = x - Player.W / 2;
        p.y = y - Player.H / 2;
    }

    private static void carryTo(Hangar h, Slot slot, Player... carriers) {
        BlueprintSlot s = h.slots.get(slot);
        for (Player p : carriers) standAt(p, s.cx(), s.cy() - 8);
        h.updateCarried();
    }

    private static void closeEventIfOpen(Hangar h) {
        if (h.event != null) for (Player p : h.players) h.lockVote(p);
    }

    @Test
    void ownerOnlyShelvesWhenRoleIsTaken() {
        Hangar h = hangar(2); // P1 Scientist, P2 Engineer
        Player engineer = h.players.get(1);
        assertEquals(Role.ENGINEER, engineer.role);
        Item cam = item(h, "cam_sharp");
        standAt(engineer, cam.cx(), cam.cy());
        h.act(engineer);
        assertNull(engineer.holding);
        assertTrue(toasts.get(toasts.size() - 1).contains("Scientist"));
    }

    @Test
    void unclaimedShelvesAreOpenToEveryone() {
        Hangar h = hangar(2); // nobody is Ground control
        Player scientist = h.players.get(0);
        Item radio = item(h, "radio_s_fast");
        standAt(scientist, radio.cx(), radio.cy());
        h.act(scientist);
        assertEquals(radio, scientist.holding);
    }

    @Test
    void installAPartOnTheBlueprint() {
        Hangar h = hangar(1);
        Player p = h.players.get(0);
        Item cam = item(h, "cam_sharp");
        standAt(p, cam.cx(), cam.cy());
        h.act(p);
        carryTo(h, Slot.INSTRUMENT, p);
        h.act(p);
        assertNull(p.holding);
        assertEquals(cam.part, h.slots.get(Slot.INSTRUMENT).installed.part);
    }

    @Test
    void wrongSlotIsRefused() {
        Hangar h = hangar(1);
        Player p = h.players.get(0);
        Item cam = item(h, "cam_basic");
        standAt(p, cam.cx(), cam.cy());
        h.act(p);
        carryTo(h, Slot.RADIO, p);
        h.act(p);
        assertEquals(cam, p.holding);
        assertNull(h.slots.get(Slot.RADIO).installed);
    }

    @Test
    void heavyPartsNeedTwoPlayers() {
        Hangar h = hangar(2);
        Player sci = h.players.get(0), eng = h.players.get(1);
        Item batt = item(h, "batt_large");
        standAt(eng, batt.cx(), batt.cy());
        h.act(eng);
        assertTrue(h.waitingForHelp(eng));
        float before = eng.x;
        h.move(eng, 1, 0, 0.5f);
        assertEquals(before, eng.x, 1e-6); // cannot move alone with the part
        standAt(sci, batt.cx() + 4, batt.cy());
        h.act(sci); // helps lift, even though it is not their shelf
        assertFalse(h.waitingForHelp(eng));
        assertEquals(2, batt.carriers.size());
        carryTo(h, Slot.BATTERY, eng, sci);
        h.act(sci);
        assertEquals(batt.part, h.slots.get(Slot.BATTERY).installed.part);
        assertNull(sci.holding);
        assertNull(eng.holding);
    }

    @Test
    void soloPlayerLiftsHeavyPartsAlone() {
        Hangar h = hangar(1);
        Player p = h.players.get(0);
        Item frame = item(h, "frame_6u");
        standAt(p, frame.cx(), frame.cy());
        h.act(p);
        assertFalse(h.waitingForHelp(p));
    }

    @Test
    void secondInstallOpensAProblemAndTheVoteDecides() {
        Hangar h = hangar(2);
        Player sci = h.players.get(0), eng = h.players.get(1);
        install(h, sci, "cam_basic", Slot.INSTRUMENT);
        install(h, eng, "point_mag", Slot.POINTING);
        assertTrue(h.event != null);
        h.vote(sci, 1); // take the risk
        h.vote(eng, -1); // fix
        h.lockVote(sci);
        assertTrue(h.event != null);
        h.lockVote(eng);
        assertNull(h.event);
        assertEquals(1, sessionOf(h).run.decisions.size());
        assertTrue(sessionOf(h).run.decisions.get(0).fixed); // tie = fix
    }

    /** Opens the console, presses right "presses" times and closes it. */
    private static void useConsole(Hangar h, Player p, Station.Kind kind, int presses) {
        Station st = station(h, kind);
        standAt(p, st.cx(), st.cy());
        h.act(p);
        assertEquals(st, h.console);
        for (int i = 0; i < presses; i++) h.consoleInput(p, 1);
        h.act(p);
        assertNull(h.console);
    }

    private void buildGoodGreenland(Hangar h, Player p) {
        String[] parts = {"cam_sharp", "panels_mid", "batt_small", "point_wheels", "radio_x", "frame_6u"};
        for (String id : parts) {
            install(h, p, id, Catalog.part(id).slot);
            closeEventIfOpen(h);
        }
        useConsole(h, p, Station.Kind.ORBIT, 2);   // ISS -> 550 km
        useConsole(h, p, Station.Kind.ROCKET, 1);  // the cheapest
        useConsole(h, p, Station.Kind.GROUND, 1);  // university antenna
    }

    @Test
    void wholeTeamOnThePadLaunchesAReviewedDesign() {
        Hangar h = hangar(1);
        Player p = h.players.get(0);
        buildGoodGreenland(h, p);
        assertTrue(sessionOf(h).design.isComplete());
        assertTrue(h.readiness().go(), h.readiness().nextStep());
        standAt(p, Hangar.PAD_X + Hangar.PAD_W / 2, Hangar.PAD_Y + Hangar.PAD_H / 2);
        for (int i = 0; i < 30; i++) h.updatePad(0.1f);
        assertTrue(h.launchRequested);
    }

    @Test
    void flightReviewNoGoBlocksThePad() {
        Hangar h = hangar(1);
        Player p = h.players.get(0);
        buildGoodGreenland(h, p);
        useConsole(h, p, Station.Kind.GROUND, 2); // global network: too expensive with these parts
        sessionOf(h).design.install(Catalog.part("panels_fold"));
        sessionOf(h).design.install(Catalog.part("batt_large"));
        assertFalse(h.readiness().go());
        standAt(p, Hangar.PAD_X + Hangar.PAD_W / 2, Hangar.PAD_Y + Hangar.PAD_H / 2);
        for (int i = 0; i < 30; i++) h.updatePad(0.1f);
        assertFalse(h.launchRequested);
        assertTrue(toasts.get(toasts.size() - 1).contains("NO-GO"));
    }

    @Test
    void openConsoleFreezesTheTeamAndOnlyItsUserControlsIt() {
        Hangar h = hangar(2);
        Player sci = h.players.get(0), eng = h.players.get(1);
        Station map = station(h, Station.Kind.ORBIT);
        standAt(sci, map.cx(), map.cy()); // with 2 players nobody is Launch lead: it is unclaimed
        h.act(sci);
        assertEquals(sci, h.consoleUser);
        float before = eng.x;
        h.move(eng, 1, 0, 0.5f);
        assertEquals(before, eng.x, 1e-6);
        h.consoleInput(eng, 1);
        assertNull(sessionOf(h).design.orbit);
        h.consoleInput(sci, -1); // backwards: the last orbit in the list
        assertEquals(Catalog.SSO_800, sessionOf(h).design.orbit);
    }

    @Test
    void incompleteDesignCannotLaunch() {
        Hangar h = hangar(1);
        Player p = h.players.get(0);
        standAt(p, Hangar.PAD_X + Hangar.PAD_W / 2, Hangar.PAD_Y + Hangar.PAD_H / 2);
        for (int i = 0; i < 30; i++) h.updatePad(0.1f);
        assertFalse(h.launchRequested);
        assertTrue(toasts.get(toasts.size() - 1).startsWith("Not ready"));
    }

    @Test
    void retryRebuildsTheBlueprintFromTheDesign() {
        Session s = new Session();
        s.playerCount = 1;
        s.newMission();
        s.design.install(Catalog.part("cam_thermal"));
        Hangar h = new Hangar(s, new Hangar.Listener() {
            public void toast(String m) {}
            public void sound(String n) {}
        });
        assertEquals("cam_thermal", h.slots.get(Slot.INSTRUMENT).installed.part.id);
    }

    private static Station station(Hangar h, Station.Kind kind) {
        for (Station st : h.stations) if (st.kind == kind) return st;
        throw new AssertionError(kind);
    }

    @Test
    void rocketConsoleNeedsAnOrbitFirst() {
        Hangar h = hangar(1);
        Player p = h.players.get(0);
        Station console = station(h, Station.Kind.ROCKET);
        standAt(p, console.cx(), console.cy());
        h.act(p);
        h.consoleInput(p, 1);
        assertNull(sessionOf(h).design.route);
        assertTrue(toasts.get(toasts.size() - 1).contains("orbit map first"));
    }

    @Test
    void rocketConsoleOffersTheCheapestRouteFirstThenCycles() {
        Hangar h = hangar(1);
        Player p = h.players.get(0);
        Session s = sessionOf(h);
        // Three presses on the map: ISS 420 -> 550 -> 800.
        useConsole(h, p, Station.Kind.ORBIT, 3);
        assertEquals(Catalog.SSO_800, s.design.orbit);

        Station console = station(h, Station.Kind.ROCKET);
        standAt(p, console.cx(), console.cy());
        h.act(p);
        h.consoleInput(p, 1);
        assertEquals("ride_sso+tug", s.design.route.id()); // Dijkstra: the empty satellite weighs almost nothing
        h.consoleInput(p, 1);
        assertEquals("ride_sso_high", s.design.route.id());
        h.consoleInput(p, 1);
        assertEquals("ride_sso+tug", s.design.route.id()); // wraps around
        h.act(p);

        // Changing orbit clears the route: it led somewhere else.
        useConsole(h, p, Station.Kind.ORBIT, 1);
        assertNull(s.design.route);
    }

    private void install(Hangar h, Player p, String partId, Slot slot) {
        Item it = item(h, partId);
        standAt(p, it.cx(), it.cy());
        h.act(p);
        carryTo(h, slot, p);
        h.act(p);
    }

    private static Session sessionOf(Hangar h) {
        try {
            java.lang.reflect.Field f = Hangar.class.getDeclaredField("session");
            f.setAccessible(true);
            return (Session) f.get(h);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
