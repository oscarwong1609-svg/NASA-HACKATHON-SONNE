package com.missionlab.screens;

import com.missionlab.MissionLabGame;
import com.missionlab.game.Controls;
import com.missionlab.game.Palette;
import com.missionlab.game.WorldMap;
import com.missionlab.model.Mission;
import com.missionlab.model.Role;
import com.missionlab.sim.SuccessTier;

import java.util.Locale;

/**
 * La sesión informativa, en dos páginas:
 * 1. La pregunta científica, la zona en el mapa, el dinero, la fecha y datos reales de NASA.
 * 2. Cómo se gana: éxito mínimo, completo y ejemplar con números concretos, el plan y los papeles del equipo.
 */
public class CommissionScreen extends BaseScreen {
    private static final float CHARS_PER_SECOND = 90;
    private int page;

    public CommissionScreen(MissionLabGame game) {
        super(game);
    }

    @Override
    protected void update(float delta) {
        if (Controls.back()) {
            game.setScreen(new TitleScreen(game));
            return;
        }
        if (Controls.confirm()) {
            int shown = (int) (time * CHARS_PER_SECOND);
            if (page == 0 && shown < session.mission.brief.length()) {
                time = session.mission.brief.length() / CHARS_PER_SECOND; // primer toque: mostrar todo
            } else if (page == 0) {
                page = 1;
                assets.play("click");
            } else {
                assets.play("place");
                game.setScreen(new HangarScreen(game));
            }
        }
    }

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
        stars(60, 9);
    }

    @Override
    protected void drawUi() {
        phaseBar(Phase.BRIEFING, 16, 352);
        if (page == 0) drawQuestion();
        else drawHowToWin();
    }

    private void drawQuestion() {
        Mission m = session.mission;
        draw.text(assets.small, "MISSION " + m.number + "  |  BRIEFING 1 OF 2: THE QUESTION", 20, 326, Palette.WARN);
        draw.text(assets.big, m.title, 20, 312, Palette.TEXT);
        int shown = Math.min(m.brief.length(), (int) (time * CHARS_PER_SECOND));
        draw.wrap(assets.font, m.brief.substring(0, shown), 20, 270, 370, Palette.TEXT);

        float y = 156;
        String lat = String.format(Locale.ROOT, "%.0f deg %s", Math.abs(m.targetLatitude), m.targetLatitude >= 0 ? "N" : "S");
        line("TARGET", m.targetName + " (" + lat + ")" + (m.night ? ", at night" : ", in daylight"), y);
        line("BUDGET", budget(m.budgetK), y -= 14);
        line("LAUNCH", String.format(Locale.ROOT, "The rocket leaves in %.0f months", m.monthsToLaunch), y -= 14);
        line("LIFETIME", String.format(Locale.ROOT, "The satellite must work for %.0f year%s", m.missionYears, m.missionYears == 1 ? "" : "s"), y -= 14);
        y -= 14;
        draw.text(assets.small, "DETAIL", 20, y, Palette.DIM);
        draw.wrap(assets.small, String.format(Locale.ROOT, "Pixels of %.0f m or smaller, %s", m.requiredGsdM, m.resolutionReason), 86, y, 300, Palette.ACCENT);

        // Mapa con la zona del encargo
        WorldMap map = assets.worldMap;
        map.bounds(404, 214, 224, 112);
        map.drawBase(draw);
        map.drawRegion(draw, m, m.night ? Palette.FIRE : Palette.WARN, (float) Math.abs(Math.sin(time * 3)));
        map.frame(draw, Palette.WALL);
        draw.text(assets.small, "TARGET AREA", 404, 208, Palette.DIM);

        draw.text(assets.small, "REAL NASA SCIENCE", 404, 186, Palette.ACCENT);
        float fy = 172;
        for (String fact : m.facts) fy -= draw.wrap(assets.small, "- " + fact, 404, fy, 224, Palette.TEXT) + 6;

        draw.text(assets.font, "Press action: how to win", 20, 30, blink());
    }

    private void drawHowToWin() {
        Mission m = session.mission;
        draw.text(assets.small, "MISSION " + m.number + "  |  BRIEFING 2 OF 2: HOW TO WIN", 20, 326, Palette.WARN);
        draw.text(assets.big, "Success criteria", 20, 312, Palette.TEXT);
        draw.wrap(assets.small, "Like every NASA mission, yours has success criteria agreed before launch. Your medal depends on which ones you meet.",
                20, 276, 380, Palette.DIM);

        float y = 244;
        String lightWord = m.night ? "at night" : "in daylight";
        y = tier(SuccessTier.MINIMUM, String.format(Locale.ROOT,
                "A usable picture of %s %s: at least %.0f%% of it received, pixels no bigger than %.0f m.",
                m.targetName, lightWord, SuccessTier.MIN_COMPLETENESS * 100, m.requiredGsdM / SuccessTier.MIN_RESOLUTION), y);
        y = tier(SuccessTier.FULL, String.format(Locale.ROOT,
                "Pixels of %.0f m or better, at least %.0f%% of the picture received, and the satellite works for %.0f year%s.",
                m.requiredGsdM, SuccessTier.FULL_COMPLETENESS * 100, m.missionYears, m.missionYears == 1 ? "" : "s"), y);
        tier(SuccessTier.EXEMPLARY, "Full success and a score of " + SuccessTier.EXEMPLARY_SCORE
                + " or more: 10% spare weight, power, battery and budget, a month to spare, and no unfixed risks.", y);

        draw.panel(410, 56, 218, 246, Palette.WALL);
        draw.text(assets.small, "THE PLAN", 420, 292, Palette.ACCENT);
        String[] steps = {
                "1  Build: carry parts to the blueprint. Use the orbit map, rocket console and ground station desk.",
                "2  Flight review: launch only when every system is GO or WARN. A single NO-GO blocks the pad.",
                "3  Launch: watch liftoff and the first orbit.",
                "4  Mission Control: send IMAGE over the target and DOWNLINK at ground stations.",
                "5  Results: your photo, medal and what to improve."};
        float sy = 276;
        for (String s : steps) sy -= draw.wrap(assets.small, s, 420, sy, 200, Palette.TEXT) + 5;

        if (session.playerCount > 1) {
            float ty = 82;
            draw.text(assets.small, "YOUR TEAM", 20, ty, Palette.DIM);
            for (int i = 0; i < session.playerCount; i++) {
                Role r = Role.values()[i];
                draw.text(assets.small, "P" + (i + 1) + "  " + r.label + ":  " + r.goal, 20, ty -= 13, Palette.role(r));
            }
        }
        draw.text(assets.font, "Press action to enter the hangar", 404, 30, blink());
    }

    private float tier(SuccessTier t, String text, float y) {
        draw.chip(assets.small, t.medal.toUpperCase(Locale.ROOT), 20, y, TitleScreen.medalColor(t));
        draw.text(assets.font, t.label, 80, y + 1, Palette.TEXT);
        float h = draw.wrap(assets.small, text, 80, y - 15, 310, Palette.TEXT);
        return y - 15 - h - 16;
    }

    private void line(String label, String value, float y) {
        draw.text(assets.small, label, 20, y, Palette.DIM);
        draw.text(assets.small, value, 86, y, Palette.ACCENT);
    }
}
