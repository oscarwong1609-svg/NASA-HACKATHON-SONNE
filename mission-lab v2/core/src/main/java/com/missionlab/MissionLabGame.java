package com.missionlab;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.missionlab.game.Assets;
import com.missionlab.game.Session;
import com.missionlab.screens.TitleScreen;

/** Punto de entrada del juego. Guarda lo que comparten todas las pantallas. */
public class MissionLabGame extends Game {
    /** Resolución del mundo en píxeles de pixel art. Se escala a la ventana. */
    public static final int WORLD_W = 320;
    public static final int WORLD_H = 180;
    /** Resolución de la capa de texto (el doble, para que se lea bien). */
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
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        batch.dispose();
        assets.dispose();
    }
}
