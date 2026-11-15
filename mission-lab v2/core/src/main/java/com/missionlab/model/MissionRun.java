package com.missionlab.model;

import java.util.ArrayList;
import java.util.List;

/** Una partida: el encargo, el diseño y las decisiones tomadas ante los problemas. */
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

    /** Riesgos aceptados que todavía pueden fallar durante la misión. */
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
