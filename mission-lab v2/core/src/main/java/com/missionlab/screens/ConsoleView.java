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

    /** Dibuja la consola entera. operator = quién la maneja (para el pie). */
    final void draw(float time, String operator) {
        draw.rect(0, 0, 640, 360, Palette.DARK, 0.97f);
        draw.rect(0, 340, 640, 20, Palette.PANEL);
        draw.rect(0, 339, 640, 1, Palette.ACCENT);
        draw.text(assets.font, title(), 12, 355, Palette.ACCENT);
        draw.textRight(assets.small, operator + " at the controls", 628, 353, Palette.DIM);
        Budgets b = Budgets.of(session.run);
        Readiness review = Readiness.of(session.run);
        drawBody(time, b, review);
        draw.rect(0, 0, 640, 12, Palette.PANEL);
        draw.text(assets.small, "LEFT / RIGHT: " + choiceHint() + "     ACTION: done", 12, 10, Palette.TEXT);
        draw.textRight(assets.small, "Flight review: " + reviewSummary(review), 628, 10, review.go() ? Palette.GOOD : Palette.WARN);
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
