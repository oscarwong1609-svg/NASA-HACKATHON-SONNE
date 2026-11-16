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
 * Mission Control: the team operates the satellite for one day in orbit.
 * LEFT sends IMAGE (Scientist), RIGHT sends DOWNLINK (Ground control); holding ACTION speeds up time.
 * Commands wait in the on-board queue until their conditions are met.
 * Time runs fast between windows and slows down by itself for target passes and contacts.
 */
public class OperationsScreen extends BaseScreen {
    /** Mission seconds per screen second. */
    private static final double WARP_CRUISE = 900, WARP_TARGET = 45, WARP_CONTACT = 120, FAST_FORWARD = 4;

    private final MissionOps ops;
    private final double powerFactor;
    private final Mission mission;
    private double warp = WARP_CRUISE;
    private boolean finishedSoundPlayed;

    public OperationsScreen(MissionLabGame game) {
        super(game);
        mission = session.mission;
        // If a panel jammed during deployment (reported by the launch timeline), there is half the power.
        double pf = 1;
        LinkedQueue<FlightEvent> t = session.result.timeline();
        while (!t.isEmpty()) {
            FlightEvent e = t.dequeue();
            if (e.kind == FlightEvent.Kind.PANELS && e.status == FlightEvent.Status.WARN) pf = 0.5;
        }
        powerFactor = pf;
        ops = new MissionOps(session.design, mission, powerFactor);
    }

    // ------------------------------------------------------------------ logic

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

    /** Time slows down when something happens that the team can act on. */
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

