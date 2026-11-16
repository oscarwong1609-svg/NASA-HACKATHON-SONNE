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
 * NON-LINEAR STRUCTURE: directed weighted graph, stored as an adjacency list.
 *
 * - Each {@link Node} holds a value (V) and the list of edges LEAVING it.
 * - Each {@link Edge} goes one way only (from → to) and carries a weight (W).
 *
 * The weight is generic so an edge can carry several resources at once
 * (in Mission Lab: fixed price, price per kilogram, maximum mass and travel time).
 * When searching for a path, a function converts each weight into a number;
 * if it returns infinity, that edge cannot be used in that search.
 *
 * It does not depend on libGDX or the game, so it is tested with plain unit tests.
 *
 * @param <V> value of each vertex (must have consistent equals/hashCode)
 * @param <W> weight of each edge
 */
public final class DirectedWeightedGraph<V, W> {

    /** A vertex: its value and the edges leaving it (adjacency list). */
    public static final class Node<V, W> {
        public final V value;
        private final List<Edge<V, W>> outgoing = new ArrayList<>();

        private Node(V value) {
            this.value = value;
        }

        /** Edges leaving this vertex, in insertion order. */
        public List<Edge<V, W>> outgoing() {
            return Collections.unmodifiableList(outgoing);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /** A directed edge from → to with its weight. Two distinct edges are never equal (identity). */
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

    /** Index value → vertex. LinkedHashMap so vertices are always iterated in the same order. */
    private final Map<V, Node<V, W>> nodes = new LinkedHashMap<>();
    private int edgeCount;

    // ------------------------------------------------------------------ build

    /** Adds a vertex (if it already exists, returns the existing one). */
    public Node<V, W> addNode(V value) {
        if (value == null) throw new IllegalArgumentException("A node needs a value");
        return nodes.computeIfAbsent(value, Node::new);
    }

    /** Adds a directed edge. Missing vertices are created. */
    public Edge<V, W> addEdge(V from, V to, W weight) {
        if (weight == null) throw new IllegalArgumentException("An edge needs a weight");
        Node<V, W> a = addNode(from);
        Node<V, W> b = addNode(to);
        Edge<V, W> e = new Edge<>(a, b, weight);
        a.outgoing.add(e);
        edgeCount++;
        return e;
    }

    // ------------------------------------------------------------------ query

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

    /** All vertices, always in the same order (insertion order). */
    public java.util.Collection<Node<V, W>> nodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edgeCount;
    }

    // ------------------------------------------------------------------ algorithms

    /**
     * Cheapest path from source to target (Dijkstra's algorithm with a priority queue).
     *
     * @param cost converts an edge weight into a cost ≥ 0.
     *             Double.POSITIVE_INFINITY = this edge cannot be used right now.
     * @return the path's edges in order, an empty list if source == target,
     *         or null if target is unreachable.
     */
    public List<Edge<V, W>> cheapestPath(V source, V target, ToDoubleFunction<W> cost) {
        Node<V, W> start = node(source);
        Node<V, W> goal = node(target);

        Map<Node<V, W>, Double> best = new HashMap<>();          // lowest known cost to each vertex
        Map<Node<V, W>, Edge<V, W>> arrivedBy = new HashMap<>(); // last edge of the best known path
        Set<Node<V, W>> settled = new HashSet<>();               // vertices whose cost is already final
        PriorityQueue<Visit<V, W>> frontier = new PriorityQueue<>();

        best.put(start, 0.0);
        frontier.add(new Visit<>(start, 0.0));
        while (!frontier.isEmpty()) {
            Visit<V, W> v = frontier.poll();
            if (!settled.add(v.node)) continue; // stale entry: something cheaper was already found
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

        // Rebuild the path by walking backwards from the target.
        List<Edge<V, W>> path = new ArrayList<>();
        for (Node<V, W> at = goal; at != start; at = arrivedBy.get(at).from) path.add(arrivedBy.get(at));
        Collections.reverse(path);
        return path;
    }

    /**
     * All simple paths (no repeated vertices) from source to target, by depth-first search.
     * Used to show the player ALL possible routes, not just the cheapest one.
     * Note: large graphs can have a huge number of paths; Mission Lab's graph is small.
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
            if (onPath.contains(e.to)) continue; // avoids cycles
            path.add(e);
            dfs(e.to, goal, onPath, path, out);
            path.remove(path.size() - 1);
        }
        onPath.remove(at);
    }

    /** Dijkstra priority-queue entry: a vertex and the cost at which it was reached. */
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
