package com.missionlab.model;

import java.util.Locale;

/** A VERTEX of the flight graph: the launch site or one of the orbits. */
public final class Waypoint {
    public final String id;
    /** Short name for drawing routes: "Launch site", "550 km"... */
    public final String name;
    /** This point's orbit, or null if it is the launch site (on the ground). */
    public final Orbit orbit;

    private Waypoint(String id, String name, Orbit orbit) {
        this.id = id;
        this.name = name;
        this.orbit = orbit;
    }

    public static Waypoint launchSite() {
        return new Waypoint("launch_site", "Launch site", null);
    }

    public static Waypoint of(Orbit orbit) {
        return new Waypoint(orbit.id, String.format(Locale.ROOT, "%.0f km", orbit.altitudeKm), orbit);
    }

    public boolean onGround() {
        return orbit == null;
    }

    @Override
    public String toString() {
        return name;
    }
}
