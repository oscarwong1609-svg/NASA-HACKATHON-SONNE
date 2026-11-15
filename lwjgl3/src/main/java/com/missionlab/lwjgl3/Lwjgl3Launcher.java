package com.missionlab.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.missionlab.MissionLabGame;

/** Arranca el juego en Windows, macOS o Linux. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Mission Lab");
        config.useVsync(true);
        config.setForegroundFPS(60);
        // 1280 x 720 = 4 veces el mundo de 320 x 180, así los píxeles quedan nítidos.
        config.setWindowedMode(1280, 720);
        new Lwjgl3Application(new MissionLabGame(), config);
    }
}
