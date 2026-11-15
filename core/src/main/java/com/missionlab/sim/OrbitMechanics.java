package com.missionlab.sim;

import com.missionlab.model.GroundStation;
import com.missionlab.model.Orbit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Física de órbitas circulares, simplificada pero de verdad (Tierra esférica, sin perturbaciones):
 * periodo, velocidad, traza en el suelo, eclipses, día y noche, y contactos con las estaciones de tierra.
 *
 * Todo lo que el jugador ve (mapa de órbitas, mesa de estaciones, Mission Control) y todo lo que se calcula
 * (Budgets, Simulator) sale de aquí, así los números nunca se contradicen.
 *
 * Convenio: en t = 0 es medianoche en Greenwich (el Sol está sobre la longitud 180°).
 */
public final class OrbitMechanics {
    private OrbitMechanics() {}

    public static final double EARTH_RADIUS_KM = 6371.0;
    /** Constante gravitatoria de la Tierra (km³/s²). */
    public static final double MU = 398600.4418;
    /** La Tierra gira 360° en un día sidéreo. */
    public static final double EARTH_ROTATION_DEG_PER_S = 360.0 / 86164.0905;
    /** Una estación ve al satélite cuando está al menos 10° por encima del horizonte. */
    public static final double MIN_ELEVATION_DEG = 10;
    /** Ascensión recta del Sol en t = 0 (grados). */
    private static final double SUN_RA_DEG = 180;

    /** Por cada (órbita, red): {minutos de contacto al día, número de pasadas al día}. */
    private static final Map<String, double[]> CONTACT_CACHE = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ números de la órbita

    public static double semiMajorAxisKm(Orbit o) {
        return EARTH_RADIUS_KM + o.altitudeKm;
    }

    /** Tiempo de una vuelta (tercera ley de Kepler). */
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
     * Radio (en grados de arco sobre la Tierra) del círculo desde el que una estación ve al satélite
     * por encima de MIN_ELEVATION_DEG. Más alta la órbita, más grande el círculo y más largos los contactos.
     */
    public static double coverageRadiusDeg(Orbit o) {
        double e = Math.toRadians(MIN_ELEVATION_DEG);
        double ratio = EARTH_RADIUS_KM / semiMajorAxisKm(o);
        return Math.toDegrees(Math.acos(ratio * Math.cos(e)) - e);
    }

    /** Ángulo entre el plano de la órbita y el Sol (beta). Con beta grande, el satélite casi no entra en la sombra. */
    public static double betaAngleDeg(Orbit o, double sunDeclinationDeg) {
        double[] n = orbitNormal(o);
        double[] s = sunVector(sunDeclinationDeg);
        return Math.toDegrees(Math.asin(clamp(dot(n, s), -1, 1)));
    }

    /** Minutos de cada vuelta que el satélite pasa en la sombra de la Tierra (solo funciona con la batería). */
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
     * Minutos de contacto al día entre la órbita y la red de estaciones (se simula un día entero cada 20 s).
     * Se guarda en caché: solo hay unas pocas combinaciones.
     */
    public static double contactMinutesPerDay(Orbit o, GroundStation gs) {
        return contacts(o, gs)[0];
    }

    /** Número de pasadas (contactos separados) al día entre la órbita y la red de estaciones. */
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

    /** La primera antena de la red que ve al satélite, o null. */
    public static GroundStation.Site siteInView(GroundStation gs, double satLat, double satLon, double radiusDeg) {
        for (GroundStation.Site s : gs.sites) {
            if (centralAngleDeg(satLat, satLon, s.lat, s.lon) <= radiusDeg) return s;
        }
        return null;
    }

    // ------------------------------------------------------------------ Sol y suelo

    /** Longitud bajo el Sol en el instante t. */
    public static double subsolarLongitude(double tSeconds) {
        return wrapLon(SUN_RA_DEG - EARTH_ROTATION_DEG_PER_S * tSeconds);
    }

    /** Altura del Sol sobre el horizonte (grados) en un punto del suelo. Negativa = es de noche. */
    public static double sunElevationDeg(double lat, double lon, double tSeconds, double sunDeclinationDeg) {
        double phi = Math.toRadians(lat), d = Math.toRadians(sunDeclinationDeg);
        double hourAngle = Math.toRadians(lon - subsolarLongitude(tSeconds));
        double s = Math.sin(phi) * Math.sin(d) + Math.cos(phi) * Math.cos(d) * Math.cos(hourAngle);
        return Math.toDegrees(Math.asin(clamp(s, -1, 1)));
    }

    /** Distancia angular entre dos puntos de la Tierra, en grados. */
    public static double centralAngleDeg(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2), dl = Math.toRadians(lon2 - lon1);
        double c = Math.sin(p1) * Math.sin(p2) + Math.cos(p1) * Math.cos(p2) * Math.cos(dl);
        return Math.toDegrees(Math.acos(clamp(c, -1, 1)));
    }

    public static double wrapLon(double lon) {
        double l = ((lon + 180) % 360 + 360) % 360 - 180;
        return l == -180 ? 180 : l;
    }

    // ------------------------------------------------------------------ dónde está el satélite

    /** Posición del satélite en un instante. */
    public static final class State {
        public final double lat, lon;
        /** false = está en la sombra de la Tierra. */
        public final boolean sunlit;

        State(double lat, double lon, boolean sunlit) {
            this.lat = lat;
            this.lon = lon;
            this.sunlit = sunlit;
        }
    }

    /** La trayectoria de un satélite en una órbita concreta. */
    public static final class Track {
        private final Orbit orbit;
        private final double sunDeclinationDeg;
        private final double raan, inc, u0, periodS, a;

        /**
         * @param startArgDeg dónde empieza el satélite dentro de la órbita (0 = cruzando el ecuador hacia el norte)
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

    // ------------------------------------------------------------------ geometría interna

    /** Longitud del nodo ascendente: la pone la hora local del cruce (LTAN) respecto al Sol. */
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
