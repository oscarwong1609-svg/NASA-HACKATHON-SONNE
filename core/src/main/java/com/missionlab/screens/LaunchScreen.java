package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.missionlab.MissionLabGame;
import com.missionlab.ds.LinkedQueue;
import com.missionlab.game.Controls;
import com.missionlab.game.Globe;
import com.missionlab.game.Palette;
import com.missionlab.model.Orbit;
import com.missionlab.sim.FlightEvent;
import com.missionlab.sim.FlightEvent.Kind;
import com.missionlab.sim.FlightEvent.Status;
import com.missionlab.sim.SimResult;

import java.util.ArrayList;
import java.util.List;

/**
 * The launch and first orbit (LEOP): countdown, liftoff, separation, panel deployment and first contact.
 * If the satellite answers, the team moves on to Mission Control; if not, straight to the report.
 *
 * The screen decides nothing: it replays the timeline written by the simulator.
 * It is a FIFO queue; at fixed intervals it dequeues the next event, updates the scene according to
 * what it says and appends it to the list on the right. When the queue is empty, the flight is over.
 * If something fails, it shows at the event where it failed (the rocket explodes, or the satellite stays silent).
 */
public class LaunchScreen extends BaseScreen {
    private static final float COUNTDOWN = 3f;
    /** Seconds the rocket ascent lasts on screen. */
    private static final float ASCENT = 3f;
    private static final float BOOM_DELAY = 1.6f;
    private static final float LOG_X = 380;

    private final SimResult result;
    /** Events not yet shown, in chronological order. */
    private final LinkedQueue<FlightEvent> pending;
    /** Events already shown (only for drawing the list). */
    private final List<FlightEvent> shown = new ArrayList<>();
    /** Moment (in screen seconds) when the next event is dequeued. */
    private float nextEventAt;
    private boolean endPlayed;

    // Scene state, which changes with each event dequeued.
    private float liftoffAt = -1;
    private boolean liftoffFailed;
    private boolean boomPlayed;
    private boolean inOrbit;
    private Orbit shownOrbit;
    private float orbitSince;
    private boolean onTug;
    private boolean contact;
    private boolean silent;

    public LaunchScreen(MissionLabGame game) {
        super(game);
        result = session.result;
        pending = result.timeline();
        nextEventAt = result.launched ? COUNTDOWN : 0;
    }

    /** Seconds each event stays on screen before the next one is dequeued. */
    private static float beat(FlightEvent e) {
        switch (e.kind) {
            case CANCELLED: return 3.5f;
            case LIFTOFF: return e.failed() ? 3.4f : ASCENT;
            case PANELS: return 1.2f;
            default: return 1.6f;
        }
    }

    private boolean done() {
        return pending.isEmpty() && time >= nextEventAt;
    }

    @Override
    protected void update(float delta) {
        // Dequeue every event that is due (usually one).
        while (!pending.isEmpty() && time >= nextEventAt) {
            FlightEvent e = pending.dequeue();
            apply(e);
            shown.add(e);
            nextEventAt = time + beat(e);
        }

        boolean ascending = liftoffAt >= 0 && !inOrbit && time < liftoffAt + ASCENT;
        if (ascending) shake = Math.max(shake, liftoffFailed ? 3 : 1.5f);
        if (liftoffFailed && !boomPlayed && time > liftoffAt + BOOM_DELAY) {
            boomPlayed = true;
            shake = 6;
            assets.play("fail");
        }
        if (done() && !endPlayed) {
            endPlayed = true;
            assets.play(result.failedAt == null && result.launched ? "success" : "fail");
        }
        if (done() && Controls.confirm()) {
            if (result.reachedOperations()) game.setScreen(new OperationsScreen(game));
            else game.setScreen(new ResultScreen(game));
        }
    }

    /** Updates the scene for the event that was just dequeued. */
    private void apply(FlightEvent e) {
        if (e.orbit != null) {
            if (!inOrbit) orbitSince = time;
            inOrbit = true;
            shownOrbit = e.orbit;
        }
        switch (e.kind) {
            case LIFTOFF:
                liftoffAt = time;
                liftoffFailed = e.failed();
                break;
            case TRANSFER:
                onTug = true;
                break;
            case ORBIT:
                onTug = false;
                if (e.failed()) silent = true;
                break;
            case CONTACT:
                contact = !e.failed();
                silent = e.failed();
                break;
            default:
                break;
        }
        if (e.kind != Kind.LIFTOFF && e.kind != Kind.CANCELLED) assets.play(e.failed() ? "deny" : "click");
    }

