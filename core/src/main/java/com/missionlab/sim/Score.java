package com.missionlab.sim;

/**
 * Score out of 100, split into parts so the player can see where it comes from.
 *
 *   Science      50  the photo: resolution × fraction received × lighting × target × orbital lifetime
 *   Margins      20  5 for each margin of at least 10%: mass, power, battery and schedule
 *   Budget       15  full marks if at least 10% of the money is left (the reserve NASA keeps)
 *   Reliability  15  probability that every part works, minus 3 per accepted risk
 */
public final class Score {
    public static final int SCIENCE_MAX = 50;
    public static final int MARGIN_MAX = 20;
    public static final int BUDGET_MAX = 15;
    public static final int RELIABILITY_MAX = 15;

    public final int science;
    public final int margins;
    public final int budget;
    public final int reliability;

    public Score(double science, double margins, double budget, double reliability) {
        this.science = (int) Math.round(science);
        this.margins = (int) Math.round(margins);
        this.budget = (int) Math.round(budget);
        this.reliability = (int) Math.round(reliability);
    }

    public int total() {
        return science + margins + budget + reliability;
    }

    public static Score zero() {
        return new Score(0, 0, 0, 0);
    }

    /**
     * Points (0..1) for one margin: full from 10% up, proportional between 0 and 10%, none if short.
     * A large surplus is not penalised: it already costs mass and money by itself.
     */
    static double marginPoints(double margin) {
        if (margin < 0) return 0;
        return Math.min(1, margin / 0.10);
    }
}
