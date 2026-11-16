package com.missionlab.model;

/**
 * A circular orbit that the team chooses on the orbit map.
 * It only stores the initial data; everything else (period, eclipse, ground track, contacts)
 * is computed by sim.OrbitMechanics with real, simplified physics.
 */
public final class Orbit {
    public final String id;
    public final String name;
    public final String description;
    public final double altitudeKm;
    /** Inclination in degrees. Over 90 = retrograde, like sun-synchronous orbits (~98°). */
    public final double inclinationDeg;
    /**
     * Local solar time at which it crosses the equator northbound (ascending node).
     * In a sun-synchronous orbit it is fixed all year; that is why it always passes over each place at the same time.
     */
    public final double ltanHours;
    /** Years before atmospheric drag brings it down, unassisted. */
    public final double lifetimeYears;
    /** The highest latitude (north or south) it passes over. */
    public final double maxLatitude;
    /** true if it is sun-synchronous (for the UI text). */
    public final boolean sunSynchronous;

    public Orbit(String id, String name, String description, double altitudeKm, double inclinationDeg,
                 double ltanHours, double lifetimeYears, boolean sunSynchronous) {
        this.id = id; this.name = name; this.description = description; this.altitudeKm = altitudeKm;
        this.inclinationDeg = inclinationDeg; this.ltanHours = ltanHours; this.lifetimeYears = lifetimeYears;
        this.sunSynchronous = sunSynchronous;
        this.maxLatitude = inclinationDeg <= 90 ? inclinationDeg : 180 - inclinationDeg;
    }

    public boolean passesOver(double latitude) {
        return Math.abs(latitude) <= maxLatitude;
    }

    @Override
    public String toString() {
        return name;
    }
}
