package com.missionlab.sim;

import com.missionlab.model.GroundStation;
import com.missionlab.model.Orbit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Circular-orbit physics, simplified but real (spherical Earth, no perturbations):
 * period, velocity, ground track, eclipses, day and night, and ground station contacts.
 *
 * Everything the player sees (orbit map, ground station desk, Mission Control) and everything computed
 * (Budgets, Simulator) comes from here, so the numbers never contradict each other.
 *
 * Convention: at t = 0 it is midnight at Greenwich (the Sun is over longitude 180°).
 */
public final class OrbitMechanics {
    private OrbitMechanics() {}

    public static final double EARTH_RADIUS_KM = 6371.0;
    /** Earth's gravitational parameter (km³/s²). */
    public static final double MU = 398600.4418;
    /** The Earth rotates 360° in one sidereal day. */
    public static final double EARTH_ROTATION_DEG_PER_S = 360.0 / 86164.0905;
    /** A station sees the satellite when it is at least 10° above the horizon. */
    public static final double MIN_ELEVATION_DEG = 10;
    /** Right ascension of the Sun at t = 0 (degrees). */
    private static final double SUN_RA_DEG = 180;

    /** For each (orbit, network): {contact minutes per day, number of passes per day}. */
    private static final Map<String, double[]> CONTACT_CACHE = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ orbit figures

    public static double semiMajorAxisKm(Orbit o) {
        return EARTH_RADIUS_KM + o.altitudeKm;
    }

    /** Time for one orbit (Kepler's third law). */
    public static double periodMinutes(Orbit o) {
        double a = semiMajorAxisKm(o);
        return 2 * Math.PI * Math.sqrt(a * a * a / MU) / 60.0;
    }

    public static double speedKmS(Orbit o) {
        return Math.sqrt(MU / semiMajorAxisKm(o));
    }

    public static double orbitsPerDay(Orbit o) {
        return 1440.0 / periodMinutes(o);
    }

    /**
     * Radius (in degrees of arc over the Earth) of the circle from which a station sees the satellite
     * above MIN_ELEVATION_DEG. The higher the orbit, the larger the circle and the longer the contacts.
     */
    public static double coverageRadiusDeg(Orbit o) {
        double e = Math.toRadians(MIN_ELEVATION_DEG);
        double ratio = EARTH_RADIUS_KM / semiMajorAxisKm(o);
        return Math.toDegrees(Math.acos(ratio * Math.cos(e)) - e);
    }

    /** Angle between the orbital plane and the Sun (beta). With a large beta, the satellite barely enters eclipse. */
    public static double betaAngleDeg(Orbit o, double sunDeclinationDeg) {
        double[] n = orbitNormal(o);
        double[] s = sunVector(sunDeclinationDeg);
        return Math.toDegrees(Math.asin(clamp(dot(n, s), -1, 1)));
    }

    /** Minutes of each orbit the satellite spends in the Earth's shadow (running on battery only). */
    public static double eclipseMinutes(Orbit o, double sunDeclinationDeg) {
        double a = semiMajorAxisKm(o), h = o.altitudeKm;
        double beta = Math.toRadians(betaAngleDeg(o, sunDeclinationDeg));
        double betaStar = Math.asin(EARTH_RADIUS_KM / a);
        if (Math.abs(beta) >= betaStar) return 0;
        double x = Math.sqrt(h * h + 2 * EARTH_RADIUS_KM * h) / (a * Math.cos(beta));
        return periodMinutes(o) * Math.acos(clamp(x, -1, 1)) / Math.PI;
    }

    public static double sunlitFraction(Orbit o, double sunDeclinationDeg) {
        return 1 - eclipseMinutes(o, sunDeclinationDeg) / periodMinutes(o);
    }

    /**
     * Daily contact minutes between the orbit and the station network (a whole day is simulated at 20 s steps).
     * Cached: there are only a few combinations.
     */
    public static double contactMinutesPerDay(Orbit o, GroundStation gs) {
        return contacts(o, gs)[0];
    }

    /** Number of passes (separate contacts) per day between the orbit and the station network. */
    public static int contactPassesPerDay(Orbit o, GroundStation gs) {
        return (int) contacts(o, gs)[1];
    }

    private static double[] contacts(Orbit o, GroundStation gs) {
        return CONTACT_CACHE.computeIfAbsent(o.id + "|" + gs.id, k -> {
            Track t = new Track(o, 0, 0);
            double radius = coverageRadiusDeg(o);
            int visible = 0, passes = 0;
            boolean in = false;
            for (double s = 0; s < 86400; s += 20) {
                State st = t.at(s);
                boolean now = siteInView(gs, st.lat, st.lon, radius) != null;
                if (now) visible++;
                if (now && !in) passes++;
                in = now;
            }
            return new double[] {visible * 20 / 60.0, passes};
        });
    }

