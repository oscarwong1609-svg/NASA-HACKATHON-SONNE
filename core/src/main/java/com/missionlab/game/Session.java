package com.missionlab.game;

import com.missionlab.data.Catalog;
import com.missionlab.ds.LinkedQueue;
import com.missionlab.model.BuildEvent;
import com.missionlab.model.Design;
import com.missionlab.model.Mission;
import com.missionlab.model.MissionRun;
import com.missionlab.model.Role;
import com.missionlab.sim.SimResult;
import com.missionlab.sim.SuccessTier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Game state passed from one screen to the next. */
public final class Session {
    public int playerCount = 1;
    /** Starts with Mission 1. */
    public Mission mission = Catalog.MISSIONS.get(0);
    public Design design = new Design();
    public MissionRun run;
    public SimResult result;
    public long seed;
    public int attempt;
    public final Random rng = new Random();
    /**
     * The test campaign for this attempt: a FIFO queue with the problems in the order they will be tested.
     * Each problem comes up only once, in the scheduled order.
     */
    private final LinkedQueue<BuildEvent> testCampaign = new LinkedQueue<>();
    /** Campaign progress: the best tier and best score for each mission in this session. */
    private final Map<String, SuccessTier> bestTier = new HashMap<>();
    private final Map<String, Integer> bestScore = new HashMap<>();

    /** Starts a mission from scratch (empty blueprint). */
    public void newMission() {
        design = new Design();
        attempt = 0;
        newRun();
    }

    /** New attempt with the same design: decisions are cleared and problems come up again. */
    public void newRun() {
        run = new MissionRun(mission, design);
        scheduleTests();
        result = null;
        seed = rng.nextLong();
        attempt++;
    }

    /** Saves the final result of an attempt if it beats what was achieved on that mission. */
    public void record(Mission m, SimResult r) {
        bestTier.merge(m.id, r.tier, (a, b) -> b.ordinal() > a.ordinal() ? b : a);
        bestScore.merge(m.id, r.score.total(), Math::max);
    }

    public SuccessTier bestTier(Mission m) {
        return bestTier.getOrDefault(m.id, SuccessTier.NONE);
    }

    /** The best score on this mission, or -1 if it has not been played yet. */
    public int bestScore(Mission m) {
        return bestScore.getOrDefault(m.id, -1);
    }

    /** The player's role, or null when playing solo (in solo play everything is open). */
    public Role roleOf(int playerIndex) {
        if (playerCount <= 1) return null;
        return Role.values()[playerIndex];
    }

    /** A role is taken if any player has it. Shelves of unclaimed roles can be used by anyone. */
    public boolean claimed(Role r) {
        return playerCount > 1 && r.ordinal() < playerCount;
    }

    public boolean canUse(int playerIndex, Role owner) {
        return !claimed(owner) || roleOf(playerIndex) == owner;
    }

    /** Heavy parts only need two people if there are at least two players. */
    public boolean twoPersonLifts() {
        return playerCount >= 2;
    }

    /**
     * Schedules the test campaign: the order is shuffled at the start of each attempt (so it is not
     * always the same) and is fixed from then on: it is a queue.
     */
    private void scheduleTests() {
        testCampaign.clear();
        List<BuildEvent> order = new ArrayList<>(Catalog.EVENTS);
        Collections.shuffle(order, rng);
        for (BuildEvent e : order) testCampaign.enqueue(e);
    }

    /**
     * The next test in the campaign that makes sense for the current design, or null if none are left.
     * If the test at the front needs a part that is not installed yet (for example, the hinge of the
     * deployable panels), it goes back to the end of the queue and the next one is tried. At most one full pass.
     */
    public BuildEvent nextEvent() {
        for (int i = 0, n = testCampaign.size(); i < n; i++) {
            BuildEvent e = testCampaign.dequeue();
            if (e.canHappen(design)) return e;
            testCampaign.enqueue(e); // cannot be tested yet: back of the queue
        }
        return null;
    }
}
