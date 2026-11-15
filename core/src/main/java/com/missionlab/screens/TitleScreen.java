package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.missionlab.MissionLabGame;
import com.missionlab.data.Catalog;
import com.missionlab.game.Controls;
import com.missionlab.game.Globe;
import com.missionlab.game.Palette;
import com.missionlab.model.Mission;
import com.missionlab.model.Role;
import com.missionlab.sim.SuccessTier;

/** Pantalla de inicio: número de jugadores y encargo, con el progreso de la campaña. */
public class TitleScreen extends BaseScreen {
    private int missionIndex;

    public TitleScreen(MissionLabGame game) {
        super(game);
        missionIndex = Math.max(0, Catalog.MISSIONS.indexOf(session.mission));
    }

    @Override
    protected void update(float delta) {
        int h = Controls.menuHorizontal();
        if (h != 0) {
            session.playerCount = Math.max(1, Math.min(4, session.playerCount + h));
            assets.play("click");
        }
        int v = Controls.menuVertical();
        if (v != 0) {
            int n = Catalog.MISSIONS.size();
            missionIndex = (missionIndex - v + n) % n;
            assets.play("click");
        }
        if (Controls.confirm()) {
            session.mission = Catalog.MISSIONS.get(missionIndex);
            session.newMission();
            assets.play("place");
            game.setScreen(new CommissionScreen(game));
        }
    }

    // ------------------------------------------------------------------ mundo: la Tierra y un CubeSat en órbita

    private static final float GLOBE_X = 256, GLOBE_Y = 48;
    private static final int GLOBE_R = 38;
    private static final float ORBIT_RX = 52, ORBIT_RY = 13, ORBIT_TILT = 0.18f;

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
        stars(120, 42);
        Globe globe = assets.sprites.globe(GLOBE_R).tilt(22);
        TextureRegion earth = globe.frame(-38 + time * 4.0);

