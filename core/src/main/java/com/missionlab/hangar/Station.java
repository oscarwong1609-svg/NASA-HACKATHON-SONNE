package com.missionlab.hangar;

import com.missionlab.data.Catalog;
import com.missionlab.model.Design;
import com.missionlab.model.Role;

import java.util.List;

/**
 * A hangar fixture that cannot be carried: the orbit map, the rocket console and the ground station desk.
 * Using it opens its full-screen console; inside, left/right changes the option.
 * The orbit map and the rocket console are the two halves of the flight graph:
 * the map chooses the destination VERTEX and the console chooses the PATH to it.
 */
public final class Station {
    public enum Kind {
        ORBIT("Orbit map", Role.LAUNCH),
        ROCKET("Rocket console", Role.LAUNCH),
        GROUND("Ground station desk", Role.GROUND);

        public final String label;
        public final Role owner;

        Kind(String label, Role owner) {
            this.label = label;
            this.owner = owner;
        }
    }

    public final Kind kind;
    public final float x, y, w, h;

    public Station(Kind kind, float x, float y, float w, float h) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public float cx() {
        return x + w / 2;
    }

    public float cy() {
        return y + h / 2;
    }

    /**
     * Moves to the next (dir = 1) or previous (dir = -1) option and returns its name.
     * Returns null if it cannot be used yet (the rocket console needs a chosen orbit).
     *
     * @param massKg current satellite mass: the console sorts routes by what they cost at that mass.
     */
    public String cycle(Design d, double massKg, int dir) {
        switch (kind) {
            case ORBIT:
                d.orbit = step(Catalog.ORBITS, d.orbit, dir);
                // The previous route led to another orbit: the path must be planned again.
                d.route = null;
                return d.orbit.name;
            case ROCKET:
                if (d.orbit == null) return null;
                // First time: the cheapest route (Dijkstra). After that: the rest, from cheapest to most expensive.
                d.route = Catalog.FLIGHTS.stepRoute(d.route, d.orbit, massKg, dir);
                return d.route == null ? null : d.route.label();
            default:
                d.station = step(Catalog.STATIONS, d.station, dir);
                return d.station.name;
        }
    }

    /** The currently chosen option, or null. */
    public Object current(Design d) {
        switch (kind) {
            case ORBIT: return d.orbit;
            case ROCKET: return d.route;
            default: return d.station;
        }
    }

    /** What the chosen option is, in words. For a route: what each vehicle does, in order. */
    public String currentDescription(Design d) {
        switch (kind) {
            case ORBIT:
                return d.orbit == null ? null : d.orbit.description;
            case ROCKET: {
                if (d.route == null) return null;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < d.route.legs().size(); i++) sb.append(i > 0 ? " " : "").append(d.route.vehicle(i).description);
                return sb.toString();
            }
            default:
                return d.station == null ? null : d.station.description;
        }
    }

    private static <T> T step(List<T> options, T current, int dir) {
        // Note: List.of(...).indexOf(null) throws an exception, so null is handled separately.
        int n = options.size();
        if (current == null) return dir < 0 ? options.get(n - 1) : options.get(0);
        int i = options.indexOf(current);
        return options.get(((i + (dir < 0 ? -1 : 1)) % n + n) % n);
    }
}
