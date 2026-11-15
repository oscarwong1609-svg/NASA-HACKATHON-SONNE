package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.missionlab.MissionLabGame;
import com.missionlab.ds.LinkedQueue;
import com.missionlab.game.Controls;
import com.missionlab.game.Palette;
import com.missionlab.game.WorldMap;
import com.missionlab.model.GroundStation;
import com.missionlab.model.Mission;
import com.missionlab.model.Role;
import com.missionlab.sim.FlightEvent;
import com.missionlab.sim.Finding;
import com.missionlab.sim.MissionOps;
import com.missionlab.sim.MissionOps.Command;
import com.missionlab.sim.MissionOps.Window;
import com.missionlab.sim.OrbitMechanics;
import com.missionlab.sim.OrbitMechanics.State;

import java.util.List;
import java.util.Locale;

/**
 * Mission Control: el equipo opera el satélite durante un día en órbita.
 * IZQUIERDA manda IMAGE (Scientist), DERECHA manda DOWNLINK (Ground control); mantener ACCIÓN acelera el tiempo.
 * Las órdenes esperan en la cola de a bordo hasta que se dan sus condiciones.
 * El tiempo va rápido entre ventanas y se frena solo en las pasadas sobre el objetivo y en los contactos.
 */
public class OperationsScreen extends BaseScreen {
    /** Segundos de misión por segundo de pantalla. */
    private static final double WARP_CRUISE = 900, WARP_TARGET = 45, WARP_CONTACT = 120, FAST_FORWARD = 4;

    private final MissionOps ops;
    private final double powerFactor;
    private final Mission mission;
    private double warp = WARP_CRUISE;
    private boolean finishedSoundPlayed;

    public OperationsScreen(MissionLabGame game) {
        super(game);
        mission = session.mission;
        // Si un panel se atascó al desplegarse (lo cuenta la línea de tiempo del lanzamiento), hay media energía.
        double pf = 1;
        LinkedQueue<FlightEvent> t = session.result.timeline();
        while (!t.isEmpty()) {
            FlightEvent e = t.dequeue();
            if (e.kind == FlightEvent.Kind.PANELS && e.status == FlightEvent.Status.WARN) pf = 0.5;
        }
        powerFactor = pf;
        ops = new MissionOps(session.design, mission, powerFactor);
    }

    // ------------------------------------------------------------------ lógica

    @Override
    protected void update(float delta) {
        if (ops.finished()) {
            if (!finishedSoundPlayed) {
                finishedSoundPlayed = true;
                assets.play(ops.received() >= 0.9 ? "success" : "alert");
            }
            if (Controls.confirm()) finish();
            return;
        }
        if (Controls.back()) {
            ops.endNow();
            return;
        }
        boolean fast = false;
        for (int i = 0; i < session.playerCount; i++) {
            com.missionlab.game.Controls.KeySet k = Controls.PLAYERS[i];
            int lr = k.leftRightPressed();
            if (lr < 0) command(i, Command.IMAGE, Role.SCIENTIST);
            if (lr > 0) command(i, Command.DOWNLINK, Role.GROUND);
            fast |= k.actionHeld();
        }
        warp = chooseWarp() * (fast ? FAST_FORWARD : 1);
        int before = ops.log().size();
        ops.step(delta * warp);
        if (ops.log().size() > before) {
            String last = ops.log().get(ops.log().size() - 1);
            assets.play(last.contains("SAFE MODE") ? "deny" : last.contains("begins") || last.contains("acquired") ? "alert" : "click");
        }
    }

    /** El tiempo se frena cuando pasa algo en lo que el equipo puede actuar. */
    private double chooseWarp() {
        if (ops.currentTarget() != null && ops.coverage() < 0.999) return WARP_TARGET;
        boolean wantsDownlink = ops.storedGb() > 0 || (!ops.queue().isEmpty() && ops.queue().peek() == Command.DOWNLINK);
        if (ops.currentContact() != null && wantsDownlink) return WARP_CONTACT;
        return WARP_CRUISE;
    }

