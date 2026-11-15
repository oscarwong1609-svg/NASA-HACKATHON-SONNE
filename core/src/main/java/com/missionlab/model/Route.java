package com.missionlab.model;

import com.missionlab.ds.DirectedWeightedGraph.Edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Un CAMINO en el grafo de vuelo: los tramos (aristas) que llevan el satélite de la base a su órbita.
 * Por ejemplo: Launch site → [Shared ride: polar] → 550 km → [Space tug] → 800 km.
 * Es lo que elige el Launch lead en la consola del cohete y lo que el simulador recorre tramo a tramo.
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

    /** El vehículo que despega desde tierra (el primer tramo). */
    public Transfer launchVehicle() {
        return vehicle(0);
    }

    public Waypoint destination() {
        return legs.get(legs.size() - 1).to.value;
    }

    public Orbit orbit() {
        return destination().orbit;
    }

    /** Suma de los pesos (en dinero) de todas las aristas, para un satélite de este peso. */
    public double costFor(double massKg) {
        double k = 0;
        for (Edge<Waypoint, Transfer> e : legs) k += e.weight.costFor(massKg);
        return k;
    }

    /** El cuello de botella: el satélite tiene que caber en TODOS los vehículos de la ruta. */
    public double capacityKg() {
        double cap = Double.POSITIVE_INFINITY;
        for (Edge<Waypoint, Transfer> e : legs) cap = Math.min(cap, e.weight.maxKg);
        return cap;
    }

    public boolean carries(double massKg) {
        return massKg <= capacityKg();
    }

    /** Meses antes del lanzamiento en los que hay que entregar el satélite (lo pide el primer vehículo). */
    public double deliveryMonths() {
        return launchVehicle().deliveryMonths;
    }

    /** Identificador corto, por ejemplo "ride_polar+tug". */
    public String id() {
        StringJoiner j = new StringJoiner("+");
        for (Edge<Waypoint, Transfer> e : legs) j.add(e.weight.id);
        return j.toString();
    }

    /** Los vehículos, por ejemplo "Shared ride: polar + Space tug". */
    public String label() {
        StringJoiner j = new StringJoiner(" + ");
        for (Edge<Waypoint, Transfer> e : legs) j.add(e.weight.name);
        return j.toString();
    }

    /** Las paradas del camino, por ejemplo "Launch site > 550 km > 800 km". */
    public String stops() {
        StringBuilder sb = new StringBuilder(legs.get(0).from.value.name);
        for (Edge<Waypoint, Transfer> e : legs) sb.append(" > ").append(e.to.value.name);
        return sb.toString();
    }

    /** Dos rutas son iguales si usan exactamente las mismas aristas del grafo. */
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
