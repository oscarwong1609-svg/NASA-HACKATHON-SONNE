package com.missionlab.ds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * ESTRUCTURA NO LINEAL: grafo dirigido y ponderado, guardado como lista de adyacencia.
 *
 * - Cada {@link Node} guarda un valor (V) y la lista de aristas que SALEN de él.
 * - Cada {@link Edge} va en un solo sentido (from → to) y lleva un peso (W).
 *
 * El peso es genérico para que una arista pueda llevar varios recursos a la vez
 * (en Mission Lab: dinero fijo, dinero por kilo, peso máximo y horas de viaje).
 * Cuando se busca un camino, una función convierte cada peso en un número;
 * si devuelve infinito, esa arista no se puede usar en esa búsqueda.
 *
 * No depende de libGDX ni del juego, así que se prueba con tests normales.
 *
 * @param <V> valor de cada vértice (debe tener equals/hashCode coherentes)
 * @param <W> peso de cada arista
 */
public final class DirectedWeightedGraph<V, W> {

    /** Un vértice: su valor y las aristas que salen de él (lista de adyacencia). */
    public static final class Node<V, W> {
        public final V value;
        private final List<Edge<V, W>> outgoing = new ArrayList<>();

        private Node(V value) {
            this.value = value;
        }

        /** Aristas que salen de este vértice, en el orden en que se añadieron. */
        public List<Edge<V, W>> outgoing() {
            return Collections.unmodifiableList(outgoing);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /** Una arista dirigida from → to con su peso. Dos aristas distintas nunca son iguales (identidad). */
    public static final class Edge<V, W> {
        public final Node<V, W> from;
        public final Node<V, W> to;
        public final W weight;

        private Edge(Node<V, W> from, Node<V, W> to, W weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return from + " -> " + to + " [" + weight + "]";
        }
    }

    /** Índice valor → vértice. LinkedHashMap para que recorrer los vértices sea siempre en el mismo orden. */
    private final Map<V, Node<V, W>> nodes = new LinkedHashMap<>();
    private int edgeCount;

    // ------------------------------------------------------------------ construir

    /** Añade un vértice (si ya existía, devuelve el que había). */
    public Node<V, W> addNode(V value) {
        if (value == null) throw new IllegalArgumentException("A node needs a value");
        return nodes.computeIfAbsent(value, Node::new);
    }

    /** Añade una arista dirigida. Los vértices se crean si no existían. */
    public Edge<V, W> addEdge(V from, V to, W weight) {
        if (weight == null) throw new IllegalArgumentException("An edge needs a weight");
        Node<V, W> a = addNode(from);
        Node<V, W> b = addNode(to);
        Edge<V, W> e = new Edge<>(a, b, weight);
        a.outgoing.add(e);
        edgeCount++;
        return e;
    }

    // ------------------------------------------------------------------ consultar

    public boolean contains(V value) {
        return nodes.containsKey(value);
    }

    public Node<V, W> node(V value) {
        Node<V, W> n = nodes.get(value);
        if (n == null) throw new IllegalArgumentException("Not in the graph: " + value);
        return n;
    }

    public List<Edge<V, W>> outgoing(V value) {
        return node(value).outgoing();
    }

    /** Todos los vértices, siempre en el mismo orden (el orden en que se añadieron). */
    public java.util.Collection<Node<V, W>> nodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edgeCount;
    }

    // ------------------------------------------------------------------ algoritmos

    /**
     * Camino más barato de source a target (algoritmo de Dijkstra con una cola de prioridad).
     *
     * @param cost convierte el peso de una arista en un coste ≥ 0.
     *             Double.POSITIVE_INFINITY = esta arista no se puede usar ahora.
     * @return las aristas del camino en orden, una lista vacía si source == target,
     *         o null si target no se puede alcanzar.
     */
    public List<Edge<V, W>> cheapestPath(V source, V target, ToDoubleFunction<W> cost) {
        Node<V, W> start = node(source);
        Node<V, W> goal = node(target);

        Map<Node<V, W>, Double> best = new HashMap<>();          // coste mínimo conocido hasta cada vértice
        Map<Node<V, W>, Edge<V, W>> arrivedBy = new HashMap<>(); // última arista del mejor camino conocido
        Set<Node<V, W>> settled = new HashSet<>();               // vértices cuyo coste ya es definitivo
        PriorityQueue<Visit<V, W>> frontier = new PriorityQueue<>();

        best.put(start, 0.0);
        frontier.add(new Visit<>(start, 0.0));
        while (!frontier.isEmpty()) {
            Visit<V, W> v = frontier.poll();
            if (!settled.add(v.node)) continue; // entrada vieja: ya se encontró algo más barato
            if (v.node == goal) break;
            for (Edge<V, W> e : v.node.outgoing) {
                double w = cost.applyAsDouble(e.weight);
                if (Double.isNaN(w) || w < 0) throw new IllegalStateException("Dijkstra needs costs >= 0, got " + w + " on " + e);
                if (Double.isInfinite(w) || settled.contains(e.to)) continue;
                double candidate = v.cost + w;
                if (candidate < best.getOrDefault(e.to, Double.POSITIVE_INFINITY)) {
                    best.put(e.to, candidate);
                    arrivedBy.put(e.to, e);
                    frontier.add(new Visit<>(e.to, candidate));
                }
            }
        }
        if (!best.containsKey(goal)) return null;

        // Reconstruir el camino yendo hacia atrás desde el destino.
        List<Edge<V, W>> path = new ArrayList<>();
        for (Node<V, W> at = goal; at != start; at = arrivedBy.get(at).from) path.add(arrivedBy.get(at));
        Collections.reverse(path);
        return path;
    }

    /**
     * Todos los caminos simples (sin repetir vértices) de source a target, por búsqueda en profundidad.
     * Sirve para enseñar al jugador TODAS las rutas posibles, no solo la más barata.
     * Ojo: en grafos grandes puede haber muchísimos caminos; el de Mission Lab es pequeño.
     */
    public List<List<Edge<V, W>>> allSimplePaths(V source, V target) {
        List<List<Edge<V, W>>> out = new ArrayList<>();
        Node<V, W> goal = node(target);
        dfs(node(source), goal, new HashSet<>(), new ArrayList<>(), out);
        return out;
    }

    private void dfs(Node<V, W> at, Node<V, W> goal, Set<Node<V, W>> onPath,
                     List<Edge<V, W>> path, List<List<Edge<V, W>>> out) {
        if (at == goal) {
            if (!path.isEmpty()) out.add(new ArrayList<>(path));
            return;
        }
        onPath.add(at);
        for (Edge<V, W> e : at.outgoing) {
            if (onPath.contains(e.to)) continue; // evita ciclos
            path.add(e);
            dfs(e.to, goal, onPath, path, out);
            path.remove(path.size() - 1);
        }
        onPath.remove(at);
    }

    /** Entrada de la cola de prioridad de Dijkstra: un vértice y el coste con que se llegó a él. */
    private static final class Visit<V, W> implements Comparable<Visit<V, W>> {
        final Node<V, W> node;
        final double cost;

        Visit(Node<V, W> node, double cost) {
            this.node = node;
            this.cost = cost;
        }

        @Override
        public int compareTo(Visit<V, W> o) {
            return Double.compare(cost, o.cost);
        }
    }
}
