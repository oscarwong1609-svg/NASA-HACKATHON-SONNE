package com.missionlab.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.missionlab.MissionLabGame;

/** Starts the game on Windows, macOS or Linux. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Mission Lab v" + MissionLabGame.VERSION);
        config.useVsync(true);
        config.setForegroundFPS(60);
        // 1280 x 720 = 4 times the 320 x 180 world, so pixels stay sharp.
        config.setWindowedMode(1280, 720);
        // Free resizing, maximising and fullscreen (macOS green button, or F11 in game). Only a minimum size
        // is set, so the window can grow to any display, including Retina and 5K/6K screens.
        config.setResizable(true);
        config.setWindowSizeLimits(640, 360, -1, -1);
        // Logical mode: the window is sized in points; the game reads the back-buffer size in physical pixels
        // every frame (ScreenCanvas), so Retina displays render at full resolution.
        config.setHdpiMode(HdpiMode.Logical);
        new Lwjgl3Application(new MissionLabGame(), config);
    }
}
