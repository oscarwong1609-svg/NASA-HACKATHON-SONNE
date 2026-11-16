package com.missionlab.model;

/**
 * A problem that comes up during the build, as in real CubeSat 101 testing.
 * The team chooses: fix it (costs money and months) or take the risk.
 */
public final class BuildEvent {
    public final String id;
    public final String title;
    public final String text;
    public final String fixLabel;
    public final String riskLabel;
    public final double fixCostK;
    public final double fixMonths;
    public final EventEffect effect;
    /** Probability that the risk happens if it is not fixed (0..1). */
    public final double chance;
    /** DELAY only: months lost if not paid for. */
    public final double delayMonths;
    /** If not null, the problem only appears when one of these parts is installed (ids separated by |). */
    public final String requiresPartId;

    public BuildEvent(String id, String title, String text, String fixLabel, String riskLabel,
                      double fixCostK, double fixMonths, EventEffect effect, double chance,
                      double delayMonths, String requiresPartId) {
        this.id = id; this.title = title; this.text = text; this.fixLabel = fixLabel; this.riskLabel = riskLabel;
        this.fixCostK = fixCostK; this.fixMonths = fixMonths; this.effect = effect; this.chance = chance;
        this.delayMonths = delayMonths; this.requiresPartId = requiresPartId;
    }

    public boolean canHappen(Design design) {
        if (requiresPartId == null) return true;
        for (Part p : design.parts()) {
            for (String id : requiresPartId.split("\\|")) if (p.id.equals(id)) return true;
        }
        return false;
    }
}
