package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.missionlab.data.Catalog;
import com.missionlab.game.Assets;
import com.missionlab.game.Draw;
import com.missionlab.game.Palette;
import com.missionlab.game.Session;
import com.missionlab.game.WorldMap;
import com.missionlab.model.GroundStation;
import com.missionlab.model.Mission;
import com.missionlab.model.Orbit;
import com.missionlab.model.Part;
import com.missionlab.model.Slot;
import com.missionlab.sim.Budgets;
import com.missionlab.sim.OrbitMechanics;
import com.missionlab.sim.OrbitMechanics.State;
import com.missionlab.sim.OrbitMechanics.Track;
import com.missionlab.sim.PowerProfile;
import com.missionlab.sim.Readiness;

/**
 * La mesa de estaciones de tierra, el puesto de Ground control:
 * - el mapa con las antenas, lo que oye cada una y los tramos del día en que hay contacto (en verde);
 * - las redes de estaciones con su precio y sus minutos de contacto con la órbita elegida;
 * - el presupuesto de datos: lo que hace la cámara frente a lo que baja el enlace;
 * - la batería a lo largo de dos vueltas: carga al sol, descarga en la sombra, y el límite seguro.
 */
final class GroundDeskView extends ConsoleView {

    GroundDeskView(Draw draw, Assets assets, Session session) {
        super(draw, assets, session);
    }

    @Override
    String icon() {
        return "ic_dish";
    }

    @Override
    String title() {
        return "GROUND STATION DESK";
    }

    @Override
    String choiceHint() {
        return "change ground network";
    }

    @Override
    void drawBody(float time, Budgets b, Readiness review) {
        Mission m = session.mission;
        Orbit o = b.orbitUsed;
        drawMap(time, m, o);
        drawNetworks(o);
        drawDataBudget(b);
        drawPower(b);
    }

    private void drawMap(float time, Mission m, Orbit o) {
        WorldMap map = assets.worldMap;
        map.bounds(12, 172, 320, 160);
        map.drawBase(draw);
        map.drawRegion(draw, m, m.night ? Palette.FIRE : Palette.WARN, 0.3f);
        GroundStation gs = session.design.station;
        double radius = OrbitMechanics.coverageRadiusDeg(o);
        Track track = new Track(o, m.sunDeclinationDeg, 0);
        // Traza de un día: verde donde alguna antena oye al satélite.
        for (double t = 0; t < 86400; t += 60) {
            State s = track.at(t);
            boolean heard = gs != null && OrbitMechanics.siteInView(gs, s.lat, s.lon, radius) != null;
            float sx = map.px(s.lon), sy = map.py(s.lat);
            draw.rect(sx - 0.5f, sy - 0.5f, heard ? 2 : 1, heard ? 2 : 1, heard ? Palette.GOOD : Palette.ACCENT, heard ? 0.95f : 0.22f);
        }
        if (gs != null) {
            for (GroundStation.Site s : gs.sites) {
                map.drawCircle(draw, s.lat, s.lon, radius, Palette.GOOD, 0.7f);
                map.drawSite(draw, s, true);
                draw.text(assets.small, s.name, map.px(s.lon) + 5, map.py(s.lat) + 4, Palette.TEXT);
            }
        }
        double t = time * 570;
        State now = track.at(t);
        map.drawSatellite(draw, now.lat, now.lon, Palette.TEXT, time);
        map.frame(draw, Palette.WALL);
        String orbitNote = session.design.orbit == null ? " (no orbit yet: 550 km shown)" : "";
        draw.text(assets.small, draw.fit(assets.small, "One day of passes. Green = an antenna hears it." + orbitNote, 340), 12, 166, Palette.DIM);
    }

    private void drawNetworks(Orbit o) {
        float x = 364, y = 330;
        draw.heading("CHOOSE A GROUND NETWORK", x, y, Palette.ACCENT);
        y -= 8;
        for (GroundStation g : Catalog.STATIONS) {
            boolean sel = g == session.design.station;
            double min = OrbitMechanics.contactMinutesPerDay(o, g);
            int passes = OrbitMechanics.contactPassesPerDay(o, g);
            draw.rect(x, y - 30, 264, 28, sel ? Palette.BLUEPRINT : Palette.PANEL, 0.95f);
            if (sel) draw.frame(x, y - 30, 264, 28, 1, Palette.ACCENT);
            draw.text(assets.small, g.name + f("  (%d antenna%s)", g.sites.size(), g.sites.size() == 1 ? "" : "s"), x + 5, y - 5, sel ? Palette.TEXT : Palette.DIM);
            draw.textRight(assets.small, budget(g.costKPerYear) + " a year", x + 259, y - 5, Palette.DIM);
            draw.text(assets.small, f("%d passes, %.0f min of contact a day", passes, min), x + 5, y - 18, min > 0 ? Palette.GOOD : Palette.BAD);
            y -= 32;
        }
        GroundStation sel = session.design.station;
        if (sel != null) draw.wrap(assets.small, sel.description, x, y - 2, 264, Palette.TEXT);
    }

