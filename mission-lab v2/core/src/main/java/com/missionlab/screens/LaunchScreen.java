package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.missionlab.MissionLabGame;
import com.missionlab.ds.LinkedQueue;
import com.missionlab.game.Controls;
import com.missionlab.game.Palette;
import com.missionlab.model.Orbit;
import com.missionlab.sim.FlightEvent;
import com.missionlab.sim.FlightEvent.Kind;
import com.missionlab.sim.FlightEvent.Status;
import com.missionlab.sim.SimResult;

import java.util.ArrayList;
import java.util.List;

/**
 * El lanzamiento y la primera órbita (LEOP): cuenta atrás, despegue, separación, paneles y primer contacto.
 * Si el satélite contesta, el equipo pasa a Mission Control; si no, directo al informe.
 *
 * La pantalla no decide nada: reproduce la línea de tiempo que escribió el simulador.
 * Es una cola FIFO; cada cierto tiempo saca (dequeue) el siguiente suceso, cambia la escena según
 * lo que dice y lo añade a la lista de la derecha. Cuando la cola se vacía, el vuelo ha terminado.
 * Si algo falla, se ve en el suceso donde falló (el cohete explota, o el satélite queda en silencio).
 */
public class LaunchScreen extends BaseScreen {
    private static final float COUNTDOWN = 3f;
    /** Segundos que dura la subida del cohete en pantalla. */
    private static final float ASCENT = 3f;
    private static final float BOOM_DELAY = 1.6f;
    private static final float LOG_X = 380;

    private final SimResult result;
    /** Sucesos que todavía no se han mostrado, en orden cronológico. */
    private final LinkedQueue<FlightEvent> pending;
    /** Sucesos ya mostrados (solo para dibujar la lista). */
    private final List<FlightEvent> shown = new ArrayList<>();
    /** Momento (en segundos de pantalla) en que se saca el siguiente suceso de la cola. */
    private float nextEventAt;
    private boolean endPlayed;

    // Estado de la escena, que cambia con cada suceso que sale de la cola.
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

    /** Segundos que se queda en pantalla cada suceso antes de sacar el siguiente. */
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
        // Saca de la cola todos los sucesos que ya tocan (normalmente uno).
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

    /** Cambia la escena según el suceso que acaba de salir de la cola. */
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

