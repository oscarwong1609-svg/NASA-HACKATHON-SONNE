package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.missionlab.data.Catalog;
import com.missionlab.ds.DirectedWeightedGraph.Edge;
import com.missionlab.ds.DirectedWeightedGraph.Node;
import com.missionlab.game.Assets;
import com.missionlab.game.Draw;
import com.missionlab.game.Palette;
import com.missionlab.game.Session;
import com.missionlab.model.Orbit;
import com.missionlab.model.Route;
import com.missionlab.model.Transfer;
import com.missionlab.model.Waypoint;
import com.missionlab.sim.Budgets;
import com.missionlab.sim.Readiness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La consola del cohete, el puesto del director de lanzamiento:
 * - el grafo de vuelo dibujado (vértices = sitios, aristas = vehículos con su precio para el peso actual),
 *   con la ruta elegida en verde, las que no pueden con el peso en rojo y la más barata (Dijkstra) marcada;
 * - la lista de rutas a la órbita elegida;
 * - la revisión de vuelo: GO / WARN / NO-GO de cada sistema y el veredicto final.
 */
final class RocketConsoleView extends ConsoleView {
    /** Dónde se dibuja cada vértice del grafo (coordenadas de la capa de texto). */
    private static final Map<String, float[]> NODE_POS = new HashMap<>();

    static {
        NODE_POS.put("launch_site", new float[] {44, 256});
        NODE_POS.put(Catalog.ISS_ORBIT.id, new float[] {140, 306});
        NODE_POS.put(Catalog.SSO_550.id, new float[] {140, 214});
        NODE_POS.put(Catalog.SSO_800.id, new float[] {226, 260});
    }

    RocketConsoleView(Draw draw, Assets assets, Session session) {
        super(draw, assets, session);
    }

    @Override
    String icon() {
        return "ic_launch";
    }

    @Override
    String title() {
        return "ROCKET CONSOLE  -  LAUNCH CONTROL";
    }

    @Override
    String choiceHint() {
        return "change launch route";
    }

    @Override
    void drawBody(float time, Budgets b, Readiness review) {
        drawGraph(time, b);
        drawRoutes(b);
        drawReview(time, review);
    }

    // ------------------------------------------------------------------ el grafo

    private void drawGraph(float time, Budgets b) {
        draw.panel(8, 166, 352, 168, Palette.WALL);
        draw.heading("FLIGHT GRAPH", 16, 330, Palette.ACCENT);
        Orbit target = session.design.orbit;
        Route chosen = session.design.route;
        Route cheapest = target == null ? null : Catalog.FLIGHTS.cheapestRoute(target, b.massKg);

        // Aristas, numeradas. Las que unen los mismos dos vértices se separan un poco.
        Map<String, Integer> seen = new HashMap<>();
        float ly = 312;
        draw.text(assets.small, f("Price for %.1f kg", b.massKg), 246, 330, Palette.DIM);
        int number = 1;
        for (Node<Waypoint, Transfer> n : Catalog.FLIGHTS.graph().nodes()) {
            for (Edge<Waypoint, Transfer> e : n.outgoing()) {
                float[] a = NODE_POS.get(e.from.value.id), c = NODE_POS.get(e.to.value.id);
                int k = seen.merge(e.from.value.id + ">" + e.to.value.id, 1, Integer::sum) - 1;
                float dx = c[0] - a[0], dy = c[1] - a[1], len = (float) Math.sqrt(dx * dx + dy * dy);
                float side = k == 0 ? 0 : (k % 2 == 1 ? -6 : 6);
                float ox = -dy / len * side, oy = dx / len * side;
                if (k == 1) { // la primera arista paralela también se aparta
                    ox = -ox;
                    oy = -oy;
                }
                boolean inChosen = chosen != null && chosen.legs().contains(e);
                boolean inCheapest = cheapest != null && cheapest.legs().contains(e);
                boolean fits = e.weight.carries(b.massKg);
                Color col = inChosen ? Palette.GOOD : !fits ? Palette.BAD : inCheapest ? Palette.ACCENT : Palette.DIM;
                float x1 = a[0] + ox, y1 = a[1] + oy, x2 = c[0] + ox, y2 = c[1] + oy;
                draw.dots(x1, y1, x2, y2, 5, inChosen ? 2.5f : 1.5f, col, inChosen ? 1f : 0.75f);
                if (inChosen) {
                    float p = (time * 0.6f) % 1f;
                    draw.rect(x1 + (x2 - x1) * p - 2, y1 + (y2 - y1) * p - 2, 4, 4, Palette.TEXT);
                }
                // Número en la arista y fila en la leyenda.
                float t = 0.35f + 0.3f * k;
                float bx = x1 + (x2 - x1) * t, by = y1 + (y2 - y1) * t;
                draw.circle(bx, by, 6, col);
                draw.textCenter(assets.small, String.valueOf(number), bx, by + 4, Palette.DARK);
                draw.circle(252, ly - 4, 6, col);
                draw.textCenter(assets.small, String.valueOf(number), 252, ly, Palette.DARK);
                draw.text(assets.small, e.weight.shortName, 262, ly, col);
                draw.text(assets.small, fits ? budget(e.weight.costFor(b.massKg)) : "too heavy", 262, ly - 11, fits ? Palette.TEXT : Palette.BAD);
                ly -= 26;
                number++;
            }
        }
        // Vértices
        for (Node<Waypoint, Transfer> n : Catalog.FLIGHTS.graph().nodes()) {
            float[] p = NODE_POS.get(n.value.id);
            boolean isTarget = target != null && n.value.orbit == target;
            boolean ground = n.value.onGround();
            draw.circle(p[0], p[1], 9, isTarget ? Palette.WARN : ground ? Palette.FIRE : Palette.EARTH);
            draw.circle(p[0], p[1], 6, Palette.PANEL);
            draw.textCenter(assets.small, ground ? "LAUNCH" : n.value.name, p[0], p[1] - 12, isTarget ? Palette.WARN : Palette.TEXT);
        }
        float lx = 16;
        lx += draw.text(assets.small, "green: your route", lx, 182, Palette.GOOD) + 10;
        lx += draw.text(assets.small, "blue: cheapest", lx, 182, Palette.ACCENT) + 10;
        draw.text(assets.small, "red: too heavy", lx, 182, Palette.BAD);
    }

