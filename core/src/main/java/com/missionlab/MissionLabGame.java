package com.missionlab;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.missionlab.game.Assets;
import com.missionlab.game.Session;
import com.missionlab.screens.TitleScreen;

/** Game entry point. Holds what every screen shares. */
public class MissionLabGame extends Game {
    /** Release version, shown in the window title and on the title screen. */
    public static final String VERSION = "4.0";
    /** World resolution in pixel-art pixels. Scaled up to the window. */
    public static final int WORLD_W = 320;
    public static final int WORLD_H = 180;
    /** Resolution of the text layer (twice the world, so text stays readable). */
    public static final int UI_W = 640;
    public static final int UI_H = 360;

    public SpriteBatch batch;
    public Assets assets;
    public Session session;

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets = new Assets();
        session = new Session();
        setScreen(new TitleScreen(this));
    }

    @Override
    public void render() {
        // F11 toggles fullscreen on every desktop (on macOS the green window button also works).
        if (Gdx.input != null && Gdx.input.isKeyJustPressed(Keys.F11)) toggleFullscreen();
        super.render();
    }

    /** Switches between fullscreen on the current monitor and the default 1280 x 720 window. */
    public static void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) Gdx.graphics.setWindowedMode(1280, 720);
        else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        batch.dispose();
        assets.dispose();
    }
}
