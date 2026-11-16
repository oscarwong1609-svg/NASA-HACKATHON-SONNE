package com.missionlab.model;

import java.util.List;

/**
 * A network of ground antennas that receive the data. Daily contact minutes are not a fixed number:
 * sim.OrbitMechanics computes them from the chosen orbit's ground track. An antenna near the pole (Svalbard)
 * sees a polar satellite on almost every orbit; a mid-latitude antenna, only a few.
 */
public final class GroundStation {
    /** One antenna: name and position. */
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
