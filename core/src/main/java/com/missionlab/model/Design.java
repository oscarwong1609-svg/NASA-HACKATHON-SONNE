package com.missionlab.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** What the team has built: one part per slot, plus orbit, launch route and ground station. */
public final class Design {
    private final EnumMap<Slot, Part> parts = new EnumMap<>(Slot.class);
    public Orbit orbit;
    /** Path in the flight graph to the orbit (see FlightNetwork). */
    public Route route;
    public GroundStation station;

    /** Installs a part and returns the one previously in that slot (or null). */
    public Part install(Part part) {
        return parts.put(part.slot, part);
    }

    public Part remove(Slot slot) {
        return parts.remove(slot);
    }

    public Part get(Slot slot) {
        return parts.get(slot);
    }

    public Collection<Part> parts() {
        return parts.values();
    }

    public boolean isComplete() {
        return missing().isEmpty();
    }

    /** What is missing, in plain words. */
    public List<String> missing() {
        List<String> out = new ArrayList<>();
        for (Slot s : Slot.values()) {
            if (!parts.containsKey(s)) out.add(s.label.toLowerCase());
        }
        if (orbit == null) out.add("orbit");
        if (route == null) out.add("launch route");
        if (station == null) out.add("ground station");
        return out;
    }

    public Design copy() {
        Design d = new Design();
        for (Map.Entry<Slot, Part> e : parts.entrySet()) d.parts.put(e.getKey(), e.getValue());
        d.orbit = orbit;
        d.route = route;
        d.station = station;
        return d;
    }
}
