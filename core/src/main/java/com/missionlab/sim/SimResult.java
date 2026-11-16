package com.missionlab.sim;

import com.missionlab.ds.LinkedQueue;
import com.missionlab.model.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the launch, Mission Control and result screens need to know.
 *
 * The simulator computes the result assuming Mission Control does everything perfectly.
 * After operations are played, withOperations() adjusts the photo and score to what the team
 * actually achieved: the design sets the ceiling and operations decide how much of it is reached.
 */
public final class SimResult {
    public final boolean launched;
    /** How many stages were passed (0 to 4). 4 = mission complete. */
    public final int stagesPassed;
    /** The stage that failed, or null if none failed (or if it never lifted off). */
    public final Stage failedAt;
    public final PhotoQuality photo;
    public final Score score;
    public final SuccessTier tier;
    /** Causes sorted from largest to smallest impact; good news goes last. */
    public final List<Finding> findings;
    public final Map<Role, Boolean> goals;
    public final Budgets budgets;
    /** Accepted risks that did materialise. */
    public final List<String> struck;
    /** Fraction of what was possible that Mission Control achieved (1 = perfect, or not played yet). */
    public final double operationsShare;
    /** Flight events in chronological order. Private: a copy is handed out to be consumed. */
    private final LinkedQueue<FlightEvent> timeline;

    SimResult(boolean launched, int stagesPassed, Stage failedAt, PhotoQuality photo, Score score,
              List<Finding> findings, Map<Role, Boolean> goals, Budgets budgets, List<String> struck,
              LinkedQueue<FlightEvent> timeline) {
        this(launched, stagesPassed, failedAt, photo, score, findings, goals, budgets, struck, timeline, 1.0);
    }

    private SimResult(boolean launched, int stagesPassed, Stage failedAt, PhotoQuality photo, Score score,
                      List<Finding> findings, Map<Role, Boolean> goals, Budgets budgets, List<String> struck,
                      LinkedQueue<FlightEvent> timeline, double operationsShare) {
        this.launched = launched;
        this.stagesPassed = stagesPassed;
        this.failedAt = failedAt;
        this.photo = photo;
        this.score = score;
        List<Finding> sorted = new ArrayList<>(findings);
        sorted.sort((a, b) -> {
            if (a.good != b.good) return a.good ? 1 : -1;
            return Double.compare(b.impact, a.impact);
        });
        this.findings = Collections.unmodifiableList(sorted);
        this.goals = Collections.unmodifiableMap(new EnumMap<>(goals));
        this.budgets = budgets;
        this.struck = Collections.unmodifiableList(new ArrayList<>(struck));
        this.timeline = new LinkedQueue<>(timeline);
        this.operationsShare = operationsShare;
        this.tier = SuccessTier.of(launched && failedAt == null, photo, score.total());
    }

    /**
     * A fresh copy of the flight timeline (FIFO queue), ready to be drained with dequeue().
     * It is a copy so the launch screen can empty it without changing the result.
     */
    public LinkedQueue<FlightEvent> timeline() {
        return new LinkedQueue<>(timeline);
    }

    public boolean stagePassed(Stage s) {
        return stagesPassed > s.ordinal();
    }

    /** true if the satellite reached science operations alive (there is something to play in Mission Control). */
    public boolean reachedOperations() {
        return launched && failedAt == null;
    }

    /** The main causes for the report (at most n). */
    public List<Finding> topFindings(int n) {
        return findings.subList(0, Math.min(n, findings.size()));
    }

    /**
     * The result after Mission Control.
     *
     * @param share fraction of the data the team managed to bring down, compared with what
     *              perfect control would have achieved with the same satellite (0..1)
     * @param note  an explanation for the report (or null)
     */
    public SimResult withOperations(double share, Finding note) {
        double s = Math.max(0, Math.min(1, share));
        PhotoQuality p = photo.withCompleteness(photo.completeness * s);
        Score sc = new Score(score.science * s, score.margins, score.budget, score.reliability);
        List<Finding> f = new ArrayList<>(findings);
        if (note != null) f.add(note);
        Map<Role, Boolean> g = new EnumMap<>(goals);
        g.put(Role.GROUND, stagesPassed >= 3 && p.completeness >= SuccessTier.FULL_COMPLETENESS && budgets.budgetReserve() >= 0.10);
        return new SimResult(launched, stagesPassed, failedAt, p, sc, f, g, budgets, struck, timeline, s);
    }
}
