package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.missionlab.game.Assets;
import com.missionlab.game.Draw;
import com.missionlab.game.Palette;
import com.missionlab.game.Session;
import com.missionlab.sim.Budgets;
import com.missionlab.sim.Readiness;

import java.util.Locale;

/**
 * Base de las tres consolas del hangar (mapa de órbitas, consola del cohete, mesa de estaciones).
 * Se dibujan a pantalla completa en la capa de texto (640 x 360) mientras alguien las maneja.
 */
abstract class ConsoleView {
    protected final Draw draw;
    protected final Assets assets;
    protected final Session session;

    ConsoleView(Draw draw, Assets assets, Session session) {
        this.draw = draw;
        this.assets = assets;
        this.session = session;
    }

    abstract String title();

    /** Qué hace izquierda/derecha en esta consola. */
    abstract String choiceHint();

    abstract void drawBody(float time, Budgets b, Readiness review);

    /** El icono de la cabecera (ver {@link com.missionlab.game.Sprites}). */
    String icon() {
        return "ic_globe";
    }

    /** Dibuja la consola entera. operator = quién la maneja (para el pie). */
    final void draw(float time, String operator) {
        draw.rect(0, 0, 640, 360, Palette.DARK, 0.97f);
        draw.scanlines(0, 12, 640, 328, 0.03f);
        // Cabecera: icono, título, LEDs y quién está a los mandos.
        draw.rect(0, 340, 640, 20, Palette.PANEL);
        draw.rect(0, 339, 640, 1, Palette.ACCENT);
        draw.rect(0, 338, 640, 1, Palette.ACCENT, 0.25f);
        draw.sprite(assets.sprites.get(icon()), 10, 345, 1);
        draw.text(assets.font, title(), 24, 354, Palette.ACCENT);
        float ow = draw.width(assets.label, operator.toUpperCase(Locale.ROOT) + " AT THE CONTROLS");
        draw.textRight(assets.label, operator.toUpperCase(Locale.ROOT) + " AT THE CONTROLS", 628, 353, Palette.DIM);
        draw.led(628 - ow - 34, 346, Palette.GOOD, true);
        draw.led(628 - ow - 26, 346, Palette.WARN, ((int) (time * 2.5f)) % 2 == 0);
        draw.led(628 - ow - 18, 346, Palette.ACCENT, Math.sin(time * 3) > -0.3);
        Budgets b = Budgets.of(session.run);
        Readiness review = Readiness.of(session.run);
        drawBody(time, b, review);
        // Pie: controles y veredicto de la revisión de vuelo.
        draw.rect(0, 0, 640, 13, Palette.PANEL);
        draw.rect(0, 13, 640, 1, draw.dim(Palette.ACCENT, 0.5f));
        float x = 12;
        x += draw.chip(assets.label, "LEFT / RIGHT", x, 9, Palette.METAL) + 5;
        x += draw.text(assets.small, choiceHint(), x, 9, Palette.TEXT) + 12;
        x += draw.chip(assets.label, "ACTION", x, 9, Palette.METAL) + 5;
        draw.text(assets.small, "done", x, 9, Palette.TEXT);
        draw.textRight(assets.label, "FLIGHT REVIEW: " + reviewSummary(review).toUpperCase(Locale.ROOT), 628, 9, review.go() ? Palette.GOOD : Palette.WARN);
    }

    static String reviewSummary(Readiness r) {
        int no = r.count(Readiness.Status.NO_GO), pend = r.count(Readiness.Status.PENDING), warn = r.count(Readiness.Status.WARN);
        if (no + pend == 0) return warn == 0 ? "ALL GO" : "GO with " + warn + " warning" + (warn == 1 ? "" : "s");
        StringBuilder sb = new StringBuilder();
        if (no > 0) sb.append(no).append(" NO-GO");
        if (pend > 0) sb.append(sb.length() > 0 ? ", " : "").append(pend).append(" to do");
        if (warn > 0) sb.append(", ").append(warn).append(" warn");
        return sb.toString();
    }

    static Color statusColor(Readiness.Status s) {
        switch (s) {
            case GO: return Palette.GOOD;
            case WARN: return Palette.WARN;
            case NO_GO: return Palette.BAD;
            default: return Palette.DIM;
        }
    }

    static String statusText(Readiness.Status s) {
        switch (s) {
            case GO: return "GO";
            case WARN: return "WARN";
            case NO_GO: return "NO-GO";
            default: return "--";
        }
    }

    /** Una fila "ETIQUETA  valor" de telemetría. */
    protected float row(String label, String value, float x, float y, Color valueColor) {
        draw.text(assets.small, label, x, y, Palette.DIM);
        draw.text(assets.small, draw.fit(assets.small, value, 188 - 62), x + 62, y, valueColor);
        return y - 12;
    }

    protected static String budget(double k) {
        return BaseScreen.budget(k);
    }

    protected static String f(String fmt, Object... args) {
        return String.format(Locale.ROOT, fmt, args);
    }
}