    // ------------------------------------------------------------------ las rutas

    private void drawRoutes(Budgets b) {
        float x = 8, y = 156;
        draw.panel(x, 16, 352, 146, Palette.WALL);
        Orbit target = session.design.orbit;
        if (target == null) {
            draw.heading("ROUTES", x + 8, y - 4, Palette.ACCENT);
            draw.wrap(assets.small, "Pick an orbit on the orbit map first: the rocket console plans the way to get there.", x + 8, y - 20, 330, Palette.TEXT);
            return;
        }
        draw.heading("ROUTES TO THE " + target.name.toUpperCase(java.util.Locale.ROOT), x + 8, y - 4, Palette.ACCENT);
        Route chosen = session.design.route;
        Route cheapest = Catalog.FLIGHTS.cheapestRoute(target, b.massKg);
        List<Route> routes = Catalog.FLIGHTS.routesFor(target, b.massKg);
        float ry = y - 20;
        for (Route r : routes) {
            boolean sel = r.equals(chosen), fits = r.carries(b.massKg);
            if (sel) draw.rect(x + 4, ry - 25, 344, 28, Palette.BLUEPRINT, 0.9f);
            String name = (sel ? "> " : "  ") + r.label();
            draw.text(assets.small, name, x + 8, ry, fits ? Palette.TEXT : Palette.BAD);
            draw.textRight(assets.small, fits ? budget(r.costFor(b.massKg)) : "TOO HEAVY", x + 344, ry, fits ? Palette.TEXT : Palette.BAD);
            if (r.equals(cheapest)) draw.chip(assets.label, "CHEAPEST", x + 14 + draw.width(assets.small, name), ry, Palette.ACCENT);
            draw.text(assets.small, f("    carries up to %.0f kg, hand over %.0f months before launch", r.capacityKg(), r.deliveryMonths()),
                    x + 8, ry - 12, Palette.DIM);
            ry -= 30;
        }
        if (chosen == null) {
            draw.wrap(assets.small, "Press RIGHT: the console suggests the cheapest route first (Dijkstra's algorithm on the flight graph).",
                    x + 8, ry - 4, 336, Palette.WARN);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < chosen.legs().size(); i++) sb.append(i > 0 ? " " : "").append(chosen.vehicle(i).description);
            draw.wrap(assets.small, sb.toString(), x + 8, ry - 4, 336, Palette.TEXT);
        }
    }

    // ------------------------------------------------------------------ revisión de vuelo

    private void drawReview(float time, Readiness review) {
        float x = 366, y = 334;
        draw.panel(x, 16, 266, 318, Palette.WALL);
        draw.heading("FLIGHT READINESS REVIEW", x + 8, y - 4, Palette.ACCENT);
        float labelW = 0;
        for (Readiness.Check c : review.checks) labelW = Math.max(labelW, draw.width(assets.small, c.label));
        float chipX = x + 12 + labelW, detailX = chipX + draw.width(assets.label, "NO-GO") + 14, detailW = x + 258 - detailX;
        float ry = y - 20;
        for (Readiness.Check c : review.checks) {
            draw.text(assets.small, c.label, x + 8, ry, Palette.TEXT);
            draw.chip(assets.label, statusText(c.status), chipX, ry, statusColor(c.status));
            draw.text(assets.small, draw.fit(assets.small, c.detail, detailW), detailX, ry, Palette.DIM);
            ry -= 16;
        }
        boolean go = review.go();
        float pulse = 0.6f + 0.4f * (float) Math.abs(Math.sin(time * 3));
        Color verdict = go ? Palette.GOOD : Palette.BAD;
        draw.rect(x + 6, 22, 250, 76, verdict, 0.15f * pulse);
        draw.frame(x + 6, 22, 250, 76, 1, verdict);
        draw.text(assets.font, go ? "LAUNCH: GO" : "LAUNCH: NO-GO", x + 14, 92, verdict);
        Readiness.Check blocker = review.firstBlocker(), warn = review.firstWarning();
        String why = blocker != null ? blocker.label + ": " + blocker.fix
                : warn != null ? "Allowed, but " + warn.label + " is WARN: " + warn.fix
                : "Every system is GO. Gather the whole team on the launch pad.";
        draw.wrap(assets.small, why, x + 14, 74, 236, Palette.TEXT);
    }
}
