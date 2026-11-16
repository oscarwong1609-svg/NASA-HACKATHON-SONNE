package com.missionlab.sim;

/**
 * The success tiers, in NASA's terms: every mission defines "minimum success" and
 * "full success" before launch. The game adds a third tier for exemplary designs.
 */
public enum SuccessTier {
    NONE("Mission not achieved", "No medal",
            "The satellite did not deliver a usable photo of the target."),
    MINIMUM("Minimum success", "Bronze",
            "A usable photo: target in view and lit, at least half the picture received, at least half the detail needed."),
    FULL("Full success", "Silver",
            "Everything the scientists asked for: full detail, 90% of the picture, and the satellite lasts the whole mission."),
    EXEMPLARY("Exemplary mission", "Gold",
            "Full success with a score of 90 or more: healthy margins, budget in reserve and few risks.");

    public static final double MIN_COMPLETENESS = 0.5;
    public static final double MIN_RESOLUTION = 0.5;
    public static final double FULL_COMPLETENESS = 0.9;
    public static final int EXEMPLARY_SCORE = 90;

    public final String label;
    public final String medal;
    public final String rule;

    SuccessTier(String label, String medal, String rule) {
        this.label = label;
        this.medal = medal;
        this.rule = rule;
    }

    /** The tier achieved with this result. */
    public static SuccessTier of(boolean flew, PhotoQuality p, int score) {
        if (!flew || !p.received || !p.lit || !p.onTarget) return NONE;
        if (p.completeness < MIN_COMPLETENESS || p.resolution < MIN_RESOLUTION) return NONE;
        boolean full = p.resolution >= 0.999 && p.completeness >= FULL_COMPLETENESS && p.lifeFactor >= 0.999;
        if (!full) return MINIMUM;
        return score >= EXEMPLARY_SCORE ? EXEMPLARY : FULL;
    }

    public boolean atLeast(SuccessTier other) {
        return ordinal() >= other.ordinal();
    }
}