    private void drawDataBudget(Budgets b) {
        float x = 364, y = 190;
        draw.panel(x - 4, 16, 272, 182, Palette.WALL);
        draw.heading("DATA BUDGET (each day)", x, y, Palette.ACCENT);
        Part cam = session.design.get(Slot.INSTRUMENT), radio = session.design.get(Slot.RADIO);
        if (cam == null || radio == null || session.design.station == null) {
            draw.wrap(assets.small, "Install a camera and a radio and pick a network to see whether the picture can reach Earth.", x, y - 16, 260, Palette.TEXT);
            return;
        }
        double max = Math.max(b.dataGbPerDay, b.downlinkGbPerDay) * 1.1;
        bar("Camera makes", b.dataGbPerDay, max, x, y - 18, Palette.WARN);
        bar("Link sends", b.downlinkGbPerDay, max, x, y - 34, b.downlinkGbPerDay >= b.dataGbPerDay ? Palette.GOOD : Palette.BAD);
        draw.text(assets.small, f("%.0f Mbps x %.0f min x 60 / 8000 = %.1f GB", radio.downlinkMbps, b.contactMinPerDay, b.downlinkGbPerDay), x, y - 52, Palette.DIM);
        String verdict = b.downlinkGbPerDay >= b.dataGbPerDay
                ? f("The whole picture gets home, with %.0f%% to spare.", 100 * b.dataMargin())
                : f("Only %.0f%% of the picture can reach Earth. Faster radio or more antennas.", 100 * b.downlinkShare());
        float h = draw.wrap(assets.small, verdict, x, y - 66, 260, b.downlinkGbPerDay >= b.dataGbPerDay ? Palette.GOOD : Palette.WARN);
        draw.wrap(assets.small, "In Mission Control you send DOWNLINK for each pass: "
                + OrbitMechanics.contactPassesPerDay(b.orbitUsed, session.design.station) + " passes a day with this network.",
                x, y - 74 - h, 260, Palette.TEXT);
    }

    private void bar(String label, double value, double max, float x, float y, Color c) {
        draw.text(assets.small, label, x, y, Palette.DIM);
        float w = 110, bx = x + 86;
        draw.rect(bx, y - 8, w, 8, Palette.DARK);
        draw.rect(bx, y - 8, (float) (w * Math.min(1, value / max)), 8, c);
        draw.text(assets.small, f("%.1f GB", value), bx + w + 6, y, Palette.TEXT);
    }

    private void drawPower(Budgets b) {
        float x = 12;
        draw.panel(8, 16, 344, 140, Palette.WALL);
        draw.heading("BATTERY THROUGH TWO ORBITS", x + 4, 152, Palette.ACCENT);
        PowerProfile p = PowerProfile.of(session.design, session.mission, 2);
        if (p == null) {
            draw.wrap(assets.small, "Install solar panels and a battery to see how the battery charges in sunlight and drains in Earth's shadow.",
                    x + 4, 136, 330, Palette.TEXT);
            return;
        }
        String verdict = p.healthy()
                ? f("Stays above its safe limit (lowest %.0f%%). Uses %.1f W, panels give %.1f W.", p.minimum * 100, b.drawW, b.generatedW)
                : f("Falls below the safe limit (to %.0f%%): safe mode at night. Bigger battery or panels.", p.minimum * 100);
        draw.wrap(assets.small, verdict, x + 4, 138, 330, p.healthy() ? Palette.GOOD : Palette.BAD);
        float cx = x + 4, cy = 40, cw = 290, ch = 64;
        draw.rect(cx, cy, cw, ch, Palette.DARK);
        int n = p.charge.length;
        for (int i = 0; i < n; i++) {
            float px = cx + cw * i / n;
            // Banda inferior: amarillo = sol, oscuro = sombra; naranja = cámara trabajando.
            draw.rect(px, cy - 6, cw / n, 4, p.sunlit[i] ? Palette.WARN : Palette.WALL);
            if (p.imaging[i]) draw.rect(px, cy - 10, cw / n, 3, Palette.FIRE);
            float py = cy + (float) (ch * p.charge[i]);
            draw.rect(px, py - 1, Math.max(1, cw / n), 2, p.charge[i] < p.floor ? Palette.BAD : Palette.GOOD);
        }
        float floorY = cy + (float) (ch * p.floor);
        draw.dots(cx, floorY, cx + cw, floorY, 4, 1, Palette.BAD, 0.8f);
        draw.text(assets.small, "full", cx + cw + 4, cy + ch + 3, Palette.DIM);
        draw.text(assets.small, "safe", cx + cw + 4, floorY + 4, Palette.BAD);
        draw.text(assets.small, "sun", cx, cy - 13, Palette.WARN);
        draw.text(assets.small, "shadow", cx + 30, cy - 13, Palette.DIM);
        draw.text(assets.small, "camera on", cx + 82, cy - 13, Palette.FIRE);
    }
}
