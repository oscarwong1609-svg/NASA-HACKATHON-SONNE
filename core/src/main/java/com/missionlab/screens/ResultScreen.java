package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.missionlab.MissionLabGame;
import com.missionlab.game.Controls;
import com.missionlab.game.Palette;
import com.missionlab.model.Mission;
import com.missionlab.model.Role;
import com.missionlab.sim.Budgets;
import com.missionlab.sim.Finding;
import com.missionlab.sim.PhotoQuality;
import com.missionlab.sim.Score;
import com.missionlab.sim.SimResult;
import com.missionlab.sim.SuccessTier;

import java.util.Locale;

/**
 * The final report: the photo at the quality achieved, the medal, the success criteria with the actual numbers,
 * the score breakdown and what to improve.
 * Blurry = low resolution. Cut off = not all data arrived. Black = night without a thermal camera.
 * Ocean = the orbit did not pass over the target.
 */
public class ResultScreen extends BaseScreen {
    private static final float PHOTO_X = 24, PHOTO_Y = 96, PHOTO_SIZE = 224;

    private final SimResult result;
    private final Mission mission;
    private Texture photoTexture;
    private TextureRegion photoRegion;
    private float visibleShare;

    public ResultScreen(MissionLabGame game) {
        super(game);
        result = session.result;
        mission = session.mission;
        session.record(mission, result);
        buildPhoto();
    }

    private void buildPhoto() {
        PhotoQuality q = result.photo;
        if (!q.received) return;
        Pixmap src = assets.loadPhoto(mission, q.onTarget);
        // Blur depends on how much detail was achieved versus what the scientists asked for.
        float rel = (float) Math.min(1.0, q.resolution);
        int factor = Math.max(1, Math.round(1 + (1 - rel) * (1 - rel) * 40));
        int w = Math.max(2, src.getWidth() / factor);
        int h = Math.max(2, src.getHeight() / factor);
        Pixmap small = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        small.setFilter(Pixmap.Filter.BiLinear);
        small.drawPixmap(src, 0, 0, src.getWidth(), src.getHeight(), 0, 0, w, h);
        photoTexture = new Texture(small);
        photoTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        src.dispose();
        small.dispose();
        int rows = Math.max(1, Math.round(h * (float) q.completeness));
        visibleShare = rows / (float) h;
        photoRegion = new TextureRegion(photoTexture, 0, 0, w, rows);
    }

