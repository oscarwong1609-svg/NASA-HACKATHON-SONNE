package com.missionlab.game;

import com.badlogic.gdx.graphics.Color;
import com.missionlab.model.Role;
import com.missionlab.model.Slot;

/**
 * The game's fixed palette. Using ONLY these colours is what makes the pixel art look professional.
 * If the art team picks another palette, change it here and the whole game follows.
 */
public final class Palette {
    private Palette() {}

    public static final Color DARK = c("05060b");
    public static final Color SPACE = c("0b0e1a");
    public static final Color FLOOR = c("1b2033");
    public static final Color FLOOR_LINE = c("232a42");
    public static final Color WALL = c("2b3352");
    public static final Color SHELF = c("4a3b2f");
    public static final Color METAL = c("9aa3b5");
    public static final Color BLUEPRINT = c("16365c");
    public static final Color BLUEPRINT_LINE = c("3f7fbf");
    public static final Color TEXT = c("e8ecf5");
    public static final Color DIM = c("8a93b0");
    public static final Color GOOD = c("4fd17a");
    public static final Color WARN = c("f2b33d");
    public static final Color BAD = c("e8504f");
    public static final Color ACCENT = c("5ec8f2");
    public static final Color FIRE = c("f28a3d");
    public static final Color EARTH = c("2f6fb0");
    public static final Color LAND = c("3f9a5a");
    public static final Color PANEL = c("101425");

    /** Colour of each role (and of its character). */
    public static Color role(Role r) {
        switch (r) {
            case SCIENTIST: return c("f2d14f");
            case ENGINEER: return c("e8834f");
            case LAUNCH: return ACCENT;
            default: return c("b07ef2");
        }
    }

    /** Placeholder colour for each part type, until there are sprites. */
    public static Color slot(Slot s) {
        switch (s) {
            case INSTRUMENT: return c("f2d14f");
            case POWER: return c("3d6ff2");
            case BATTERY: return c("4fd17a");
            case POINTING: return c("d14fb5");
            case RADIO: return c("b07ef2");
            default: return METAL;
        }
    }

    public static Color forRatio(double ratio) {
        if (ratio > 1.0) return BAD;
        if (ratio > 0.8) return WARN;
        return GOOD;
    }

    private static Color c(String hex) {
        return Color.valueOf(hex);
    }
}
