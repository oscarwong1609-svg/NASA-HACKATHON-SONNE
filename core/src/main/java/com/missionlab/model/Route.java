package com.missionlab.model;

import com.missionlab.ds.DirectedWeightedGraph.Edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * A PATH in the flight graph: the legs (edges) that take the satellite from the launch site to its orbit.
 * For example: Launch site → [Shared ride: polar] → 550 km → [Space tug] → 800 km.
 * It is what the Launch lead chooses in the rocket console and what the simulator walks leg by leg.
 */
public final class Route {
    private final List<Edge<Waypoint, Transfer>> legs;

    public Route(List<Edge<Waypoint, Transfer>> legs) {
        if (legs == null || legs.isEmpty()) throw new IllegalArgumentException("A route needs at least one leg");
        for (int i = 1; i < legs.size(); i++) {
            if (legs.get(i).from != legs.get(i - 1).to) throw new IllegalArgumentException("Legs must connect");
        }
        this.legs = Collections.unmodifiableList(new ArrayList<>(legs));
    }

    public List<Edge<Waypoint, Transfer>> legs() {
        return legs;
    }

    public Transfer vehicle(int leg) {
        return legs.get(leg).weight;
    }

    /** The vehicle that lifts off from the ground (the first leg). */
    public Transfer launchVehicle() {
        return vehicle(0);
    }

    public Waypoint destination() {
        return legs.get(legs.size() - 1).to.value;
    }

    public Orbit orbit() {
        return destination().orbit;
    }

    /** Sum of the (monetary) weights of all edges, for a satellite of this mass. */
    public double costFor(double massKg) {
        double k = 0;
        for (Edge<Waypoint, Transfer> e : legs) k += e.weight.costFor(massKg);
        return k;
    }

    /** The bottleneck: the satellite must fit in EVERY vehicle on the route. */
    public double capacityKg() {
        double cap = Double.POSITIVE_INFINITY;
        for (Edge<Waypoint, Transfer> e : legs) cap = Math.min(cap, e.weight.maxKg);
        return cap;
    }

    public boolean carries(double massKg) {
        return massKg <= capacityKg();
    }

    /** Months before launch by which the satellite must be delivered (set by the first vehicle). */
    public double deliveryMonths() {
        return launchVehicle().deliveryMonths;
    }

    /** Short identifier, for example "ride_polar+tug". */
    public String id() {
        StringJoiner j = new StringJoiner("+");
        for (Edge<Waypoint, Transfer> e : legs) j.add(e.weight.id);
        return j.toString();
    }

    /** The vehicles, for example "Shared ride: polar + Space tug". */
    public String label() {
        StringJoiner j = new StringJoiner(" + ");
        for (Edge<Waypoint, Transfer> e : legs) j.add(e.weight.name);
        return j.toString();
    }

    /** The stops along the path, for example "Launch site > 550 km > 800 km". */
    public String stops() {
        StringBuilder sb = new StringBuilder(legs.get(0).from.value.name);
        for (Edge<Waypoint, Transfer> e : legs) sb.append(" > ").append(e.to.value.name);
        return sb.toString();
    }

    /** Two routes are equal if they use exactly the same graph edges. */
    @Override
    public boolean equals(Object o) {
        return o instanceof Route && legs.equals(((Route) o).legs);
    }

    @Override
    public int hashCode() {
        return legs.hashCode();
    }

    @Override
    public String toString() {
        return label();
    }
}
