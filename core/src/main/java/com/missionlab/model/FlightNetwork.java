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
 * The Launch lead's flight map, built on a directed weighted graph.
 *
 *   Vertices: the launch site and each orbit ({@link Waypoint}).
 *   Edges:    a vehicle that goes from one vertex to another ({@link Transfer}). They are directed because
 *             trips only go "up": a rocket does not bring you back to the ground and the space tug
 *             only raises the orbit.
 *   Weights:  money (fixed + per kilogram), maximum mass and hours.
 *
 * The Launch lead's question ("what is the cheapest way to reach my orbit at this mass?")
 * is exactly a shortest-path problem, solved with Dijkstra. Edges whose vehicle
 * cannot carry the satellite's mass count as infinite, so the best route changes as
 * the team installs parts.
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

    // ------------------------------------------------------------------ build (used by Catalog)

    /** Launch site → orbit edge. */
    public FlightNetwork launch(Transfer rocket, Orbit to) {
        graph.addEdge(launchSite, at(to), rocket);
        return this;
    }

    /** Orbit → orbit edge (for example, a space tug that raises the satellite). */
    public FlightNetwork transfer(Transfer vehicle, Orbit from, Orbit to) {
        graph.addEdge(at(from), at(to), vehicle);
        return this;
    }

    // ------------------------------------------------------------------ query

    public Waypoint launchSite() {
        return launchSite;
    }

    public Waypoint at(Orbit orbit) {
        Waypoint w = waypoints.get(orbit);
        if (w == null) throw new IllegalArgumentException("Orbit not in the flight network: " + orbit);
        return w;
    }

    /** The graph, to draw or traverse it (the tests use it too). */
    public DirectedWeightedGraph<Waypoint, Transfer> graph() {
        return graph;
    }

    /**
     * The cheapest route to the orbit for a satellite of this mass (Dijkstra).
     * null if no path can carry that mass.
     */
    public Route cheapestRoute(Orbit to, double massKg) {
        List<Edge<Waypoint, Transfer>> path = graph.cheapestPath(launchSite, at(to),
                t -> t.carries(massKg) ? t.costFor(massKg) : Double.POSITIVE_INFINITY);
        return path == null || path.isEmpty() ? null : new Route(path);
    }

    /** All routes that reach the orbit (depth-first search), ignoring mass. */
    public List<Route> routesTo(Orbit to) {
        List<Route> out = new ArrayList<>();
        for (List<Edge<Waypoint, Transfer>> p : graph.allSimplePaths(launchSite, at(to))) out.add(new Route(p));
        return out;
    }

    /** Routes to the orbit in console order: first those that can carry the mass, from cheapest to most expensive. */
    public List<Route> routesFor(Orbit to, double massKg) {
        List<Route> out = routesTo(to);
        out.sort(Comparator.comparing((Route r) -> !r.carries(massKg))
                .thenComparingDouble(r -> r.costFor(massKg))
                .thenComparingInt(r -> r.legs().size()));
        return Collections.unmodifiableList(out);
    }

    /**
     * What the rocket console does when action is pressed: with no route chosen, it proposes the cheapest (Dijkstra);
     * with a route, it moves to the next one in the list. Returns null if there is no route to that orbit.
     */
    public Route nextRoute(Route current, Orbit to, double massKg) {
        return stepRoute(current, to, massKg, 1);
    }

    /** Like nextRoute, but forwards (dir = 1) or backwards (dir = -1) through the list. */
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

    /** Finds a route by its vehicle ids, for example route(POLAR_800, "ride_polar", "tug"). */
    public Route route(Orbit to, String... vehicleIds) {
        String id = String.join("+", vehicleIds);
        for (Route r : routesTo(to)) if (r.id().equals(id)) return r;
        throw new IllegalArgumentException("No route " + id + " to " + to);
    }
}