    private void command(int playerIndex, Command c, Role owner) {
        if (!session.canUse(playerIndex, owner)) {
            assets.play("deny");
            return;
        }
        assets.play(ops.send(c) ? "place" : "deny");
    }

    private void finish() {
        double best = MissionOps.autopilotReceived(session.design, mission, powerFactor);
        double share = ops.shareOf(best);
        Finding note;
        if (best <= 1e-6) {
            note = Finding.problem("Mission Control could not get any picture: the satellite never had a useful pass over "
                    + mission.targetName + " with the right light.", "Check the orbit map: does your orbit fly over the target?", 0);
        } else if (share >= 0.999) {
            note = Finding.good(String.format(Locale.ROOT, "Mission Control was perfect: %d%% of the picture received, as much as this satellite allows.",
                    Math.round(ops.received() * 100)));
        } else {
            note = Finding.problem(String.format(Locale.ROOT, "Mission Control brought down %d%% of the picture; this satellite could have delivered %d%%.",
                            Math.round(ops.received() * 100), Math.round(Math.min(1, best) * 100)),
                    "Queue IMAGE before the target pass, and a DOWNLINK for every ground station contact.",
                    20 * (1 - share));
        }
        session.result = session.result.withOperations(share, note);
        game.setScreen(new ResultScreen(game));
    }

