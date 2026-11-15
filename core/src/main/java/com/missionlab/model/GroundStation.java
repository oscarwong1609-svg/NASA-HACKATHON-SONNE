package com.missionlab.model;

import java.util.List;

/**
 * Una red de antenas en tierra que reciben los datos. Los minutos de contacto al día no son un número fijo:
 * sim.OrbitMechanics los calcula con la traza de la órbita elegida. Una antena cerca del polo (Svalbard)
 * ve a un satélite polar en casi todas las vueltas; una antena a media latitud, solo unas pocas.
 */
public final class GroundStation {
    /** Una antena: nombre y posición. */
    public static final class Site {
        public final String name;
        public final double lat;
        public final double lon;

        public Site(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    public final String id;
    public final String name;
    public final String description;
    public final double costKPerYear;
    public final List<Site> sites;

    public GroundStation(String id, String name, String description, double costKPerYear, Site... sites) {
        this.id = id; this.name = name; this.description = description; this.costKPerYear = costKPerYear;
        this.sites = List.of(sites);
    }

    @Override
    public String toString() {
        return name;
    }
}
