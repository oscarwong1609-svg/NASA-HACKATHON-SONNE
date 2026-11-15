package com.missionlab.hangar;

import com.missionlab.data.Catalog;
import com.missionlab.model.Design;
import com.missionlab.model.Role;

import java.util.List;

/**
 * Un mueble del hangar que no se carga: el mapa de órbitas, la consola del cohete y la mesa de estaciones.
 * Al usarlo se abre su consola a pantalla completa; dentro, izquierda/derecha cambian la opción.
 * El mapa de órbitas y la consola del cohete son las dos mitades del grafo de vuelo:
 * el mapa elige el VÉRTICE de destino y la consola elige el CAMINO hasta él.
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
     * Pasa a la opción siguiente (dir = 1) o anterior (dir = -1) y devuelve su nombre.
     * Devuelve null si todavía no se puede usar (la consola del cohete necesita una órbita elegida).
     *
     * @param massKg peso actual del satélite: la consola ordena las rutas según lo que cuestan con ese peso.
     */
    public String cycle(Design d, double massKg, int dir) {
        switch (kind) {
            case ORBIT:
                d.orbit = step(Catalog.ORBITS, d.orbit, dir);
                // La ruta anterior llevaba a otra órbita: hay que trazar el camino de nuevo.
                d.route = null;
                return d.orbit.name;
            case ROCKET:
                if (d.orbit == null) return null;
                // Primera vez: la ruta más barata (Dijkstra). Después: el resto, de más barata a más cara.
                d.route = Catalog.FLIGHTS.stepRoute(d.route, d.orbit, massKg, dir);
                return d.route == null ? null : d.route.label();
            default:
                d.station = step(Catalog.STATIONS, d.station, dir);
                return d.station.name;
        }
    }

    /** La opción elegida ahora, o null. */
    public Object current(Design d) {
        switch (kind) {
            case ORBIT: return d.orbit;
            case ROCKET: return d.route;
            default: return d.station;
        }
    }

    /** Qué es la opción elegida, en palabras. Para una ruta: qué hace cada vehículo, en orden. */
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
        // Ojo: List.of(...).indexOf(null) lanza una excepción, así que null se trata aparte.
        int n = options.size();
        if (current == null) return dir < 0 ? options.get(n - 1) : options.get(0);
        int i = options.indexOf(current);
        return options.get(((i + (dir < 0 ? -1 : 1)) % n + n) % n);
    }
}
