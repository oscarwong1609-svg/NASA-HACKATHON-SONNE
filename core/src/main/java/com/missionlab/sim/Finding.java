package com.missionlab.sim;

/** An explanation for the final report: what happened, what to try next time and how much it weighed. */
public final class Finding {
    public final String text;
    public final String tip;
    /** Approximate points lost; used to sort causes from largest to smallest. */
    public final double impact;
    public final boolean good;

    public Finding(String text, String tip, double impact, boolean good) {
        this.text = text;
        this.tip = tip;
        this.impact = impact;
        this.good = good;
    }

    public static Finding problem(String text, String tip, double impact) {
        return new Finding(text, tip, impact, false);
    }

    public static Finding good(String text) {
        return new Finding(text, null, 0, true);
    }
}