    // ------------------------------------------------------------------ mundo

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
    }

    // ------------------------------------------------------------------ texto

    @Override
    protected void drawUi() {
        phaseBar(Phase.OPERATIONS, 16, 352);
        double tt = ops.trackTime();
        draw.text(assets.small, "MISSION CONTROL", 10, 334, Palette.ACCENT);
        int h = (int) (ops.elapsedS() / 3600), mi = (int) (ops.elapsedS() % 3600 / 60);
        draw.textRight(assets.small, String.format(Locale.ROOT, "%02d:%02d of 30:00   time x%.0f", h, mi, warp), 426, 334,
                warp < WARP_CRUISE ? Palette.WARN : Palette.DIM);

        drawMap(tt);
        drawBanner();
        drawLog();
        drawSidebar();

        draw.rect(0, 0, 640, 12, Palette.PANEL);
        String who = session.playerCount > 1 ? " (Scientist)" : "";
        String who2 = session.playerCount > 1 ? " (Ground control)" : "";
        draw.text(assets.small, draw.fit(assets.small, "LEFT: IMAGE" + who + "   RIGHT: DOWNLINK" + who2 + "   hold ACTION: fast-forward   Esc: end", 620),
                10, 10, Palette.TEXT);
        if (ops.finished()) drawFinished();
    }

    private void drawMap(double tt) {
        WorldMap map = assets.worldMap;
        map.bounds(10, 116, 416, 208);
        map.drawBase(draw);
        map.drawNight(draw, tt, mission.sunDeclinationDeg, 0.5f);
        boolean overTarget = ops.currentTarget() != null;
        map.drawRegion(draw, mission, mission.night ? Palette.FIRE : Palette.WARN,
                overTarget ? 1f : (float) Math.abs(Math.sin(time * 2)) * 0.6f);
        double period = ops.track().periodSeconds();
        map.drawTrack(draw, ops.track(), tt - period, tt, 40, Palette.ACCENT, 0.3f, 1);
        map.drawTrack(draw, ops.track(), tt, tt + period, 40, Palette.ACCENT, 0.85f, 1.5f);
        double radius = ops.coverageRadiusDeg();
        Window contact = ops.currentContact();
        for (GroundStation.Site s : ops.station().sites) {
            boolean active = contact != null && contact.site.equals(s.name);
            map.drawCircle(draw, s.lat, s.lon, radius, active ? Palette.GOOD : Palette.DIM, active ? 0.9f : 0.5f);
            map.drawSite(draw, s, active);
        }
        State sat = ops.satellite();
        Color c = ops.safeMode() ? Palette.BAD : ops.imaging() ? Palette.WARN : ops.downlinking() ? Palette.GOOD : Palette.TEXT;
        map.drawSatellite(draw, sat.lat, sat.lon, c, time);
        if (ops.downlinking() && contact != null) {
            for (GroundStation.Site s : ops.station().sites) {
                if (!s.name.equals(contact.site)) continue;
                float k = (time * 2) % 1f;
                float x1 = map.px(sat.lon), y1 = map.py(sat.lat), x2 = map.px(s.lon), y2 = map.py(s.lat);
                if (Math.abs(x2 - x1) < 200) draw.rect(x1 + (x2 - x1) * k - 1, y1 + (y2 - y1) * k - 1, 3, 3, Palette.GOOD);
            }
        }
        map.frame(draw, Palette.WALL);
    }

    private void drawBanner() {
        String text = null;
        Color c = Palette.WARN;
        if (ops.safeMode()) {
            text = "SAFE MODE: battery low, waiting for sunlight";
            c = Palette.BAD;
        } else if (ops.currentTarget() != null && ops.coverage() < 0.999) {
            text = ops.imaging() ? "IMAGING " + mission.targetName.toUpperCase(Locale.ROOT) + "..." : "PASS OVER " + mission.targetName.toUpperCase(Locale.ROOT) + " NOW: queue IMAGE!";
        } else if (ops.currentContact() != null) {
            text = ops.downlinking() ? "DOWNLINKING TO " + ops.currentContact().site.toUpperCase(Locale.ROOT)
                    : "IN CONTACT WITH " + ops.currentContact().site.toUpperCase(Locale.ROOT) + (ops.storedGb() > 0 ? ": queue DOWNLINK!" : "");
            c = Palette.GOOD;
        }
        if (text == null) return;
        float pulse = 0.6f + 0.4f * (float) Math.abs(Math.sin(time * 5));
        draw.rect(10, 116, 416, 14, c, 0.35f * pulse);
        draw.textCenter(assets.small, text, 218, 127, c);
    }

    private void drawLog() {
        draw.panel(10, 16, 416, 92, Palette.WALL);
        draw.text(assets.small, "MISSION LOG", 16, 104, Palette.ACCENT);
        List<String> log = ops.log();
        float y = 90;
        for (int i = Math.max(0, log.size() - 6); i < log.size(); i++) {
            String line = log.get(i);
            Color c = line.contains("SAFE") ? Palette.BAD : line.contains("complete") || line.contains("covered") ? Palette.GOOD : Palette.TEXT;
            draw.text(assets.small, draw.fit(assets.small, line, 402), 16, y, i == log.size() - 1 ? c : Palette.DIM);
            y -= 12;
        }
    }

    private void drawSidebar() {
        float x = 440, w = 190;
        draw.panel(x - 4, 16, w + 8, 318, Palette.WALL);
        float y = 328;
        draw.text(assets.small, "OBJECTIVES", x, y, Palette.ACCENT);
        y = objective(mission.night ? "1  Image the target at night" : "1  Image the target area", ops.coverage(), x, y - 14);
        y = objective("2  Bring the picture home", ops.received(), x, y - 4);

        State sat = ops.satellite();
        y -= 8;
        draw.text(assets.small, "TELEMETRY", x, y, Palette.ACCENT);
        y -= 13;
        draw.text(assets.small, String.format(Locale.ROOT, "%.0f%s %.0f%s   %.0f km   %.1f km/s", Math.abs(sat.lat), sat.lat >= 0 ? "N" : "S",
                Math.abs(sat.lon), sat.lon >= 0 ? "E" : "W", session.design.orbit.altitudeKm, OrbitMechanics.speedKmS(session.design.orbit)), x, y, Palette.TEXT);
        y -= 12;
        draw.text(assets.small, sat.sunlit ? "In sunlight: panels charging" : "In Earth's shadow: on battery", x, y, sat.sunlit ? Palette.WARN : Palette.DIM);
        y -= 12;
        draw.text(assets.small, String.format(Locale.ROOT, "Power in %.1f W, out %.1f W", ops.generationW(), ops.loadW()), x, y,
                ops.generationW() >= ops.loadW() ? Palette.GOOD : Palette.WARN);

        y -= 18;
        draw.text(assets.small, "BATTERY", x, y, Palette.ACCENT);
        float by = y - 18, bw = w - 40;
        draw.rect(x, by, bw, 8, Palette.DARK);
        double soc = ops.batteryFraction();
        draw.rect(x, by, (float) (bw * soc), 8, soc < ops.batteryFloor() + 0.03 ? Palette.BAD : Palette.GOOD);
        float fx = x + (float) (bw * ops.batteryFloor());
        draw.rect(fx, by - 2, 1, 12, Palette.BAD);
        draw.text(assets.small, Math.round(soc * 100) + "%", x + bw + 6, by + 8, Palette.TEXT);
        draw.text(assets.small, "red line = safe limit", x, by - 5, Palette.DIM);

        y = by - 20;
        draw.text(assets.small, "DATA", x, y, Palette.ACCENT);
        y -= 13;
        draw.text(assets.small, String.format(Locale.ROOT, "On board: %.2f GB", ops.storedGb()), x, y, Palette.TEXT);
        y -= 12;
        draw.text(assets.small, String.format(Locale.ROOT, "On Earth: %.2f of %.1f GB", ops.receivedGb(), ops.imageGb()), x, y, Palette.TEXT);

        y -= 16;
        draw.text(assets.small, "UPLINK QUEUE (in order)", x, y, Palette.ACCENT);
        y -= 13;
        LinkedQueue<Command> q = ops.queue();
        if (q.isEmpty()) {
            draw.text(assets.small, "Empty. LEFT / RIGHT to add.", x, y, Palette.DIM);
            y -= 12;
        } else {
            int i = 0;
            for (Command c : q) {
                String waiting = i > 0 ? "next in line" : c == Command.IMAGE ? "at target pass" : "at next station";
                float cw = draw.chip(assets.small, c.name(), x, y, c == Command.IMAGE ? Palette.WARN : Palette.GOOD);
                draw.text(assets.small, waiting, x + cw + 6, y, Palette.DIM);
                y -= 14;
                i++;
            }
        }

        y -= 6;
        draw.text(assets.small, "COMING UP", x, y, Palette.ACCENT);
        y -= 13;
        Window nt = ops.next(Window.Type.TARGET), nc = ops.next(Window.Type.CONTACT);
        draw.text(assets.small, nt == null ? "No more target passes" : "Target " + eta(nt), x, y, nt == null ? Palette.DIM : Palette.WARN);
        y -= 12;
        draw.text(assets.small, draw.fit(assets.small, nc == null ? "No more ground contacts" : nc.site + " " + eta(nc), w), x, y, nc == null ? Palette.DIM : Palette.GOOD);
    }

    private float objective(String label, double done, float x, float y) {
        boolean ok = done >= 0.999;
        draw.text(assets.small, label, x, y, ok ? Palette.GOOD : Palette.TEXT);
        float bw = 150;
        draw.rect(x, y - 20, bw, 6, Palette.DARK);
        draw.rect(x, y - 20, (float) (bw * done), 6, ok ? Palette.GOOD : Palette.WARN);
        draw.text(assets.small, Math.round(done * 100) + "%", x + bw + 6, y - 12, ok ? Palette.GOOD : Palette.TEXT);
        return y - 26;
    }

    private String eta(Window w) {
        double t = ops.elapsedS();
        if (w.contains(t)) return "NOW";
        double s = w.start - t;
        return String.format(Locale.ROOT, "in %d h %02d min", (int) (s / 3600), (int) (s % 3600 / 60));
    }

    private void drawFinished() {
        draw.rect(0, 0, 640, 360, Palette.DARK, 0.6f);
        draw.panel(140, 120, 360, 120, Palette.ACCENT);
        boolean good = ops.received() >= 0.9;
        draw.textCenter(assets.big, "OPERATIONS COMPLETE", 320, 222, good ? Palette.GOOD : Palette.WARN);
        draw.textCenter(assets.font, String.format(Locale.ROOT, "%d%% of the picture is on the ground.", Math.round(ops.received() * 100)), 320, 180, Palette.TEXT);
        draw.textCenter(assets.font, "Press action for the mission report", 320, 150, blink());
    }

    /** Para las pruebas: el motor de operaciones de esta pantalla. */
    MissionOps ops() {
        return ops;
    }
}