    // ------------------------------------------------------------------ world

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
        stars(80, 5);
        if (inOrbit) drawOrbitScene();
        else drawPadScene();
    }

    /** Rideshares fly on the large rocket with a fairing; ISS deliveries on one with a capsule. */
    private boolean bigRocket() {
        return session.design.route != null && session.design.route.launchVehicle().maxKg >= 20;
    }

    private void drawPadScene() {
        float ground = 30;
        // Ground, concrete pad with flame trench and lattice service tower.
        // Horizon: distant hills and spaceport lights.
        for (int x = 0; x < MissionLabGame.WORLD_W; x++) {
            float hh = 7 + 4 * (float) Math.sin(x * 0.045) + 3 * (float) Math.sin(x * 0.13 + 1.7);
            draw.rect(x, ground, 1, Math.round(hh), Palette.FLOOR_LINE);
        }
        for (int i = 0; i < 9; i++) {
            float lx = 14 + i * 33 + (i % 3) * 5;
            if (lx > 110 && lx < 210) continue;
            draw.rect(lx, ground + 3 + (i % 2), 1, 1, ((int) (time * 1.5f + i)) % 3 == 0 ? Palette.BAD : Palette.WARN, 0.8f);
        }
        draw.rect(0, 0, MissionLabGame.WORLD_W, ground, Palette.FLOOR);
        draw.rect(0, ground - 1, MissionLabGame.WORLD_W, 1, Palette.WALL);
        for (int x = 0; x < MissionLabGame.WORLD_W; x += 12) draw.rect(x, ground - 6, 6, 1, Palette.FLOOR_LINE);
        draw.rect(118, ground, 84, 4, Palette.METAL);
        draw.rect(118, ground + 3, 84, 1, Palette.TEXT, 0.5f);
        draw.rect(148, ground, 24, 2, Palette.DARK);
        float tx = 186;
        for (float y = ground + 4; y < ground + 84; y += 5) {
            draw.rect(tx, y, 1, 5, Palette.METAL);
            draw.rect(tx + 7, y, 1, 5, Palette.METAL);
            draw.rect(tx + 1, y, 6, 1, Palette.METAL, 0.7f);
            for (int k = 1; k < 5; k++) draw.rect(tx + k * 1.5f, y + k, 1, 1, Palette.METAL, 0.6f);
        }
        draw.rect(tx - 1, ground + 84, 10, 2, Palette.METAL);
        draw.rect(tx + 3, ground + 86, 1, 4, Palette.METAL);
        draw.rect(tx + 3, ground + 90, 1, 1, ((int) (time * 2)) % 2 == 0 ? Palette.BAD : Palette.DARK);

        TextureRegion rocket = assets.sprites.get(bigRocket() ? "rocket_big" : "rocket_small");
        float rw = rocket.getRegionWidth();
        float rx = 160 - rw / 2, ry = ground + 4;
        boolean gone = false;
        if (liftoffAt >= 0) {
            float t = time - liftoffAt;
            float climb = t * t * 18;
            if (liftoffFailed) {
                if (t > BOOM_DELAY) {
                    drawExplosion(160, ry + Math.min(climb, 40) + 20, t - BOOM_DELAY);
                    gone = true;
                }
                climb = Math.min(climb, 40);
            }
            if (!gone) {
                ry += climb;
                drawFlame(160, ry, t);
            }
        } else {
            // Tower arms holding the rocket and liquid-oxygen vapour before liftoff.
            draw.rect(rx + rw, ry + 40, tx - rx - rw, 1, Palette.METAL);
            draw.rect(rx + rw, ry + 20, tx - rx - rw, 1, Palette.METAL);
            for (int i = 0; i < 4; i++) {
                float k = (time * 0.6f + i * 0.25f) % 1f;
                draw.circle(rx - 2 - k * 6, ry + 8 + i * 9 + k * 4, 1.5f + k * 2, new Color(0.9f, 0.93f, 1f, 0.55f * (1 - k)));
            }
        }
        if (!gone) draw.sprite(rocket, rx, ry, 1);
        if (gone) draw.rect(rx, ground + 4, rw, 2, Palette.DARK, 0.6f);
    }

    /** Pixel-art flame: white core, yellow, orange, and smoke spreading out to the sides. */
    private void drawFlame(float cx, float y, float t) {
        float f = (float) Math.sin(time * 40);
        float len = 10 + 3 * f;
        draw.rect(cx - 4, y - 4, 8, 4, Palette.FIRE);
        draw.rect(cx - 3, y - len, 6, len - 2, Palette.FIRE);
        draw.rect(cx - 2, y - len + 2, 4, len - 3, Palette.WARN);
        draw.rect(cx - 1, y - 6, 2, 5, Color.WHITE);
        draw.rect(cx - 1, y - len - 3 - f, 2, 3, Palette.FIRE, 0.7f);
        for (int i = 0; i < 10; i++) {
            float k = ((time * 1.4f) + i * 0.1f) % 1f;
            float side = (i % 2 == 0 ? -1 : 1) * (4 + k * (12 + (i % 3) * 5));
            float sy = Math.max(30, y - len - 2 - k * 10);
            draw.circle(cx + side, sy + 2, 2 + k * 3, new Color(0.85f, 0.88f, 0.94f, 0.45f * (1 - k)));
        }
    }

    private void drawExplosion(float cx, float cy, float t) {
        float r = Math.min(26, 6 + t * 40);
        float a = Math.max(0, 1 - t / 2.5f);
        draw.circle(cx, cy, r, new Color(Palette.FIRE.r, Palette.FIRE.g, Palette.FIRE.b, a));
        draw.circle(cx - 2, cy + 1, r * 0.65f, new Color(Palette.WARN.r, Palette.WARN.g, Palette.WARN.b, a));
        draw.circle(cx - 3, cy + 2, r * 0.3f, new Color(1, 1, 1, a));
        for (int i = 0; i < 12; i++) {
            double ang = i * 0.53;
            float d = t * 50;
            draw.rect(Math.round(cx + (float) Math.cos(ang) * d), Math.round(cy + (float) Math.sin(ang) * d - t * t * 20), 2, 2, i % 3 == 0 ? Palette.FIRE : Palette.METAL);
        }
    }

    private static final float EARTH_X = 112, EARTH_Y = 92;
    private static final int EARTH_R = 48;

    private void drawOrbitScene() {
        Globe globe = assets.sprites.globe(EARTH_R).tilt(16);
        double centerLon = session.mission.targetLongitude - 25 + (time - orbitSince) * 5;
        TextureRegion earth = globe.frame(centerLon);

        // The higher the orbit, the farther from the Earth it is drawn (so the tug can be seen raising it).
        boolean polar = shownOrbit.maxLatitude > 60;
        float gap = 8 + (float) (shownOrbit.altitudeKm - 400) / 40f;
        float orx = polar ? 18 : EARTH_R + gap, ory = polar ? EARTH_R + gap : 14;
        double a = (time - orbitSince) * 1.4;
        boolean satBehind = isBehind(a, polar);

        drawOrbitDots(orx, ory, polar, true);
        if (satBehind) drawSat(a, orx, ory, true);
        draw.sprite(earth, EARTH_X - globe.size / 2f, EARTH_Y - globe.size / 2f, 1);

        // Target on the globe, if it is on the visible side.
        float[] p = globe.project(session.mission.targetLatitude, session.mission.targetLongitude, centerLon);
        if (p[2] > 0.1f) {
            float px = EARTH_X + p[0] * EARTH_R, py = EARTH_Y + p[1] * EARTH_R;
            Color tc = session.mission.night ? Palette.FIRE : Palette.WARN;
            float pulse = (time * 1.5f) % 1f;
            draw.circle(px, py, 2 + pulse * 5, new Color(tc.r, tc.g, tc.b, 0.5f * (1 - pulse)));
            draw.rect(Math.round(px) - 1, Math.round(py) - 1, 3, 3, tc);
            draw.rect(Math.round(px), Math.round(py), 1, 1, Color.WHITE);
        }
        drawOrbitDots(orx, ory, polar, false);
        if (!satBehind) drawSat(a, orx, ory, false);
    }

    /** The half of the orbit that passes behind the planet. */
    private static boolean isBehind(double a, boolean polar) {
        return polar ? Math.cos(a) < 0 : Math.sin(a) > 0;
    }

    private void drawOrbitDots(float orx, float ory, boolean polar, boolean back) {
        for (int i = 0; i < 64; i++) {
            double a = i / 64.0 * Math.PI * 2;
            if (isBehind(a, polar) != back) continue;
            float x = EARTH_X + (float) Math.cos(a) * orx, y = EARTH_Y + (float) Math.sin(a) * ory;
            draw.rect(Math.round(x), Math.round(y), 1, 1, Palette.ACCENT, back ? 0.2f : 0.55f);
        }
    }

    private void drawSat(double a, float orx, float ory, boolean behind) {
        float sx = EARTH_X + (float) Math.cos(a) * orx, sy = EARTH_Y + (float) Math.sin(a) * ory;
        boolean dim = silent || behind;
        if (onTug) draw.spriteCentered(assets.sprites.get("tug"), sx, sy - 7, 1);
        draw.spriteCentered(assets.sprites.get(dim ? "cubesat_dim" : "cubesat"), sx, sy, 1);
        if (contact && !silent && !behind) {
            // Radio pulses towards the Earth.
            for (int i = 0; i < 3; i++) {
                float k = ((time * 1.5f) + i / 3f) % 1f;
                draw.rect(Math.round(sx + (EARTH_X - sx) * k * 0.7f), Math.round(sy + (EARTH_Y - sy) * k * 0.7f), 1, 1, Palette.GOOD, 1 - k);
            }
        }
    }

    // ------------------------------------------------------------------ text

    @Override
    protected void drawUi() {
        phaseBar(Phase.LAUNCH, 16, 352);
        if (!result.launched) {
            draw.terminal(110, 200, 420, 120, "RANGE SAFETY", Palette.BAD, time);
            draw.textCenter(assets.big, "LAUNCH CANCELLED", 320, 290, Palette.BAD);
            draw.wrap(assets.font, result.findings.get(0).text, 124, 262, 392, Palette.TEXT);
            prompt();
            return;
        }
        if (liftoffAt < 0) {
            int n = Math.max(1, 3 - (int) time);
            countdown("T-" + n, Palette.WARN, "TERMINAL COUNT");
            return;
        }
        if (!inOrbit && !liftoffFailed) {
            countdown("LIFTOFF!", Palette.GOOD, "WE HAVE LIFTOFF");
            return;
        }
        if (liftoffFailed) {
            if (time > liftoffAt + BOOM_DELAY) countdown("LIFTOFF FAILED", Palette.BAD, "VEHICLE LOST");
        }
        drawLog(liftoffFailed ? 270 : 330);
        prompt();
    }

    /** The large countdown sign, with its frame and a status line. */
    private void countdown(String text, Color c, String status) {
        // To the right of the rocket, so it is not covered while ascending.
        float cx = 504;
        float w = Math.max(160, draw.width(assets.big, text) + 40);
        float x = Math.round(cx - w / 2);
        draw.rect(x, 262, w, 46, Palette.PANEL, 0.85f);
        draw.brackets(x, 262, w, 46, 8, c);
        draw.textCenter(assets.label, status, cx, 302, draw.dim(c, 0.8f));
        draw.textShadow(assets.big, text, Math.round(cx - draw.width(assets.big, text) / 2), 290, c);
    }

    /** The list of events already dequeued, with their mission time, in a terminal panel. */
    private void drawLog(float top) {
        float x = LOG_X - 8, w = 640 - LOG_X;
        float h = top - 48;
        float y = draw.terminal(x, 48, w, h, "FLIGHT LOG  //  LEOP", Palette.ACCENT, time);
        if (!liftoffFailed) {
            draw.text(assets.font, session.design.orbit.name, LOG_X, y, Palette.TEXT);
            draw.text(assets.small, "Route: " + session.design.route.label(), LOG_X, y - 13, Palette.DIM);
            y -= 26;
            for (float lx = LOG_X; lx < LOG_X + w - 16; lx += 4) draw.rect(lx, y + 5, 2, 1, Palette.WALL);
        }
        for (FlightEvent e : shown) {
            Color c = e.status == Status.OK ? Palette.GOOD : e.status == Status.WARN ? Palette.WARN : Palette.BAD;
            String status = e.status == Status.OK ? "OK" : e.status == Status.WARN ? "WARN" : "FAIL";
            draw.text(assets.small, e.clock(), LOG_X, y, Palette.DIM);
            draw.chip(assets.label, status, LOG_X + 206, y, c);
            float h2 = draw.wrap(assets.small, e.text, LOG_X + 44, y, 156, Palette.TEXT);
            y -= Math.max(h2, 7) + 10;
        }
    }

    private void prompt() {
        if (!done()) return;
        String next = result.reachedOperations() ? "Press action: take command at Mission Control" : "Press action to see the report";
        draw.textRight(assets.font, next, 628, 30, blink());
    }
}
