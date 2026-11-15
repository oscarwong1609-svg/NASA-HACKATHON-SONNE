package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.missionlab.MissionLabGame;
import com.missionlab.data.Catalog;
import com.missionlab.game.Controls;
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

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
        stars(90, 42);
        draw.circle(270, 40, 34, Palette.EARTH);
        draw.circle(262, 48, 12, Palette.LAND);
        draw.circle(282, 30, 8, Palette.LAND);
        double a = time * 0.6;
        float sx = 270 + (float) Math.cos(a) * 48, sy = 40 + (float) Math.sin(a) * 18;
        draw.rect(sx - 1, sy - 1, 3, 3, Palette.TEXT);
        draw.rect(sx - 4, sy, 3, 1, Palette.ACCENT);
        draw.rect(sx + 2, sy, 3, 1, Palette.ACCENT);
    }

    @Override
    protected void drawUi() {
        draw.text(assets.big, "MISSION LAB", 40, 330, Palette.TEXT);
        draw.text(assets.font, "Design a satellite. Launch it. Run it from Mission Control.", 42, 290, Palette.ACCENT);

        draw.text(assets.small, "CHOOSE A MISSION  (up / down)", 42, 262, Palette.DIM);
        float y = 244;
        for (int i = 0; i < Catalog.MISSIONS.size(); i++) {
            Mission m = Catalog.MISSIONS.get(i);
            boolean sel = i == missionIndex;
            if (sel) draw.rect(36, y - 34, 400, 40, Palette.PANEL, 0.9f);
            if (sel) draw.frame(36, y - 34, 400, 40, 1, Palette.WARN);
            draw.text(assets.small, "MISSION " + m.number + (m.number == 1 ? "  -  start here" : "  -  harder trade-offs"), 46, y, sel ? Palette.WARN : Palette.DIM);
            draw.text(assets.font, m.title, 46, y - 14, sel ? Palette.TEXT : Palette.DIM);
            SuccessTier best = session.bestTier(m);
            int score = session.bestScore(m);
            String progress = score < 0 ? "Not flown yet" : "Best: " + best.medal + ", " + score + "/100";
            draw.textRight(assets.small, progress, 428, y, score < 0 ? Palette.DIM : medalColor(best));
            y -= 48;
        }

        draw.text(assets.font, "Players:  <  " + session.playerCount + "  >", 42, 140, Palette.TEXT);
        y = 120;
        for (int i = 0; i < session.playerCount; i++) {
            String who = session.playerCount == 1 ? "You (every role)" : Role.values()[i].label;
            draw.text(assets.small, "P" + (i + 1) + "  " + who + ":  " + Controls.PLAYERS[i].label, 42, y,
                    session.playerCount == 1 ? Palette.GOOD : Palette.role(Role.values()[i]));
            y -= 14;
        }

        draw.text(assets.font, "Press E, Enter or Space to start", 42, 52, blink());
        draw.text(assets.small, "A NASA Space Apps Challenge game. Simplified numbers, based on real CubeSat missions.", 42, 24, Palette.DIM);
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
