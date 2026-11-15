package com.missionlab.screens;

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
import com.missionlab.sim.MissionOps;
import com.missionlab.sim.OrbitMechanics;
import com.missionlab.sim.OrbitMechanics.State;
import com.missionlab.sim.OrbitMechanics.Track;
import com.missionlab.sim.Readiness;

/**
 * El mapa de órbitas: la Tierra de verdad con la zona del encargo, la noche, la traza de la órbita elegida,
 * el satélite moviéndose y lo que oyen las estaciones de tierra. A la derecha, la telemetría de la órbita.
 */
final class OrbitMapView extends ConsoleView {
    /** Segundos de misión que pasan por cada segundo de pantalla (una vuelta en ~10 s). */
    private static final double SPEED = 570;

    OrbitMapView(Draw draw, Assets assets, Session session) {
        super(draw, assets, session);
    }

    @Override
    String title() {
        return "ORBIT MAP";
    }

    @Override
    String choiceHint() {
        return "change orbit";
    }

    @Override
    void drawBody(float time, Budgets b, Readiness review) {
        Mission m = session.mission;
        Orbit o = session.design.orbit;
        WorldMap map = assets.worldMap;
        map.bounds(12, 104, 416, 208);
        double t = time * SPEED;
        map.drawBase(draw);
        map.drawNight(draw, t, m.sunDeclinationDeg, 0.45f);
        map.drawRegion(draw, m, m.night ? Palette.FIRE : Palette.WARN, (float) Math.abs(Math.sin(time * 3)));

        if (o == null) {
            map.frame(draw, Palette.WALL);
            draw.textCenter(assets.font, "Press LEFT or RIGHT to choose an orbit", 220, 214, Palette.TEXT);
        } else {
            Track track = new Track(o, m.sunDeclinationDeg, 0);
            double period = track.periodSeconds();
            // Un día entero, tenue: así se ve qué partes del planeta cubre la órbita.
            map.drawTrack(draw, track, t - 86400, t, 90, Palette.ACCENT, 0.18f, 1);
            // La vuelta y media siguientes, más marcadas.
            map.drawTrack(draw, track, t, t + period * 1.5, 30, Palette.ACCENT, 0.8f, 1.5f);
            GroundStation gs = session.design.station;
            if (gs != null) {
                double radius = OrbitMechanics.coverageRadiusDeg(o);
                for (GroundStation.Site s : gs.sites) {
                    map.drawCircle(draw, s.lat, s.lon, radius, Palette.GOOD, 0.6f);
                    map.drawSite(draw, s, false);
                }
            }
            State now = track.at(t);
            boolean over = m.inRegion(now.lat, now.lon);
            map.drawSatellite(draw, now.lat, now.lon, now.sunlit ? Palette.TEXT : Palette.DIM, time);
            map.frame(draw, Palette.WALL);
            draw.text(assets.small, f("Satellite: %.0f%s %.0f%s  |  %s%s", Math.abs(now.lat), now.lat >= 0 ? "N" : "S",
                    Math.abs(now.lon), now.lon >= 0 ? "E" : "W", now.sunlit ? "in sunlight" : "in Earth's shadow",
                    over ? "  |  OVER " + m.targetName.toUpperCase(java.util.Locale.ROOT) : ""), 12, 98, over ? Palette.WARN : Palette.TEXT);
        }
        draw.text(assets.small, "Shaded = night now.  Box = " + m.targetName + ".  Dots = where the satellite flies.", 12, 324, Palette.DIM);

        drawChoices(m, o);
        if (o != null) drawTelemetry(m, o, b);
        drawExplanation(m, o);
    }

