package com.missionlab.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Lo que el equipo ha montado: una pieza por hueco, más órbita, ruta de lanzamiento y estación de tierra. */
public final class Design {
    private final EnumMap<Slot, Part> parts = new EnumMap<>(Slot.class);
    public Orbit orbit;
    /** Camino en el grafo de vuelo hasta la órbita (ver FlightNetwork). */
    public Route route;
    public GroundStation station;

    /** Instala una pieza y devuelve la que había antes en ese hueco (o null). */
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

    /** Lo que falta, en palabras sencillas. */
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
