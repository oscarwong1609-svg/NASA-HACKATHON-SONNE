package com.missionlab.model;

/**
 * A satellite part. All parts have mass, power draw, price, lead time and reliability.
 * The other fields only matter for their slot type (for example, batteryWh only for batteries).
 * The numbers are simplified for teaching, but in the order of magnitude of real CubeSat parts.
 */
public final class Part {
    public final String id;
    public final String name;
    public final String description;
    public final Slot slot;

    public final double massKg;
    /**
     * Power draw in watts WHILE OPERATING. The camera only draws power while imaging and the radio only while
     * transmitting; attitude control draws power all the time. Budgets scales each draw by its duty cycle.
     */
    public final double powerW;
    /** Price in thousands of dollars. */
    public final double costK;
    /** Lead time in months. Parts are ordered together: the slowest one sets the schedule. */
    public final double leadMonths;
    /** Probability of working (0..1). Affects the reliability score. */
    public final double reliability;
    /** If true, games with 2+ players need two people to carry it. */
    public final boolean heavy;
    /** Volume inside the structure, in U (1U = a 10 cm cube). Solar panels mount outside. */
    public final double volumeU;

    // Instrument
    /**
     * Instantaneous field of view of one pixel, in microradians. Ground resolution (m) = altitude (km) × IFOV / 1000.
     * That is why the same camera sees more detail from a lower orbit.
     */
    public final double ifovMicroRad;
    /** Data from each day's target image, in GB. */
    public final double dataGbPerDay;
    /** Pointing accuracy required (0..1). Narrow-field cameras need very good pointing. */
    public final double pointingNeed;
    public final boolean seesAtNight;
    // Solar panels
    /** Watts while in sunlight. */
    public final double generatesW;
    /** Panels deployed on hinges (they can jam). */
    public final boolean deployable;
    // Battery
    public final double batteryWh;
    // Attitude control
    public final double stability;
    // Radio
    public final double downlinkMbps;
    // Structure
    /** Maximum total satellite mass with this structure (CubeSat Design Specification: 2 kg per U). */
    public final double frameMaxKg;
    /** Volume the structure offers, in U. */
    public final double frameVolumeU;

    private Part(Builder b) {
        id = b.id; name = b.name; description = b.description; slot = b.slot;
        massKg = b.massKg; powerW = b.powerW; costK = b.costK; leadMonths = b.leadMonths;
        reliability = b.reliability; heavy = b.heavy; volumeU = b.volumeU;
        ifovMicroRad = b.ifovMicroRad; dataGbPerDay = b.dataGbPerDay; pointingNeed = b.pointingNeed;
        seesAtNight = b.seesAtNight; generatesW = b.generatesW; deployable = b.deployable; batteryWh = b.batteryWh;
        stability = b.stability; downlinkMbps = b.downlinkMbps; frameMaxKg = b.frameMaxKg; frameVolumeU = b.frameVolumeU;
    }

    public static Builder of(String id, Slot slot, String name) {
        return new Builder(id, slot, name);
    }

    /** Ground resolution (metres per pixel) from this altitude. Instruments only. */
    public double groundResolutionM(double altitudeKm) {
        return altitudeKm * ifovMicroRad / 1000.0;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final class Builder {
        private final String id;
        private final Slot slot;
        private final String name;
        private String description = "";
        private double massKg, powerW, costK, leadMonths = 1, reliability = 0.97, volumeU;
        private boolean heavy;
        private double ifovMicroRad, dataGbPerDay, pointingNeed;
        private boolean seesAtNight, deployable;
        private double generatesW, batteryWh, stability, downlinkMbps, frameMaxKg, frameVolumeU;

        private Builder(String id, Slot slot, String name) {
            this.id = id; this.slot = slot; this.name = name;
        }

        public Builder text(String d) { description = d; return this; }
        public Builder mass(double kg) { massKg = kg; return this; }
        public Builder power(double w) { powerW = w; return this; }
        public Builder cost(double k) { costK = k; return this; }
        public Builder lead(double months) { leadMonths = months; return this; }
        public Builder reliability(double r) { reliability = r; return this; }
        public Builder heavy() { heavy = true; return this; }
        public Builder volume(double u) { volumeU = u; return this; }
        public Builder camera(double ifovMicroRad, double gbPerDay, double needsPointing, boolean night) {
            this.ifovMicroRad = ifovMicroRad; dataGbPerDay = gbPerDay; pointingNeed = needsPointing; seesAtNight = night;
            return this;
        }
        public Builder generates(double w) { generatesW = w; return this; }
        public Builder deployable() { deployable = true; return this; }
        public Builder stores(double wh) { batteryWh = wh; return this; }
        public Builder stability(double s) { stability = s; return this; }
        public Builder downlink(double mbps) { downlinkMbps = mbps; return this; }
        /** A structure of "units" U: CubeSat Design Specification, up to 2 kg per U. */
        public Builder frame(double units) { frameVolumeU = units; frameMaxKg = 2.0 * units; return this; }
        public Part build() { return new Part(this); }
    }
}