    private void drawChoices(Mission m, Orbit current) {
        float x = 440, y = 326;
        draw.text(assets.small, "CHOOSE AN ORBIT", x, y, Palette.ACCENT);
        y -= 8;
        for (Orbit o : Catalog.ORBITS) {
            boolean sel = o == current, over = o.passesOver(m.targetLatitude);
            draw.rect(x, y - 30, 188, 28, sel ? Palette.BLUEPRINT : Palette.PANEL, 0.95f);
            if (sel) draw.frame(x, y - 30, 188, 28, 1, Palette.ACCENT);
            draw.text(assets.small, draw.fit(assets.small, o.name, 178), x + 5, y - 5, sel ? Palette.TEXT : Palette.DIM);
            draw.chip(assets.small, over ? "OVER TARGET" : "MISSES TARGET", x + 5, y - 17, over ? Palette.GOOD : Palette.BAD);
            draw.textRight(assets.small, f("%.0f deg", o.inclinationDeg), x + 183, y - 17, Palette.DIM);
            y -= 32;
        }
    }

    private void drawTelemetry(Mission m, Orbit o, Budgets b) {
        float x = 440, y = 214;
        draw.text(assets.small, "TELEMETRY", x, y, Palette.ACCENT);
        y -= 13;
        double period = OrbitMechanics.periodMinutes(o);
        double eclipse = OrbitMechanics.eclipseMinutes(o, m.sunDeclinationDeg);
        int passes = MissionOps.usefulPassesPerDay(o, m);
        y = row("Altitude", f("%.0f km", o.altitudeKm), x, y, Palette.TEXT);
        y = row("Tilt", f("%.1f deg%s", o.inclinationDeg, o.sunSynchronous ? ", sun-sync" : ""), x, y, Palette.TEXT);
        y = row("One orbit", f("%.0f min at %.1f km/s", period, OrbitMechanics.speedKmS(o)), x, y, Palette.TEXT);
        y = row("Per day", f("%.1f orbits", OrbitMechanics.orbitsPerDay(o)), x, y, Palette.TEXT);
        y = row("Shadow", f("%.0f min per orbit", eclipse), x, y, Palette.TEXT);
        y = row("Reaches", f("%.0f deg N and S", o.maxLatitude), x, y, o.passesOver(m.targetLatitude) ? Palette.GOOD : Palette.BAD);
        y = row("Passes", passes + " a day " + (m.night ? "at night" : "in daylight"), x, y, passes > 0 ? Palette.GOOD : Palette.BAD);
        boolean lasts = o.lifetimeYears >= m.missionYears;
        y = row("Lifetime", o.lifetimeYears > 50 ? "decades: drag sail" : f("about %.1f years", o.lifetimeYears), x, y,
                lasts ? Palette.TEXT : Palette.BAD);
        Part cam = session.design.get(Slot.INSTRUMENT);
        if (cam == null) {
            row("Detail", "add a camera to see", x, y, Palette.DIM);
        } else {
            double gsd = cam.groundResolutionM(o.altitudeKm);
            row("Detail", f("%.0f m pixels (need %.0f)", gsd, m.requiredGsdM), x, y, gsd <= m.requiredGsdM ? Palette.GOOD : Palette.WARN);
        }
    }

    private void drawExplanation(Mission m, Orbit o) {
        if (o == null) {
            draw.wrap(assets.small, "Your orbit decides what the satellite can see, how sharp its photos are, how long it lasts "
                    + "and how often it talks to the ground. " + m.targetName + " is at " + f("%.0f", Math.abs(m.targetLatitude))
                    + " degrees " + (m.targetLatitude >= 0 ? "north" : "south") + ".", 12, 84, 416, Palette.TEXT);
            return;
        }
        float h = draw.wrap(assets.small, o.description, 12, 84, 416, Palette.TEXT);
        String lesson = !o.passesOver(m.targetLatitude)
                ? "This orbit never reaches " + m.targetName + ". Lower inclination orbits stay near the equator."
                : o.sunSynchronous
                ? "Sun-synchronous orbits cross each place at the same local time every day, so photos can be compared day after day."
                : "Low orbits give sharp photos, but the thin air up there slowly pulls the satellite down.";
        draw.wrap(assets.small, lesson, 12, 84 - h - 6, 416, o.passesOver(m.targetLatitude) ? Palette.ACCENT : Palette.WARN);
    }
}
