package com.missionlab.model;

import java.util.ArrayList;
import java.util.List;

/** One playthrough: the mission, the design and the decisions taken on the problems. */
public final class MissionRun {
    public final Mission mission;
    public final Design design;
    public final List<Decision> decisions = new ArrayList<>();

    public MissionRun(Mission mission, Design design) {
        this.mission = mission;
        this.design = design;
    }

    public void decide(BuildEvent event, boolean fixed) {
        decisions.add(new Decision(event, fixed));
    }

    public double extraCostK() {
        double k = 0;
        for (Decision d : decisions) if (d.fixed) k += d.event.fixCostK;
        return k;
    }

    public double extraMonths() {
        double m = 0;
        for (Decision d : decisions) {
            if (d.fixed) m += d.event.fixMonths;
            else if (d.event.effect == EventEffect.DELAY) m += d.event.delayMonths;
        }
        return m;
    }

    /** Accepted risks that can still fail during the mission. */
    public int risksTaken() {
        int n = 0;
        for (Decision d : decisions) if (!d.fixed && d.event.effect != EventEffect.DELAY) n++;
        return n;
    }

    public static final class Decision {
        public final BuildEvent event;
        public final boolean fixed;

        public Decision(BuildEvent event, boolean fixed) {
            this.event = event;
            this.fixed = fixed;
        }
    }
}
