package com.missionlab.model;

import java.util.List;

/**
 * A science mission: the question, the area, the budget, the launch date and what counts as success.
 * Both missions in the game have exactly the same structure; only the numbers change.
 */
public final class Mission {
    public final String id;
    /** Order in the campaign: Mission 1, Mission 2... */
    public final int number;
    public final String title;
    public final String brief;
    public final String targetName;
    /** Point marked on the maps (for example, Jakobshavn Glacier). */
    public final double targetLatitude;
    public final double targetLongitude;
    /** Area to be imaged (degrees). A pass over this box counts as a useful pass. */
    public final double regionLatMin, regionLatMax, regionLonMin, regionLonMax;
    /** true = the target is only visible at night (for example, fires). */
    public final boolean night;
    public final double budgetK;
    /** Months until the rocket launches. */
    public final double monthsToLaunch;
    public final double missionYears;
    /** Ground resolution the scientists ask for (metres per pixel). Finer does not earn extra points. */
    public final double requiredGsdM;
    /** Why that resolution is needed, in words. */
    public final String resolutionReason;
    /** Solar declination in the mission's season (degrees): decides day, night and eclipses. */
    public final double sunDeclinationDeg;
    /** Fraction of the time the camera is taking pictures (0..1). */
    public final double imagingDuty;
    /** File in assets/photos/. If it does not exist, the game draws a placeholder image. */
    public final String photoFile;
    /** Credit line for the real NASA photo (fill in when the image is saved). */
    public final String photoCredit;
    /** A similar real NASA mission, for the final card. */
    public final String similarMission;
    /** Real facts for the briefing and the final screen. */
    public final List<String> facts;

    private Mission(Builder b) {
        id = b.id; number = b.number; title = b.title; brief = b.brief; targetName = b.targetName;
        targetLatitude = b.targetLat; targetLongitude = b.targetLon;
        regionLatMin = b.latMin; regionLatMax = b.latMax; regionLonMin = b.lonMin; regionLonMax = b.lonMax;
        night = b.night; budgetK = b.budgetK; monthsToLaunch = b.monthsToLaunch; missionYears = b.missionYears;
        requiredGsdM = b.requiredGsdM; resolutionReason = b.resolutionReason; sunDeclinationDeg = b.sunDeclinationDeg;
        imagingDuty = b.imagingDuty; photoFile = b.photoFile; photoCredit = b.photoCredit;
        similarMission = b.similarMission; facts = List.copyOf(b.facts);
    }

    public static Builder builder(String id, int number, String title) {
        return new Builder(id, number, title);
    }

    /** true if the point is inside the mission area. */
    public boolean inRegion(double lat, double lon) {
        return lat >= regionLatMin && lat <= regionLatMax && lon >= regionLonMin && lon <= regionLonMax;
    }

    @Override
    public String toString() {
        return title;
    }

    public static final class Builder {
        private final String id;
        private final int number;
        private final String title;
        private String brief = "", targetName = "", resolutionReason = "", photoFile = "", photoCredit = "", similarMission = "";
        private double targetLat, targetLon, latMin, latMax, lonMin, lonMax;
        private boolean night;
        private double budgetK, monthsToLaunch, missionYears, requiredGsdM, sunDeclinationDeg, imagingDuty = 0.2;
        private List<String> facts = List.of();

        private Builder(String id, int number, String title) {
            this.id = id; this.number = number; this.title = title;
        }

        public Builder brief(String text) { brief = text; return this; }
        public Builder target(String name, double lat, double lon) { targetName = name; targetLat = lat; targetLon = lon; return this; }
        public Builder region(double latMin, double latMax, double lonMin, double lonMax) {
            this.latMin = latMin; this.latMax = latMax; this.lonMin = lonMin; this.lonMax = lonMax; return this;
        }
        public Builder atNight() { night = true; return this; }
        public Builder budget(double k) { budgetK = k; return this; }
        public Builder launchInMonths(double m) { monthsToLaunch = m; return this; }
        public Builder lasts(double years) { missionYears = years; return this; }
        public Builder resolution(double gsdM, String why) { requiredGsdM = gsdM; resolutionReason = why; return this; }
        public Builder season(double sunDeclinationDeg) { this.sunDeclinationDeg = sunDeclinationDeg; return this; }
        public Builder imagingDuty(double duty) { imagingDuty = duty; return this; }
        public Builder photo(String file, String credit) { photoFile = file; photoCredit = credit; return this; }
        public Builder similarTo(String realMission) { similarMission = realMission; return this; }
        public Builder facts(String... lines) { facts = List.of(lines); return this; }
        public Mission build() { return new Mission(this); }
    }
}
