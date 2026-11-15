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
 * El informe final: la foto con la calidad conseguida, la medalla, los criterios de éxito con los números reales,
 * la puntuación por partes y qué mejorar.
 * Borrosa = poca resolución. Cortada = no llegaron todos los datos. Negra = de noche sin cámara térmica.
 * Océano = la órbita no pasó por el objetivo.
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
        // La borrosidad depende de cuánto detalle se consiguió frente a lo que pidieron los científicos.
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
        if (time < 0.6f) return; // evita saltarse la pantalla sin querer
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
        draw.rect(0, 0, 640, 14, Palette.PANEL);
        draw.text(assets.small, "ACTION: back to the hangar to improve this design        Esc: mission select", 12, 11, Palette.TEXT);
    }

    private void drawPhoto() {
        float x = PHOTO_X, y = PHOTO_Y, s = PHOTO_SIZE;
        draw.rect(x - 3, y - 3, s + 6, s + 6, Palette.METAL);
        draw.rect(x, y, s, s, Palette.DARK);
        PhotoQuality q = result.photo;
        if (photoRegion == null) {
            draw.textCenter(assets.font, "NO PHOTO", x + s / 2, y + s / 2 + 6, Palette.BAD);
        } else {
            float h = s * visibleShare;
            if (q.lit) batch.setColor(Color.WHITE);
            else batch.setColor(0.07f, 0.07f, 0.10f, 1);
            batch.draw(photoRegion, x, y + s - h, s, h);
            batch.setColor(Color.WHITE);
            if (visibleShare < 0.98f) {
                draw.textCenter(assets.small, "NO DATA RECEIVED", x + s / 2, y + (s - h) / 2 + 5, Palette.BAD);
            }
            if (!q.lit) draw.textCenter(assets.small, "TOO DARK TO SEE", x + s / 2, y + s - 14, Palette.WARN);
            if (!q.onTarget) draw.textCenter(assets.small, "WRONG PLACE: OPEN OCEAN", x + s / 2, y + s - 28, Palette.WARN);
        }
        draw.wrap(assets.small, mission.targetName + ". " + mission.photoCredit, x, y - 8, s, Palette.DIM);
    }

    private void drawSummary() {
        float x = 270, w = 360;
        Score sc = result.score;
        SuccessTier tier = result.tier;
        String head = !result.launched ? "LAUNCH CANCELLED" : result.failedAt != null ? "MISSION LOST" : tier.label.toUpperCase(Locale.ROOT);
        Color c = tier == SuccessTier.NONE ? Palette.BAD : TitleScreen.medalColor(tier);
        float mw = draw.chip(assets.small, tier.medal.toUpperCase(Locale.ROOT), x, 330, c);
        draw.text(assets.small, "MISSION " + mission.number + ": " + mission.title, x + mw + 8, 330, Palette.DIM);
        draw.text(assets.big, head, x, 316, c);
        draw.text(assets.font, "Score " + sc.total() + " / 100", x, 282, Palette.TEXT);
        draw.text(assets.small, String.format(Locale.ROOT, "Science %d/%d   Margins %d/%d   Budget %d/%d   Reliability %d/%d",
                sc.science, Score.SCIENCE_MAX, sc.margins, Score.MARGIN_MAX, sc.budget, Score.BUDGET_MAX,
                sc.reliability, Score.RELIABILITY_MAX), x, 266, Palette.DIM);

        float y = criteria(x, 250);

        draw.text(assets.small, "WHAT MATTERED MOST", x, y, Palette.WARN);
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
            draw.text(assets.small, "TEAM GOALS", x, 66, Palette.DIM);
            float gx = x + 70;
            for (int i = 0; i < session.playerCount; i++) {
                Role r = Role.values()[i];
                boolean met = Boolean.TRUE.equals(result.goals.get(r));
                gx += draw.text(assets.small, r.label + (met ? " met" : " missed"), gx, 66, met ? Palette.role(r) : Palette.DIM) + 10;
            }
        }
        draw.wrap(assets.small, "Real NASA mission with a similar job: " + mission.similarMission + ".", x, 54, w, Palette.ACCENT);
    }

    /** Los tres niveles de éxito con los números conseguidos. Devuelve la y donde sigue el texto. */
    private float criteria(float x, float y) {
        PhotoQuality p = result.photo;
        Budgets b = result.budgets;
        boolean flew = result.launched && result.failedAt == null && p.received;
        double gsd = p.resolution > 0 ? mission.requiredGsdM / p.resolution : 0;
        String pixels = flew ? String.format(Locale.ROOT, "%.0f m", gsd) : "none";
        String received = flew ? Math.round(p.completeness * 100) + "%" : "0%";

        draw.text(assets.small, "SUCCESS CRITERIA", x, y, Palette.ACCENT);
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
        draw.chip(assets.small, ok ? "YES" : "NO", x, y, ok ? Palette.GOOD : Palette.DIM);
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