    @Override
    protected void update(float delta) {
        if (time < 0.6f) return; // prevents skipping the screen by accident
        if (Controls.back()) {
            game.setScreen(new TitleScreen(game));
        } else if (Controls.confirm()) {
            session.newRun();
            game.setScreen(new HangarScreen(game));
        }
    }

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
        stars(40, 77);
    }

    @Override
    protected void drawUi() {
        phaseBar(Phase.RESULTS, 16, 352);
        drawPhoto();
        drawSummary();
        draw.rect(0, 0, 640, 13, Palette.PANEL);
        draw.rect(0, 13, 640, 1, draw.dim(Palette.ACCENT, 0.5f));
        float fx = 12;
        fx += draw.chip(assets.label, "ACTION", fx, 9, Palette.GOOD) + 5;
        fx += draw.text(assets.small, "back to the hangar to improve this design", fx, 9, Palette.TEXT) + 14;
        fx += draw.chip(assets.label, "ESC", fx, 9, Palette.METAL) + 5;
        draw.text(assets.small, "mission select", fx, 9, Palette.TEXT);
    }

    private void drawPhoto() {
        float x = PHOTO_X, y = PHOTO_Y, s = PHOTO_SIZE;
        PhotoQuality q = result.photo;
        Color accent = q.received && q.onTarget && q.lit ? Palette.GOOD : q.received ? Palette.WARN : Palette.BAD;
        draw.terminal(x - 8, y - 8, s + 16, s + 29, "DOWNLINKED IMAGE", accent, time);
        draw.rect(x - 1, y - 1, s + 2, s + 2, Palette.METAL);
        draw.rect(x, y, s, s, Palette.DARK);
        if (photoRegion == null) {
            draw.textCenter(assets.big, "NO PHOTO", x + s / 2, y + s / 2 + 8, Palette.BAD);
            draw.textCenter(assets.small, "The satellite never sent a picture home.", x + s / 2, y + s / 2 - 16, Palette.DIM);
        } else {
            float h = s * visibleShare;
            if (q.lit) batch.setColor(Color.WHITE);
            else batch.setColor(0.07f, 0.07f, 0.10f, 1);
            batch.draw(photoRegion, x, y + s - h, s, h);
            batch.setColor(Color.WHITE);
            draw.scanlines(x, y, s, s, 0.03f);
            if (visibleShare < 0.98f) {
                // What did not arrive: lost-signal noise.
                for (int i = 0; i < 40; i++) {
                    float nx = (float) ((Math.sin(i * 12.9898 + (int) (time * 8)) * 43758.5453) % 1 + 1) % 1;
                    float ny = (float) ((Math.sin(i * 78.233 + (int) (time * 8)) * 12345.678) % 1 + 1) % 1;
                    draw.rect(Math.round(x + nx * (s - 6)), Math.round(y + ny * (s - h - 2)), 6, 1, Palette.WALL);
                }
                draw.textCenter(assets.label, "NO DATA RECEIVED", x + s / 2, y + (s - h) / 2 + 3, Palette.BAD);
            }
            if (!q.lit) draw.textCenter(assets.label, "TOO DARK TO SEE", x + s / 2, y + s - 12, Palette.WARN);
            if (!q.onTarget) draw.textCenter(assets.label, "WRONG PLACE: OPEN OCEAN", x + s / 2, y + s - 26, Palette.WARN);
            draw.brackets(x + 4, y + 4, s - 8, s - 8, 8, new Color(1, 1, 1, 0.5f));
        }
        draw.wrap(assets.small, mission.targetName + ". " + mission.photoCredit, x - 6, y - 14, s + 12, Palette.DIM);
    }

    private void drawSummary() {
        float x = 270, w = 360;
        Score sc = result.score;
        SuccessTier tier = result.tier;
        String head = !result.launched ? "LAUNCH CANCELLED" : result.failedAt != null ? "MISSION LOST" : tier.label.toUpperCase(Locale.ROOT);
        Color c = tier == SuccessTier.NONE ? Palette.BAD : TitleScreen.medalColor(tier);
        // The medal, with a glint that sweeps across it from time to time.
        draw.sprite(assets.sprites.get(medalSprite(tier)), x, 282, 1);
        float shine = (time * 0.7f) % 2f;
        if (tier != SuccessTier.NONE && shine < 1) {
            float sx = x + 3 + shine * 20;
            draw.rect(Math.round(sx), 286, 1, 18, Color.WHITE, 0.35f);
        }
        float hx = x + 34;
        float mw = draw.chip(assets.label, tier.medal.toUpperCase(Locale.ROOT), hx, 330, c);
        draw.text(assets.label, "MISSION " + mission.number + ": " + mission.title.toUpperCase(Locale.ROOT), hx + mw + 8, 330, Palette.DIM);
        if (draw.width(assets.big, head) <= 640 - hx - 6) draw.textShadow(assets.big, head, hx, 316, c);
        else draw.textShadow(assets.font, head, hx, 312, c);
        float sw = draw.text(assets.font, "Score " + sc.total() + " / 100", hx, 294, Palette.TEXT);
        // Score bar in 20 cells.
        float bx = hx + sw + 10;
        for (int i = 0; i < 20; i++) {
            boolean on = (i + 0.5) / 20.0 <= sc.total() / 100.0;
            draw.rect(bx + i * 6, 287, 5, 7, on ? c : Palette.WALL);
        }
        draw.text(assets.small, String.format(Locale.ROOT, "Science %d/%d   Margins %d/%d   Budget %d/%d   Reliability %d/%d",
                sc.science, Score.SCIENCE_MAX, sc.margins, Score.MARGIN_MAX, sc.budget, Score.BUDGET_MAX,
                sc.reliability, Score.RELIABILITY_MAX), hx, 278, Palette.DIM);

        float y = criteria(x, 256);

        draw.heading("WHAT MATTERED MOST", x, y, Palette.WARN);
        y -= 13;
        int tips = 0;
        for (Finding f : result.topFindings(3)) {
            if (y < 88) break;
            y -= draw.wrap(assets.small, (f.good ? "+ " : "- ") + f.text, x, y, w, f.good ? Palette.GOOD : Palette.TEXT) + 3;
            if (!f.good && f.tip != null && tips < 2 && y > 88) {
                y -= draw.wrap(assets.small, "Try: " + f.tip, x + 10, y, w - 10, Palette.ACCENT) + 5;
                tips++;
            }
        }
        if (!result.struck.isEmpty() && y > 88) {
            draw.wrap(assets.small, "Risk that struck: " + result.struck.get(0), x, y, w, Palette.BAD);
        }

        if (session.playerCount > 1) {
            draw.heading("TEAM GOALS", x, 66, Palette.DIM);
            float gx = x + 70;
            for (int i = 0; i < session.playerCount; i++) {
                Role r = Role.values()[i];
                boolean met = Boolean.TRUE.equals(result.goals.get(r));
                gx += draw.text(assets.small, r.label + (met ? " met" : " missed"), gx, 66, met ? Palette.role(r) : Palette.DIM) + 10;
            }
        }
        draw.wrap(assets.small, "Real NASA mission with a similar job: " + mission.similarMission + ".", x, 54, w, Palette.ACCENT);
    }

    /** The three success tiers with the numbers achieved. Returns the y where the text continues. */
    private float criteria(float x, float y) {
        PhotoQuality p = result.photo;
        Budgets b = result.budgets;
        boolean flew = result.launched && result.failedAt == null && p.received;
        double gsd = p.resolution > 0 ? mission.requiredGsdM / p.resolution : 0;
        String pixels = flew ? String.format(Locale.ROOT, "%.0f m", gsd) : "none";
        String received = flew ? Math.round(p.completeness * 100) + "%" : "0%";

        draw.heading("SUCCESS CRITERIA", x, y, Palette.ACCENT);
        y -= 13;
        boolean minOk = result.tier.atLeast(SuccessTier.MINIMUM);
        y = check(minOk, "Minimum: lit photo of the target, " + received + " received (need 50%), " + pixels
                + " pixels (need " + String.format(Locale.ROOT, "%.0f", mission.requiredGsdM * 2) + " m or less)", x, y);
        boolean fullOk = result.tier.atLeast(SuccessTier.FULL);
        String life = flew ? (p.lifeFactor >= 0.999 ? "lasts the mission" : String.format(Locale.ROOT, "lasts %.0f%% of it", p.lifeFactor * 100)) : "-";
        y = check(fullOk, String.format(Locale.ROOT, "Full: %s pixels (need %.0f m), %s received (need 90%%), %s",
                pixels, mission.requiredGsdM, received, life), x, y);
        y = check(result.tier == SuccessTier.EXEMPLARY, String.format(Locale.ROOT, "Exemplary: full success and score %d (need %d), %.0f%% budget left",
                result.score.total(), SuccessTier.EXEMPLARY_SCORE, b.budgetReserve() * 100), x, y);
        if (result.operationsShare < 0.999 && flew) {
            y -= draw.wrap(assets.small, String.format(Locale.ROOT, "Mission Control got %.0f%% of what this satellite could deliver.",
                    result.operationsShare * 100), x, y, 360, Palette.WARN) + 4;
        }
        return y - 6;
    }

    private float check(boolean ok, String text, float x, float y) {
        draw.chip(assets.label, ok ? "YES" : "NO", x, y, ok ? Palette.GOOD : Palette.DIM);
        float h = draw.wrap(assets.small, text, x + 36, y, 324, ok ? Palette.TEXT : Palette.DIM);
        return y - Math.max(h, 10) - 5;
    }

    @Override
    public void dispose() {
        if (photoTexture != null) {
            photoTexture.dispose();
            photoTexture = null;
        }
    }
}