    // ------------------------------------------------------------------ world

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
    }

    // ------------------------------------------------------------------ text

    @Override
    protected void drawUi() {
        phaseBar(Phase.OPERATIONS, 16, 352);
        double tt = ops.trackTime();
        draw.terminal(6, 112, 424, 230, "MISSION CONTROL  //  GROUND TRACK", Palette.ACCENT, time);
        int h = (int) (ops.elapsedS() / 3600), mi = (int) (ops.elapsedS() % 3600 / 60);
        boolean slow = warp < WARP_CRUISE;
        draw.textRight(assets.label, String.format(Locale.ROOT, "T+%02d:%02d / 30:00   x%.0f", h, mi, warp), 392, 338,
                slow ? Palette.WARN : Palette.DIM);

        drawMap(tt);
        drawBanner();
        drawLog();
        drawSidebar();

        draw.rect(0, 0, 640, 12, Palette.PANEL);
        draw.rect(0, 12, 640, 1, draw.dim(Palette.ACCENT, 0.5f));
        String who = session.playerCount > 1 ? " (Scientist)" : "";
        String who2 = session.playerCount > 1 ? " (Ground control)" : "";
        float x = 8;
        x += draw.chip(assets.label, "LEFT", x, 8, Palette.WARN) + 4;
        x += draw.text(assets.small, "IMAGE" + who, x, 8, Palette.TEXT) + 10;
        x += draw.chip(assets.label, "RIGHT", x, 8, Palette.GOOD) + 4;
        x += draw.text(assets.small, "DOWNLINK" + who2, x, 8, Palette.TEXT) + 10;
        x += draw.chip(assets.label, "HOLD ACTION", x, 8, Palette.METAL) + 4;
        x += draw.text(assets.small, "fast-forward", x, 8, Palette.TEXT) + 10;
        x += draw.chip(assets.label, "ESC", x, 8, Palette.METAL) + 4;
        draw.text(assets.small, "end", x, 8, Palette.TEXT);
        if (ops.finished()) drawFinished();
    }

    private void drawMap(double tt) {
        WorldMap map = assets.worldMap;
        map.bounds(12, 116, 412, 206);
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
        float w = draw.width(assets.small, text) + 26;
        float bx = Math.round(218 - w / 2);
        draw.rect(bx, 118, w, 13, Palette.DARK, 0.85f);
        draw.rect(bx, 118, w, 13, c, 0.25f * pulse);
        draw.brackets(bx, 118, w, 13, 3, c);
        draw.led(bx + 5, 122, c, pulse > 0.8f);
        draw.text(assets.small, text, bx + 15, 127, c);
    }

    private void drawLog() {
        float top = draw.terminal(6, 16, 424, 92, "MISSION LOG", Palette.METAL, time);
        List<String> log = ops.log();
        float y = top;
        for (int i = Math.max(0, log.size() - 6); i < log.size(); i++) {
            String line = log.get(i);
            boolean last = i == log.size() - 1;
            Color c = line.contains("SAFE") ? Palette.BAD : line.contains("complete") || line.contains("covered") ? Palette.GOOD : Palette.TEXT;
            draw.text(assets.small, ">", 12, y, last ? c : Palette.WALL);
            float w = draw.text(assets.small, draw.fit(assets.small, line, 396), 20, y, last ? c : Palette.DIM);
            if (last && ((int) (time * 2.5f)) % 2 == 0) draw.rect(22 + w, y - 5, 4, 5, c);
            y -= 11;
        }
    }

    private void drawSidebar() {
        float x = 442, w = 186;
        float y = draw.terminal(436, 16, 198, 326, "SPACECRAFT", Palette.ACCENT, time) + 1;
        draw.heading("OBJECTIVES", x, y, Palette.ACCENT);
        y = objective(mission.night ? "1  Image the target at night" : "1  Image the target area", ops.coverage(), x, y - 14);
        y = objective("2  Bring the picture home", ops.received(), x, y - 4);

        State sat = ops.satellite();
        y -= 8;
        draw.heading("TELEMETRY", x, y, Palette.ACCENT);
        y -= 13;
        draw.text(assets.small, String.format(Locale.ROOT, "%.0f%s %.0f%s   %.0f km   %.1f km/s", Math.abs(sat.lat), sat.lat >= 0 ? "N" : "S",
                Math.abs(sat.lon), sat.lon >= 0 ? "E" : "W", session.design.orbit.altitudeKm, OrbitMechanics.speedKmS(session.design.orbit)), x, y, Palette.TEXT);
        y -= 12;
        draw.text(assets.small, sat.sunlit ? "In sunlight: panels charging" : "In Earth's shadow: on battery", x, y, sat.sunlit ? Palette.WARN : Palette.DIM);
        y -= 12;
        draw.text(assets.small, String.format(Locale.ROOT, "Power in %.1f W, out %.1f W", ops.generationW(), ops.loadW()), x, y,
                ops.generationW() >= ops.loadW() ? Palette.GOOD : Palette.WARN);

        y -= 18;
        draw.heading("BATTERY", x, y, Palette.ACCENT);
        float by = y - 18, bw = 140;
        double soc = ops.batteryFraction();
        boolean low = soc < ops.batteryFloor() + 0.03;
        // A 20-cell battery with its terminal, draining from right to left.
        draw.rect(x, by - 1, bw + 2, 10, Palette.DARK);
        draw.frame(x - 1, by - 2, bw + 4, 12, 1, Palette.METAL);
        draw.rect(x + bw + 3, by + 1, 2, 6, Palette.METAL);
        float segW = bw / 20f;
        for (int i = 0; i < 20; i++) {
            if ((i + 0.5) / 20.0 > soc) break;
            draw.rect(Math.round(x + 1 + i * segW), by, Math.max(1, Math.round(segW) - 1), 7, low ? Palette.BAD : Palette.GOOD);
        }
        float fx = Math.round(x + 1 + (float) (bw * ops.batteryFloor()));
        draw.rect(fx, by - 3, 1, 13, Palette.BAD);
        draw.text(assets.small, Math.round(soc * 100) + "%", x + bw + 10, by + 7, Palette.TEXT);
        draw.text(assets.small, "red line = safe limit", x, by - 5, Palette.DIM);

        y = by - 20;
        draw.heading("DATA", x, y, Palette.ACCENT);
        y -= 13;
        draw.text(assets.small, String.format(Locale.ROOT, "On board: %.2f GB", ops.storedGb()), x, y, Palette.TEXT);
        y -= 12;
        draw.text(assets.small, String.format(Locale.ROOT, "On Earth: %.2f of %.1f GB", ops.receivedGb(), ops.imageGb()), x, y, Palette.TEXT);

        y -= 16;
        draw.heading("UPLINK QUEUE (in order)", x, y, Palette.ACCENT);
        y -= 13;
        LinkedQueue<Command> q = ops.queue();
        if (q.isEmpty()) {
            draw.text(assets.small, "Empty. LEFT / RIGHT to add.", x, y, Palette.DIM);
            y -= 12;
        } else {
            int i = 0;
            for (Command c : q) {
                String waiting = i > 0 ? "next in line" : c == Command.IMAGE ? "at target pass" : "at next station";
                float cw = draw.chip(assets.label, c.name(), x, y, c == Command.IMAGE ? Palette.WARN : Palette.GOOD);
                draw.text(assets.small, waiting, x + cw + 6, y, Palette.DIM);
                y -= 14;
                i++;
            }
        }

        y -= 6;
        draw.heading("COMING UP", x, y, Palette.ACCENT);
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
        draw.frame(x - 1, y - 21, bw + 2, 8, 1, Palette.WALL);
        draw.rect(x, y - 20, Math.round(bw * done), 6, ok ? Palette.GOOD : Palette.WARN);
        if (done > 0) draw.rect(x, y - 16, Math.round(bw * done), 1, Color.WHITE, 0.3f);
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
        draw.rect(0, 0, 640, 360, Palette.DARK, 0.65f);
        boolean good = ops.received() >= 0.9;
        Color c = good ? Palette.GOOD : Palette.WARN;
        draw.terminal(130, 112, 380, 136, "END OF OPERATIONS", c, time);
        draw.sprite(assets.sprites.get("cubesat"), 320 - 19, 196, 2);
        draw.textCenter(assets.big, "OPERATIONS COMPLETE", 320, 188, c);
        draw.textCenter(assets.font, String.format(Locale.ROOT, "%d%% of the picture is on the ground.", Math.round(ops.received() * 100)), 320, 160, Palette.TEXT);
        draw.textCenter(assets.font, "Press action for the mission report", 320, 132, blink());
    }

    /** For tests: this screen's operations engine. */
    MissionOps ops() {
        return ops;
    }
}