        double a = time * 0.55;
        boolean behind = Math.sin(a) > 0;
        drawOrbitPath(true);
        if (behind) drawSat(a, true);
        draw.sprite(earth, GLOBE_X - globe.size / 2f, GLOBE_Y - globe.size / 2f, 1);
        drawOrbitPath(false);
        if (!behind) drawSat(a, false);
    }

    /** La órbita como un anillo inclinado de puntos; la mitad de atrás queda tapada por la Tierra. */
    private void drawOrbitPath(boolean back) {
        for (int i = 0; i < 72; i++) {
            double a = i / 72.0 * Math.PI * 2;
            if ((Math.sin(a) > 0) != back) continue;
            float x = GLOBE_X + (float) Math.cos(a) * ORBIT_RX;
            float y = GLOBE_Y + (float) Math.sin(a) * ORBIT_RY + (float) Math.cos(a) * ORBIT_RX * ORBIT_TILT;
            draw.rect(Math.round(x), Math.round(y), 1, 1, Palette.ACCENT, back ? 0.18f : 0.4f);
        }
    }

    private void drawSat(double a, boolean behind) {
        float x = GLOBE_X + (float) Math.cos(a) * ORBIT_RX;
        float y = GLOBE_Y + (float) Math.sin(a) * ORBIT_RY + (float) Math.cos(a) * ORBIT_RX * ORBIT_TILT;
        draw.spriteCentered(assets.sprites.get(behind ? "cubesat_dim" : "cubesat"), x, y, 1);
    }

    // ------------------------------------------------------------------ texto

    @Override
    protected void drawUi() {
        // Logo: sombra profunda + degradado metálico, y una línea de rótulo debajo.
        float lx = 36, ly = 334;
        draw.text(assets.logo, "MISSION LAB", lx + 4, ly - 4, Palette.BLUEPRINT);
        draw.textGradient(assets.logo, "MISSION LAB", lx, ly, Palette.TEXT, Palette.ACCENT);
        float lw = draw.width(assets.logo, "MISSION LAB");
        draw.rect(lx, ly - 42, lw, 1, Palette.ACCENT, 0.5f);
        draw.rect(lx, ly - 42, 24, 1, Palette.WARN);
        draw.text(assets.label, "CUBESAT MISSION DESIGN LAB", lx + lw - draw.width(assets.label, "CUBESAT MISSION DESIGN LAB"), ly - 48, Palette.DIM);
        draw.text(assets.font, "Design a satellite. Launch it. Run it from Mission Control.", lx, ly - 64, Palette.ACCENT);

        draw.text(assets.label, "CHOOSE A MISSION", lx, 248, Palette.DIM);
        draw.text(assets.small, "up / down", lx + 4 + draw.width(assets.label, "CHOOSE A MISSION") + 6, 248, Palette.DIM);
        float y = 238;
        for (int i = 0; i < Catalog.MISSIONS.size(); i++) {
            missionCard(Catalog.MISSIONS.get(i), i == missionIndex, 36, y);
            y -= 54;
        }

        drawCrew(lx, 120);

        draw.text(assets.font, "Press E, Enter or Space to start", lx, 40, blink());
        draw.text(assets.small, "A NASA Space Apps Challenge game. Simplified numbers, based on real CubeSat missions.", lx, 16, Palette.DIM);
    }

    /** Tarjeta de una misión: insignia, número, título, objetivo y la mejor medalla conseguida. */
    private void missionCard(Mission m, boolean sel, float x, float top) {
        float w = 392, h = 48, bottom = top - h;
        if (sel) {
            draw.rect(x, bottom, w, h, Palette.PANEL, 0.95f);
            draw.scanlines(x + 1, bottom + 1, w - 2, h - 2, 0.05f);
            draw.frame(x, bottom, w, h, 1, draw.dim(Palette.WARN, 0.5f));
            draw.brackets(x, bottom, w, h, 7, Palette.WARN);
            if (((int) (time * 3)) % 2 == 0) draw.text(assets.label, ">", x - 10, top - 20, Palette.WARN);
        } else {
            draw.rect(x, bottom, w, h, Palette.PANEL, 0.6f);
            draw.frame(x, bottom, w, h, 1, Palette.WALL);
        }
        Color fade = sel ? Color.WHITE : new Color(0.55f, 0.58f, 0.68f, 1);
        draw.sprite(assets.sprites.get(patchSprite(m)), x + 10, bottom + 12, 1, fade);

        float tx = x + 44;
        float nw = draw.text(assets.label, "MISSION " + m.number, tx, top - 8, sel ? Palette.WARN : Palette.DIM);
        draw.text(assets.small, m.number == 1 ? "start here" : "harder trade-offs", tx + nw + 8, top - 8, Palette.DIM);
        draw.text(assets.font, m.title, tx, top - 19, sel ? Palette.TEXT : Palette.DIM);
        draw.text(assets.small, m.targetName + (m.night ? ", at night" : ", in daylight"), tx, top - 35, sel ? Palette.ACCENT : Palette.WALL);

        SuccessTier best = session.bestTier(m);
        int score = session.bestScore(m);
        float rx = x + w - 10;
        if (score >= 0) {
            draw.sprite(assets.sprites.get(medalSprite(best)), rx - 26, bottom + 8, 1, fade);
            draw.textRight(assets.label, best.medal.toUpperCase(java.util.Locale.ROOT), rx - 32, top - 8, medalColor(best));
            draw.textRight(assets.small, "best " + score + "/100", rx - 32, top - 20, Palette.DIM);
        } else {
            draw.textRight(assets.small, "Not flown yet", rx, top - 8, Palette.DIM);
        }
    }

    /** El equipo: un astronauta por jugador, con su color, su papel y sus teclas. */
    private void drawCrew(float x, float top) {
        float w = draw.text(assets.label, "CREW", x, top, Palette.DIM);
        draw.text(assets.font, "<  " + session.playerCount + (session.playerCount == 1 ? " PLAYER  >" : " PLAYERS  >"), x + w + 10, top + 2, Palette.TEXT);
        draw.text(assets.small, "left / right", x + w + 10 + draw.width(assets.font, "<  2 PLAYERS  >") + 8, top, Palette.DIM);
        for (int i = 0; i < session.playerCount; i++) {
            float px = x + i * 102;
            boolean solo = session.playerCount == 1;
            Role role = Role.values()[i];
            Color c = solo ? Palette.GOOD : Palette.role(role);
            float bob = ((int) (time * 2 + i * 0.7f)) % 2 == 0 ? 0 : 1;
            draw.rect(px + 2, top - 50, 24, 2, Palette.DARK, 0.6f);
            draw.sprite(assets.sprites.astronaut(c, 0, false), px + 4, top - 48 + bob, 2);
            draw.text(assets.label, "P" + (i + 1), px + 30, top - 16, c);
            draw.text(assets.small, solo ? "Every role" : role.label, px + 30, top - 28, Palette.TEXT);
            draw.wrap(assets.small, Controls.PLAYERS[i].label, px + 30, top - 38, 70, Palette.DIM);
        }
    }

    static Color medalColor(SuccessTier t) {
        switch (t) {
            case EXEMPLARY: return Palette.WARN;
            case FULL: return Palette.TEXT;
            case MINIMUM: return Palette.FIRE;
            default: return Palette.DIM;
        }
    }
}
