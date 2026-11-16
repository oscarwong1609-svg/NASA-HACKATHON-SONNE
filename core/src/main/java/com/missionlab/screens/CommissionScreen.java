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
 * The mission briefing, in two pages:
 * 1. The science question, the area on the map, the budget, the date and real NASA facts.
 * 2. How to win: minimum, full and exemplary success with concrete numbers, the plan and the team roles.
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
                time = session.mission.brief.length() / CHARS_PER_SECOND; // first press: show everything
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
        header(m, "BRIEFING 1 OF 2: THE QUESTION", m.title);

        // The transmission: the brief is typed letter by letter, with a terminal cursor.
        float ty = draw.terminal(16, 180, 378, 112, "INCOMING TRANSMISSION", Palette.ACCENT, time);
        int shown = Math.min(m.brief.length(), (int) (time * CHARS_PER_SECOND));
        String typed = m.brief.substring(0, shown);
        draw.wrap(assets.font, typed, 24, ty, 362, Palette.TEXT);
        if (((int) (time * 3)) % 2 == 0 || shown < m.brief.length()) {
            java.util.List<String> lines = assets.font.lines(typed, 362);
            String last = lines.get(lines.size() - 1);
            float cx = 24 + draw.width(assets.font, last) + (last.isEmpty() ? 0 : 3);
            float cy = ty - (lines.size() - 1) * assets.font.lineHeight;
            draw.rect(cx, cy - assets.font.capHeight, 6, assets.font.capHeight, Palette.ACCENT);
        }

        // The dossier: one row per item, with its icon.
        float y = draw.terminal(16, 44, 378, 128, "MISSION DOSSIER", Palette.WARN, time) - 2;
        String lat = String.format(Locale.ROOT, "%.0f deg %s", Math.abs(m.targetLatitude), m.targetLatitude >= 0 ? "N" : "S");
        y = dossier("ic_target", "TARGET", m.targetName + " (" + lat + ")" + (m.night ? ", at night" : ", in daylight"), y);
        y = dossier("ic_budget", "BUDGET", budget(m.budgetK) + " for parts and launch", y);
        y = dossier("ic_launch", "LAUNCH", String.format(Locale.ROOT, "The rocket leaves in %.0f months", m.monthsToLaunch), y);
        y = dossier("ic_life", "LIFETIME", String.format(Locale.ROOT, "The satellite must work for %.0f year%s", m.missionYears, m.missionYears == 1 ? "" : "s"), y);
        dossier("ic_detail", "DETAIL", String.format(Locale.ROOT, "Pixels of %.0f m or smaller, %s", m.requiredGsdM, m.resolutionReason), y);

        // Map with the mission area
        float my = draw.terminal(398, 196, 236, 144, "TARGET AREA", Palette.ACCENT, time);
        WorldMap map = assets.worldMap;
        map.bounds(404, my - 112, 224, 112);
        map.drawBase(draw);
        map.drawRegion(draw, m, m.night ? Palette.FIRE : Palette.WARN, (float) Math.abs(Math.sin(time * 3)));
        map.frame(draw, Palette.WALL);

        float fy = draw.terminal(398, 44, 236, 144, "REAL NASA SCIENCE", Palette.GOOD, time);
        for (String fact : m.facts) {
            draw.rect(406, fy - 4, 3, 3, Palette.GOOD);
            fy -= draw.wrap(assets.small, fact, 414, fy, 212, Palette.TEXT) + 7;
        }

        draw.text(assets.font, "Press action: how to win", 20, 30, blink());
    }

    /** Header shared by both pages: patch, label and large title. */
    private void header(Mission m, String page, String title) {
        draw.sprite(assets.sprites.get(patchSprite(m)), 16, 300, 1);
        draw.text(assets.label, "MISSION " + m.number + "  //  " + page, 48, 330, Palette.WARN);
        draw.textShadow(assets.big, title, 48, 318, Palette.TEXT);
    }

    /** One dossier row: icon, label and value (on one or two lines). Returns the y of the next row. */
    private float dossier(String icon, String label, String value, float y) {
        draw.sprite(assets.sprites.get(icon), 24, y - 7, 1);
        draw.text(assets.label, label, 38, y, Palette.DIM);
        float h = draw.wrap(assets.small, value, 104, y, 282, Palette.ACCENT);
        float next = y - Math.max(h, 7) - 12;
        for (float x = 24; x < 386; x += 4) draw.rect(x, next + 6, 2, 1, Palette.WALL, 0.7f);
        return next;
    }

    private void drawHowToWin() {
        Mission m = session.mission;
        header(m, "BRIEFING 2 OF 2: HOW TO WIN", "Success criteria");
        draw.wrap(assets.small, "Like every NASA mission, yours has success criteria agreed before launch. Your medal depends on which ones you meet.",
                16, 288, 370, Palette.DIM);

        float y = 262;
        String lightWord = m.night ? "at night" : "in daylight";
        y = tier(SuccessTier.MINIMUM, String.format(Locale.ROOT,
                "A usable picture of %s %s: at least %.0f%% of it received, pixels no bigger than %.0f m.",
                m.targetName, lightWord, SuccessTier.MIN_COMPLETENESS * 100, m.requiredGsdM / SuccessTier.MIN_RESOLUTION), y);
        y = tier(SuccessTier.FULL, String.format(Locale.ROOT,
                "Pixels of %.0f m or better, at least %.0f%% of the picture received, and the satellite works for %.0f year%s.",
                m.requiredGsdM, SuccessTier.FULL_COMPLETENESS * 100, m.missionYears, m.missionYears == 1 ? "" : "s"), y);
        tier(SuccessTier.EXEMPLARY, "Full success and a score of " + SuccessTier.EXEMPLARY_SCORE
                + " or more: 10% spare weight, power, battery and budget, a month to spare, and no unfixed risks.", y);

        drawPlan();

        if (session.playerCount > 1) {
            float ty = 70;
            draw.text(assets.label, "YOUR TEAM", 16, ty + 12, Palette.DIM);
            for (int i = 0; i < session.playerCount; i++) {
                Role r = Role.values()[i];
                float rx = 16 + (i % 2) * 190, ry = ty - (i / 2) * 22;
                draw.sprite(assets.sprites.astronaut(Palette.role(r), 0, false), rx, ry - 14, 1);
                draw.text(assets.label, "P" + (i + 1) + " " + r.label.toUpperCase(Locale.ROOT), rx + 14, ry, Palette.role(r));
                draw.text(assets.small, draw.fit(assets.small, r.goal, 172), rx + 14, ry - 9, Palette.TEXT);
            }
        }
        draw.text(assets.font, "Press action to enter the hangar", 404, 30, blink());
    }

    /** "The plan" as a Mission Control console display: numbered steps on a timeline. */
    private void drawPlan() {
        float x = 400, w = 234;
        float y = draw.terminal(x, 88, w, 216, "FLIGHT PLAN  //  MCC", Palette.ACCENT, time) - 2;
        String[][] steps = {
                {"BUILD", "Carry parts to the blueprint. Use the orbit map, rocket console and ground station desk."},
                {"FLIGHT REVIEW", "Launch only when every system is GO or WARN. A single NO-GO blocks the pad."},
                {"LAUNCH", "Watch liftoff and the first orbit."},
                {"MISSION CONTROL", "Send IMAGE over the target and DOWNLINK at ground stations."},
                {"RESULTS", "Your photo, medal and what to improve."}};
        float lineTop = y - 3, lineBottom = y;
        for (int i = 0; i < steps.length; i++) {
            boolean first = i == 0;
            draw.rect(x + 9, y - 6, 7, 7, first ? Palette.WARN : Palette.ACCENT);
            draw.rect(x + 10, y - 5, 5, 5, Palette.PANEL);
            draw.rect(x + 11, y - 4, 3, 3, first ? Palette.WARN : Palette.ACCENT);
            float nw = draw.text(assets.label, String.format(Locale.ROOT, "%02d", i + 1), x + 22, y, Palette.DIM);
            draw.text(assets.label, steps[i][0], x + 28 + nw, y, first ? Palette.WARN : Palette.TEXT);
            float h = draw.wrap(assets.small, steps[i][1], x + 22, y - 11, w - 32, Palette.DIM);
            lineBottom = y - 6;
            y -= 11 + h + 12;
        }
        for (float ly = lineBottom; ly < lineTop - 6; ly += 3) draw.rect(x + 12, ly, 1, 2, Palette.ACCENT, 0.5f);
        String prompt = "> CREW READY FOR ASSIGNMENT";
        float pw = draw.text(assets.small, prompt, x + 10, 102, Palette.GOOD);
        if (((int) (time * 2.5f)) % 2 == 0) draw.rect(x + 12 + pw, 96, 4, 6, Palette.GOOD);
    }

    private float tier(SuccessTier t, String text, float y) {
        draw.sprite(assets.sprites.get(medalSprite(t)), 16, y - 30, 1);
        draw.text(assets.label, t.medal.toUpperCase(Locale.ROOT), 50, y, TitleScreen.medalColor(t));
        draw.text(assets.font, t.label, 50 + draw.width(assets.label, t.medal.toUpperCase(Locale.ROOT)) + 8, y + 1, Palette.TEXT);
        float h = draw.wrap(assets.small, text, 50, y - 14, 330, Palette.TEXT);
        return Math.min(y - 14 - h - 14, y - 46);
    }
}
