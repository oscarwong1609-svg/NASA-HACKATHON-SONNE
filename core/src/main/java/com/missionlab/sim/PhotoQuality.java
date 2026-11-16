package com.missionlab.sim;

/** How the final photo turns out. The result screen uses these numbers to draw it. */
public final class PhotoQuality {
    /** true if the satellite managed to take and send the photo. */
    public final boolean received;
    /** 0..1: 1 = as much detail as the scientists ask for; 0.5 = pixels twice as large. */
    public final double resolution;
    /** 0..1: fraction of the photo that reached the ground (the rest is shown black). */
    public final double completeness;
    /** false = the photo comes out black (a regular camera at night). */
    public final boolean lit;
    /** false = the orbit does not pass over the target and the photo shows the ocean. */
    public final boolean onTarget;
    /** 0..1: fraction of the mission the satellite survives in orbit. */
    public final double lifeFactor;

    public PhotoQuality(boolean received, double resolution, double completeness, boolean lit, boolean onTarget, double lifeFactor) {
        this.received = received;
        this.resolution = resolution;
        this.completeness = completeness;
        this.lit = lit;
        this.onTarget = onTarget;
        this.lifeFactor = lifeFactor;
    }

    public static PhotoQuality none() {
        return new PhotoQuality(false, 0, 0, false, false, 0);
    }

    public PhotoQuality withCompleteness(double c) {
        return new PhotoQuality(received, resolution, c, lit, onTarget, lifeFactor);
    }
}
