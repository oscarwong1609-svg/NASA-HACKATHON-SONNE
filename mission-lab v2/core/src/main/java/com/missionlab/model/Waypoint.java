package com.missionlab.model;

import java.util.Locale;

/** Un VÉRTICE del grafo de vuelo: la base de lanzamiento o una de las órbitas. */
public final class Waypoint {
    public final String id;
    /** Nombre corto para dibujar rutas: "Launch site", "550 km"... */
    public final String name;
    /** La órbita de este punto, o null si es la base de lanzamiento (en tierra). */
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
