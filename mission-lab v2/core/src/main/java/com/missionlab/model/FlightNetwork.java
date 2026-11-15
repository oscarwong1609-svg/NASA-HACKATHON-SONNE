package com.missionlab.model;

import com.missionlab.ds.DirectedWeightedGraph;
import com.missionlab.ds.DirectedWeightedGraph.Edge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El mapa de vuelo del Launch lead, construido sobre un grafo dirigido y ponderado.
 *
 *   Vértices: la base de lanzamiento y cada órbita ({@link Waypoint}).
 *   Aristas:  un vehículo que lleva de un vértice a otro ({@link Transfer}). Son dirigidas porque
 *             los viajes solo van "hacia arriba": un cohete no te devuelve a tierra y el remolcador
 *             solo sube de órbita.
 *   Pesos:    dinero (fijo + por kilo), peso máximo y horas.
 *
 * La pregunta del Launch lead ("¿cuál es la forma más barata de llegar a mi órbita con este peso?")
 * es exactamente un problema de camino mínimo, y se resuelve con Dijkstra. Las aristas cuyo vehículo
 * no puede con el peso del satélite cuentan como infinitas, así que la mejor ruta cambia mientras
 * el equipo monta piezas.
 */
public final class FlightNetwork {
    private final DirectedWeightedGraph<Waypoint, Transfer> graph = new DirectedWeightedGraph<>();
    private final Waypoint launchSite = Waypoint.launchSite();
    private final Map<Orbit, Waypoint> waypoints = new LinkedHashMap<>();

    public FlightNetwork(Collection<Orbit> orbits) {
        graph.addNode(launchSite);
        for (Orbit o : orbits) {
            Waypoint w = Waypoint.of(o);
            waypoints.put(o, w);
            graph.addNode(w);
        }
    }

    // ------------------------------------------------------------------ construir (lo usa Catalog)

    /** Arista base de lanzamiento → órbita. */
    public FlightNetwork launch(Transfer rocket, Orbit to) {
        graph.addEdge(launchSite, at(to), rocket);
        return this;
    }

    /** Arista órbita → órbita (por ejemplo, un remolcador que sube el satélite). */
    public FlightNetwork transfer(Transfer vehicle, Orbit from, Orbit to) {
        graph.addEdge(at(from), at(to), vehicle);
        return this;
    }

    // ------------------------------------------------------------------ consultar

    public Waypoint launchSite() {
        return launchSite;
    }

    public Waypoint at(Orbit orbit) {
        Waypoint w = waypoints.get(orbit);
        if (w == null) throw new IllegalArgumentException("Orbit not in the flight network: " + orbit);
        return w;
    }

    /** El grafo, para dibujarlo o recorrerlo (las pruebas también lo usan). */
    public DirectedWeightedGraph<Waypoint, Transfer> graph() {
        return graph;
    }

    /**
     * La ruta más barata hasta la órbita para un satélite de este peso (Dijkstra).
     * null si ningún camino puede con ese peso.
     */
    public Route cheapestRoute(Orbit to, double massKg) {
        List<Edge<Waypoint, Transfer>> path = graph.cheapestPath(launchSite, at(to),
                t -> t.carries(massKg) ? t.costFor(massKg) : Double.POSITIVE_INFINITY);
        return path == null || path.isEmpty() ? null : new Route(path);
    }

    /** Todas las rutas que llegan a la órbita (búsqueda en profundidad), sin mirar el peso. */
    public List<Route> routesTo(Orbit to) {
        List<Route> out = new ArrayList<>();
        for (List<Edge<Waypoint, Transfer>> p : graph.allSimplePaths(launchSite, at(to))) out.add(new Route(p));
        return out;
    }

    /** Las rutas a la órbita en el orden de la consola: primero las que pueden con el peso, de más barata a más cara. */
    public List<Route> routesFor(Orbit to, double massKg) {
        List<Route> out = routesTo(to);
        out.sort(Comparator.comparing((Route r) -> !r.carries(massKg))
                .thenComparingDouble(r -> r.costFor(massKg))
                .thenComparingInt(r -> r.legs().size()));
        return Collections.unmodifiableList(out);
    }

    /**
     * Lo que hace la consola del cohete al pulsar acción: sin ruta elegida, propone la más barata (Dijkstra);
     * con ruta, pasa a la siguiente de la lista. Devuelve null si no hay ninguna ruta a esa órbita.
     */
    public Route nextRoute(Route current, Orbit to, double massKg) {
        return stepRoute(current, to, massKg, 1);
    }

    /** Como nextRoute, pero hacia delante (dir = 1) o hacia atrás (dir = -1) en la lista. */
    public Route stepRoute(Route current, Orbit to, double massKg, int dir) {
        List<Route> options = routesFor(to, massKg);
        if (options.isEmpty()) return null;
        int i = current == null ? -1 : options.indexOf(current);
        if (i < 0) {
            Route cheapest = cheapestRoute(to, massKg);
            return cheapest != null ? cheapest : options.get(0);
        }
        int n = options.size();
        return options.get(((i + (dir < 0 ? -1 : 1)) % n + n) % n);
    }

    /** Busca una ruta por los ids de sus vehículos, por ejemplo route(POLAR_800, "ride_polar", "tug"). */
    public Route route(Orbit to, String... vehicleIds) {
        String id = String.join("+", vehicleIds);
        for (Route r : routesTo(to)) if (r.id().equals(id)) return r;
        throw new IllegalArgumentException("No route " + id + " to " + to);
    }
}
