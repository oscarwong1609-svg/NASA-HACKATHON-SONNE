package com.missionlab.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.missionlab.MissionLabGame;
import com.missionlab.game.Assets;
import com.missionlab.game.Draw;
import com.missionlab.game.Palette;
import com.missionlab.game.Session;

import java.util.Random;

/**
 * Base de todas las pantallas. Dibuja en dos capas:
 * el mundo pixel art (320 x 180) y encima el texto (640 x 360). Las dos ocupan la misma zona de la ventana.
 */
public abstract class BaseScreen extends ScreenAdapter {
    protected final MissionLabGame game;
    protected final Session session;
    protected final Assets assets;
    protected final SpriteBatch batch;
    protected final Draw draw;

    protected final OrthographicCamera worldCam = new OrthographicCamera();
    protected final OrthographicCamera uiCam = new OrthographicCamera();
    protected final FitViewport worldView = new FitViewport(MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, worldCam);
    protected final FitViewport uiView = new FitViewport(MissionLabGame.UI_W, MissionLabGame.UI_H, uiCam);

    /** Temblor de pantalla en píxeles; baja solo. */
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
        if (game.getScreen() != this) return; // update() cambió de pantalla

        ScreenUtils.clear(Palette.DARK.r, Palette.DARK.g, Palette.DARK.b, 1);

        float sx = 0, sy = 0;
        if (shake > 0) {
            sx = (shakeRng.nextFloat() - 0.5f) * shake * 2;
            sy = (shakeRng.nextFloat() - 0.5f) * shake * 2;
            shake = Math.max(0, shake - delta * 10);
        }
        worldView.apply();
        worldCam.position.set(MissionLabGame.WORLD_W / 2f + Math.round(sx), MissionLabGame.WORLD_H / 2f + Math.round(sy), 0);
        worldCam.update();
        batch.setProjectionMatrix(worldCam.combined);
        batch.begin();
        drawWorld();
        batch.end();

        uiView.apply();
        uiCam.position.set(MissionLabGame.UI_W / 2f, MissionLabGame.UI_H / 2f, 0);
        uiCam.update();
        batch.setProjectionMatrix(uiCam.combined);
        batch.begin();
        drawUi();
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        worldView.update(width, height, true);
        uiView.update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
    }

    /** Fondo de estrellas para las pantallas de menú y de órbita. */
    protected void stars(int count, long seedValue) {
        Random r = new Random(seedValue);
        for (int i = 0; i < count; i++) {
            float x = r.nextInt(MissionLabGame.WORLD_W);
            float y = r.nextInt(MissionLabGame.WORLD_H);
            float tw = (float) Math.sin(time * (1 + r.nextFloat() * 2) + i);
            draw.rect(x, y, 1, 1, Palette.TEXT, 0.35f + 0.35f * tw * tw);
        }
    }

    /** Una cantidad de presupuesto: $812k o $1.10M. */
    protected static String budget(double k) {
        if (Math.abs(k) >= 1000) return String.format(java.util.Locale.ROOT, "$%.2fM", k / 1000);
        return String.format(java.util.Locale.ROOT, "$%.0fk", k);
    }

    /** Las cinco fases de una partida, para la barra de progreso. */
    protected enum Phase {
        BRIEFING("Briefing"), BUILD("Build & review"), LAUNCH("Launch"), OPERATIONS("Mission Control"), RESULTS("Results");

        final String label;

        Phase(String label) {
            this.label = label;
        }
    }

    /**
     * Barra de progreso de la partida: dónde está el equipo y qué viene después.
     * Se dibuja en la capa de texto, con la esquina superior izquierda en (x, y).
     */
    protected void phaseBar(Phase current, float x, float y) {
        float cx = x;
        for (Phase p : Phase.values()) {
            String label = (p.ordinal() + 1) + " " + p.label.toUpperCase(java.util.Locale.ROOT);
            boolean now = p == current, done = p.ordinal() < current.ordinal();
            com.badlogic.gdx.graphics.Color c = now ? Palette.WARN : done ? Palette.GOOD : Palette.DIM;
            if (now) cx += draw.chip(assets.small, label, cx, y, c);
            else cx += draw.text(assets.small, label, cx + 4, y, c) + 8;
            if (p != Phase.RESULTS) cx += draw.text(assets.small, ">", cx + 3, y, Palette.WALL) + 8;
        }
    }

    /** Texto parpadeante para "pulsa para seguir". */
    protected com.badlogic.gdx.graphics.Color blink() {
        return new com.badlogic.gdx.graphics.Color(1, 1, 1, 0.5f + 0.5f * (float) Math.sin(time * 4));
    }
}
