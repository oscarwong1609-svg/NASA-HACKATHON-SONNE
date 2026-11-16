package com.missionlab.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.missionlab.MissionLabGame;
import com.missionlab.game.Assets;
import com.missionlab.game.Draw;
import com.missionlab.game.Palette;
import com.missionlab.game.Session;

import java.util.Random;

/**
 * Base class for every screen. Draws in two layers:
 * the pixel-art world (320 x 180) and the text on top (640 x 360). Both cover the same area of the window.
 */
public abstract class BaseScreen extends ScreenAdapter {
    protected final MissionLabGame game;
    protected final Session session;
    protected final Assets assets;
    protected final SpriteBatch batch;
    protected final Draw draw;

    protected final OrthographicCamera worldCam = new OrthographicCamera();
    protected final OrthographicCamera uiCam = new OrthographicCamera();
    {
        // Both cameras cover the whole canvas; ScreenCanvas then fits it to the window size.
        worldCam.setToOrtho(false, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H);
        uiCam.setToOrtho(false, MissionLabGame.UI_W, MissionLabGame.UI_H);
    }

    /** Screen shake in pixels; decays by itself. */
    protected float shake;
    protected float time;
    private final Random shakeRng = new Random();

    protected BaseScreen(MissionLabGame game) {
        this.game = game;
        this.session = game.session;
        this.assets = game.assets;
        this.batch = game.batch;
        this.draw = new Draw(game.batch, game.assets);
    }

    protected abstract void update(float delta);

    protected abstract void drawWorld();

    protected abstract void drawUi();

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1 / 20f);
        time += delta;
        update(delta);
        if (game.getScreen() != this) return; // update() switched screens

        if (!assets.canvas.begin(Palette.DARK)) return;

        float sx = 0, sy = 0;
        if (shake > 0) {
            sx = (shakeRng.nextFloat() - 0.5f) * shake * 2;
            sy = (shakeRng.nextFloat() - 0.5f) * shake * 2;
            shake = Math.max(0, shake - delta * 10);
        }
        worldCam.position.set(MissionLabGame.WORLD_W / 2f + Math.round(sx), MissionLabGame.WORLD_H / 2f + Math.round(sy), 0);
        worldCam.update();
        batch.setProjectionMatrix(worldCam.combined);
        batch.begin();
        drawWorld();
        batch.end();

        uiCam.position.set(MissionLabGame.UI_W / 2f, MissionLabGame.UI_H / 2f, 0);
        uiCam.update();
        batch.setProjectionMatrix(uiCam.combined);
        batch.begin();
        drawUi();
        batch.end();
        assets.canvas.end(batch, Palette.SPACE);
    }

    @Override
    public void resize(int width, int height) {
        // Nothing to do: ScreenCanvas reads the window size every frame.
    }

    @Override
    public void hide() {
        dispose();
    }

    /**
     * Starfield for the menu and orbit screens: mostly faint stars, some bluish
     * and a few bright ones with a cross, all twinkling at their own pace.
     */
    protected void stars(int count, long seedValue) {
        Random r = new Random(seedValue);
        for (int i = 0; i < count; i++) {
            float x = r.nextInt(MissionLabGame.WORLD_W);
            float y = r.nextInt(MissionLabGame.WORLD_H);
            int kind = r.nextInt(24);
            float speed = 1 + r.nextFloat() * 2;
            float tw = (float) Math.sin(time * speed + i);
            float a = 0.3f + 0.35f * tw * tw;
            if (kind == 0) {
                draw.rect(x, y, 1, 1, Palette.TEXT, 0.9f);
                draw.rect(x - 1, y, 1, 1, STAR_BLUE, a * 0.6f);
                draw.rect(x + 1, y, 1, 1, STAR_BLUE, a * 0.6f);
                draw.rect(x, y - 1, 1, 1, STAR_BLUE, a * 0.6f);
                draw.rect(x, y + 1, 1, 1, STAR_BLUE, a * 0.6f);
            } else if (kind < 5) {
                draw.rect(x, y, 1, 1, STAR_BLUE, a);
            } else if (kind < 7) {
                draw.rect(x, y, 1, 1, STAR_WARM, a * 0.8f);
            } else {
                draw.rect(x, y, 1, 1, Palette.TEXT, a * 0.55f);
            }
        }
    }

    private static final com.badlogic.gdx.graphics.Color STAR_BLUE = com.badlogic.gdx.graphics.Color.valueOf("9ec9ff");
    private static final com.badlogic.gdx.graphics.Color STAR_WARM = com.badlogic.gdx.graphics.Color.valueOf("ffd9a0");

    /** The mission patch (flames for the night-time fire, ice crystal for Greenland). */
    protected static String patchSprite(com.missionlab.model.Mission m) {
        return m.night ? "patch_fire" : "patch_ice";
    }

    /** The medal for a success tier: gold = exemplary, silver = full, bronze = minimum. */
    protected static String medalSprite(com.missionlab.sim.SuccessTier t) {
        switch (t) {
            case EXEMPLARY: return "medal_gold";
            case FULL: return "medal_silver";
            case MINIMUM: return "medal_bronze";
            default: return "medal_none";
        }
    }

    /** A budget amount: $812k or $1.10M. */
    protected static String budget(double k) {
        if (Math.abs(k) >= 1000) return String.format(java.util.Locale.ROOT, "$%.2fM", k / 1000);
        return String.format(java.util.Locale.ROOT, "$%.0fk", k);
    }

    /** The five phases of a playthrough, for the progress bar. */
    protected enum Phase {
        BRIEFING("Briefing"), BUILD("Build & review"), LAUNCH("Launch"), OPERATIONS("Mission Control"), RESULTS("Results");

        final String label;

        Phase(String label) {
            this.label = label;
        }
    }

    /**
     * Playthrough progress bar: where the team is and what comes next.
     * Drawn on the text layer, with its top-left corner at (x, y).
     */
    protected void phaseBar(Phase current, float x, float y) {
        float cx = x;
        for (Phase p : Phase.values()) {
            // Silkscreen draws "&" almost like "$": the bar uses "+" instead.
            String label = (p.ordinal() + 1) + " " + p.label.toUpperCase(java.util.Locale.ROOT).replace("&", "+");
            boolean now = p == current, done = p.ordinal() < current.ordinal();
            if (now) {
                cx += draw.chip(assets.label, label, cx, y, Palette.WARN);
            } else {
                // Done: green box; pending: empty box.
                if (done) draw.rect(cx + 3, y - 5, 5, 5, Palette.GOOD);
                else draw.frame(cx + 3, y - 5, 5, 5, 1, Palette.WALL);
                cx += draw.text(assets.label, label, cx + 11, y, done ? Palette.GOOD : Palette.DIM) + 14;
            }
            if (p != Phase.RESULTS) {
                draw.rect(cx + 3, y - 3, 6, 1, done || now ? Palette.DIM : Palette.WALL);
                cx += 12;
            }
        }
    }

    /** Blinking text for "press to continue". */
    protected com.badlogic.gdx.graphics.Color blink() {
        return new com.badlogic.gdx.graphics.Color(1, 1, 1, 0.5f + 0.5f * (float) Math.sin(time * 4));
    }
}