    /** The first antenna in the network that sees the satellite, or null. */
    public static GroundStation.Site siteInView(GroundStation gs, double satLat, double satLon, double radiusDeg) {
        for (GroundStation.Site s : gs.sites) {
            if (centralAngleDeg(satLat, satLon, s.lat, s.lon) <= radiusDeg) return s;
        }
        return null;
    }

    // ------------------------------------------------------------------ Sun and ground

    /** Subsolar longitude at instant t. */
    public static double subsolarLongitude(double tSeconds) {
        return wrapLon(SUN_RA_DEG - EARTH_ROTATION_DEG_PER_S * tSeconds);
    }

    /** Solar elevation above the horizon (degrees) at a ground point. Negative = night. */
    public static double sunElevationDeg(double lat, double lon, double tSeconds, double sunDeclinationDeg) {
        double phi = Math.toRadians(lat), d = Math.toRadians(sunDeclinationDeg);
        double hourAngle = Math.toRadians(lon - subsolarLongitude(tSeconds));
        double s = Math.sin(phi) * Math.sin(d) + Math.cos(phi) * Math.cos(d) * Math.cos(hourAngle);
        return Math.toDegrees(Math.asin(clamp(s, -1, 1)));
    }

    /** Angular distance between two points on the Earth, in degrees. */
    public static double centralAngleDeg(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2), dl = Math.toRadians(lon2 - lon1);
        double c = Math.sin(p1) * Math.sin(p2) + Math.cos(p1) * Math.cos(p2) * Math.cos(dl);
        return Math.toDegrees(Math.acos(clamp(c, -1, 1)));
    }

    public static double wrapLon(double lon) {
        double l = ((lon + 180) % 360 + 360) % 360 - 180;
        return l == -180 ? 180 : l;
    }

    // ------------------------------------------------------------------ where the satellite is

    /** Satellite position at an instant. */
    public static final class State {
        public final double lat, lon;
        /** false = it is in the Earth's shadow. */
        public final boolean sunlit;

        State(double lat, double lon, boolean sunlit) {
            this.lat = lat;
            this.lon = lon;
            this.sunlit = sunlit;
        }
    }

    /** The trajectory of a satellite in a specific orbit. */
    public static final class Track {
        private final Orbit orbit;
        private final double sunDeclinationDeg;
        private final double raan, inc, u0, periodS, a;

        /**
         * @param startArgDeg where the satellite starts within the orbit (0 = crossing the equator northbound)
         */
        public Track(Orbit orbit, double sunDeclinationDeg, double startArgDeg) {
            this.orbit = orbit;
            this.sunDeclinationDeg = sunDeclinationDeg;
            this.raan = raanDeg(orbit);
            this.inc = Math.toRadians(orbit.inclinationDeg);
            this.u0 = startArgDeg;
            this.periodS = periodMinutes(orbit) * 60;
            this.a = semiMajorAxisKm(orbit);
        }

        public Orbit orbit() {
            return orbit;
        }

        public double periodSeconds() {
            return periodS;
        }

        public State at(double tSeconds) {
            double u = Math.toRadians(u0 + 360.0 * tSeconds / periodS);
            double om = Math.toRadians(raan);
            double x = Math.cos(om) * Math.cos(u) - Math.sin(om) * Math.sin(u) * Math.cos(inc);
            double y = Math.sin(om) * Math.cos(u) + Math.cos(om) * Math.sin(u) * Math.cos(inc);
            double z = Math.sin(u) * Math.sin(inc);
            double lat = Math.toDegrees(Math.asin(clamp(z, -1, 1)));
            double lon = wrapLon(Math.toDegrees(Math.atan2(y, x)) - EARTH_ROTATION_DEG_PER_S * tSeconds);
            double[] s = sunVector(sunDeclinationDeg);
            double proj = x * s[0] + y * s[1] + z * s[2];
            boolean sunlit = proj >= 0 || a * a * (1 - proj * proj) >= EARTH_RADIUS_KM * EARTH_RADIUS_KM;
            return new State(lat, lon, sunlit);
        }
    }

    // ------------------------------------------------------------------ internal geometry

    /** Longitude of the ascending node: set by the local time of the crossing (LTAN) relative to the Sun. */
    private static double raanDeg(Orbit o) {
        return SUN_RA_DEG + (o.ltanHours - 12) * 15;
    }

    private static double[] orbitNormal(Orbit o) {
        double om = Math.toRadians(raanDeg(o)), i = Math.toRadians(o.inclinationDeg);
        return new double[] {Math.sin(om) * Math.sin(i), -Math.cos(om) * Math.sin(i), Math.cos(i)};
    }

    private static double[] sunVector(double declinationDeg) {
        double d = Math.toRadians(declinationDeg), ra = Math.toRadians(SUN_RA_DEG);
        return new double[] {Math.cos(d) * Math.cos(ra), Math.cos(d) * Math.sin(ra), Math.sin(d)};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
