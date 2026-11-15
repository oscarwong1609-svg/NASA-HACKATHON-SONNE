package com.missionlab.ds;

import com.missionlab.ds.DirectedWeightedGraph.Edge;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectedWeightedGraphTest {

    /**
     *   A --4--> B --1--> D
     *   A --1--> C --1--> B
     *   C --5--> D
     */
    private static DirectedWeightedGraph<String, Double> sample() {
        DirectedWeightedGraph<String, Double> g = new DirectedWeightedGraph<>();
        g.addEdge("A", "B", 4.0);
        g.addEdge("A", "C", 1.0);
        g.addEdge("C", "B", 1.0);
        g.addEdge("B", "D", 1.0);
        g.addEdge("C", "D", 5.0);
        return g;
    }

    private static String trace(List<Edge<String, Double>> path) {
        StringBuilder sb = new StringBuilder(path.get(0).from.value);
        for (Edge<String, Double> e : path) sb.append(e.to.value);
        return sb.toString();
    }

    private static double total(List<Edge<String, Double>> path) {
        double t = 0;
        for (Edge<String, Double> e : path) t += e.weight;
        return t;
    }

    @Test
    void buildsAnAdjacencyList() {
        DirectedWeightedGraph<String, Double> g = sample();
        assertEquals(4, g.nodeCount());
        assertEquals(5, g.edgeCount());
        assertEquals(2, g.outgoing("A").size());
        assertEquals(0, g.outgoing("D").size());
    }

    @Test
    void dijkstraFindsTheCheapestPathNotTheShortest() {
        List<Edge<String, Double>> p = sample().cheapestPath("A", "D", w -> w);
        assertEquals("ACBD", trace(p)); // 3 aristas pero cuesta 3; A→B→D cuesta 5
        assertEquals(3.0, total(p), 1e-9);
    }

    @Test
    void edgesOnlyGoOneWay() {
        assertNull(sample().cheapestPath("D", "A", w -> w));
    }

    @Test
    void infiniteCostBlocksAnEdge() {
        // Bloquear las aristas de peso 1 deja solo A→B (4) y C→D (5): de A a D no hay camino.
        assertNull(sample().cheapestPath("A", "D", w -> w == 1.0 ? Double.POSITIVE_INFINITY : w));
        List<Edge<String, Double>> p = sample().cheapestPath("A", "D", w -> w == 4.0 ? Double.POSITIVE_INFINITY : w);
        assertEquals("ACBD", trace(p));
    }

    @Test
    void sameStartAndEndIsAnEmptyPath() {
        assertTrue(sample().cheapestPath("B", "B", w -> w).isEmpty());
    }

    @Test
    void negativeCostsAreRejected() {
        assertThrows(IllegalStateException.class, () -> sample().cheapestPath("A", "D", w -> -w));
    }

    @Test
    void unknownNodesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> sample().cheapestPath("A", "Z", w -> w));
    }

    @Test
    void listsEverySimplePath() {
        DirectedWeightedGraph<String, Double> g = sample();
        g.addEdge("B", "A", 1.0); // un ciclo A→B→A no debe repetirse para siempre
        List<List<Edge<String, Double>>> paths = g.allSimplePaths("A", "D");
        assertEquals(3, paths.size()); // ABD, ACBD, ACD
    }
}