    // ------------------------------------------------------------------ mundo

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
        stars(80, 5);
        if (inOrbit) drawOrbitScene();
        else drawPadScene();
    }

    private void drawPadScene() {
        float ground = 30;
        draw.rect(0, 0, MissionLabGame.WORLD_W, ground, Palette.WALL);
        draw.rect(120, ground, 80, 4, Palette.METAL);
        draw.rect(200, ground, 6, 90, Palette.METAL); // torre

        float rx = 154, ry = ground + 4;
        if (liftoffAt >= 0) {
            float t = time - liftoffAt;
            float climb = t * t * 18;
            if (liftoffFailed) {
                if (t > BOOM_DELAY) {
                    drawExplosion(rx + 6, ry + Math.min(climb, 40), t - BOOM_DELAY);
                    return;
                }
                climb = Math.min(climb, 40);
            }
            ry += climb;
            drawFlame(rx + 6, ry);
        }
        drawRocket(rx, ry);
    }

    private void drawRocket(float x, float y) {
        draw.rect(x, y, 12, 44, Palette.TEXT);
        draw.rect(x + 1, y + 44, 10, 4, Palette.TEXT);
        draw.rect(x + 3, y + 48, 6, 4, Palette.BAD);
        draw.rect(x + 5, y + 52, 2, 2, Palette.BAD);
        draw.rect(x - 4, y, 4, 10, Palette.METAL);
        draw.rect(x + 12, y, 4, 10, Palette.METAL);
        draw.rect(x + 2, y + 32, 8, 5, Palette.ACCENT);
    }

    private void drawFlame(float cx, float y) {
        float f = (float) Math.sin(time * 40) * 2;
        draw.rect(cx - 4, y - 8, 8, 8, Palette.FIRE);
        draw.rect(cx - 2, y - 14 - f, 4, 6 + f, Palette.WARN);
        for (int i = 0; i < 6; i++) {
            float sx = cx + (float) Math.sin(time * 13 + i * 2) * 10;
            float sy = y - 16 - i * 4;
            draw.rect(sx, sy, 3, 3, Palette.METAL, 0.4f);
        }
    }

    private void drawExplosion(float cx, float cy, float t) {
        float r = Math.min(26, 6 + t * 40);
        float a = Math.max(0, 1 - t / 2.5f);
        draw.circle(cx, cy, r, new Color(Palette.FIRE.r, Palette.FIRE.g, Palette.FIRE.b, a));
        draw.circle(cx, cy, r * 0.6f, new Color(Palette.WARN.r, Palette.WARN.g, Palette.WARN.b, a));
        for (int i = 0; i < 10; i++) {
            double ang = i * 0.63;
            float d = t * 50;
            draw.rect(cx + (float) Math.cos(ang) * d, cy + (float) Math.sin(ang) * d - t * t * 20, 2, 2, Palette.METAL);
        }
    }

    private void drawOrbitScene() {
        float cx = 120, cy = 90, r = 48;
        draw.circle(cx, cy, r, Palette.EARTH);
        draw.circle(cx - 14, cy + 12, 18, Palette.LAND);
        draw.circle(cx + 18, cy - 16, 12, Palette.LAND);
        draw.circle(cx + 6, cy + 30, 8, Palette.TEXT); // polo con hielo

        // objetivo en el globo
        float ty = cy + (float) (session.mission.targetLatitude / 90.0) * r * 0.9f;
        draw.circle(cx - 6, ty, 2.5f, session.mission.night ? Palette.FIRE : Palette.WARN);

        // Más alta la órbita, más lejos de la Tierra se dibuja (así se ve subir al remolcador).
        boolean polar = shownOrbit.maxLatitude > 60;
        float gap = 8 + (float) (shownOrbit.altitudeKm - 400) / 40f;
        float orx = polar ? 16 : r + gap, ory = polar ? r + gap : 14;
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0 * Math.PI * 2;
            draw.rect(cx + (float) Math.cos(a) * orx, cy + (float) Math.sin(a) * ory, 1, 1, Palette.DIM);
        }
        double a = (time - orbitSince) * 1.4;
        float sx = cx + (float) Math.cos(a) * orx, sy = cy + (float) Math.sin(a) * ory;
        Color body = silent ? Palette.DIM : Palette.TEXT;
        draw.rect(sx - 2, sy - 2, 4, 4, body);
        draw.rect(sx - 7, sy - 1, 4, 2, silent ? Palette.DIM : Palette.ACCENT);
        draw.rect(sx + 3, sy - 1, 4, 2, silent ? Palette.DIM : Palette.ACCENT);
        if (onTug) draw.rect(sx - 1, sy - 6, 3, 3, Palette.WARN); // el remolcador empujando
        if (contact && !silent) {
            // pulsos de radio hacia la Tierra
            float k = (time * 2) % 1f;
            draw.rect(sx + (cx - sx) * k, sy + (cy - sy) * k, 1, 1, Palette.GOOD);
        }
    }

    // ------------------------------------------------------------------ texto

    @Override
    protected void drawUi() {
        phaseBar(Phase.LAUNCH, 16, 352);
        if (!result.launched) {
            draw.textCenter(assets.big, "LAUNCH CANCELLED", 320, 300, Palette.BAD);
            draw.wrap(assets.font, result.findings.get(0).text, 120, 250, 400, Palette.TEXT);
            prompt();
            return;
        }
        if (liftoffAt < 0) {
            int n = Math.max(1, 3 - (int) time);
            draw.textCenter(assets.big, "T-" + n, 320, 300, Palette.WARN);
            return;
        }
        if (!inOrbit && !liftoffFailed) {
            draw.textCenter(assets.big, "LIFTOFF!", 320, 320, Palette.GOOD);
            return;
        }
        if (liftoffFailed) {
            if (time > liftoffAt + BOOM_DELAY) draw.textCenter(assets.big, "LIFTOFF FAILED", 320, 330, Palette.BAD);
        } else {
            draw.text(assets.font, session.design.orbit.name, LOG_X, 334, Palette.DIM);
            draw.text(assets.small, "Route: " + session.design.route.label(), LOG_X, 318, Palette.DIM);
        }
        drawLog(liftoffFailed ? 300 : 298);
        prompt();
    }

    /** La lista de sucesos que ya salieron de la cola, con su hora de misión. */
    private void drawLog(float y) {
        for (FlightEvent e : shown) {
            Color c = e.status == Status.OK ? Palette.GOOD : e.status == Status.WARN ? Palette.WARN : Palette.BAD;
            String status = e.status == Status.OK ? "OK" : e.status == Status.WARN ? "WARN" : "FAILED";
            draw.text(assets.small, e.clock(), LOG_X, y, Palette.DIM);
            draw.text(assets.small, status, LOG_X + 212, y, c);
            float h = draw.wrap(assets.small, e.text, LOG_X + 48, y, 160, Palette.TEXT);
            y -= Math.max(h, 10) + 9;
        }
    }

    private void prompt() {
        if (!done()) return;
        float blink = 0.5f + 0.5f * (float) Math.sin(time * 4);
        String next = result.reachedOperations() ? "Press action: take command at Mission Control" : "Press action to see the report";
        draw.text(assets.font, next, 300, 36, new Color(1, 1, 1, blink));
    }
}
