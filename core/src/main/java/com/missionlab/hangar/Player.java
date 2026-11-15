package com.missionlab.hangar;

import com.badlogic.gdx.graphics.Color;
import com.missionlab.game.Controls;
import com.missionlab.model.Role;

/** Un personaje en el hangar. */
public final class Player {
    public static final float W = 8, H = 10;
    public static final float SPEED = 64;

    public final int index;
    public final Role role;
    public final Color color;
    public final Controls.KeySet keys;
    public float x, y;
    public Item holding;
    /** Para animar los pasos. */
    public float walkTime;
    public boolean facingLeft;

    public Player(int index, Role role, Color color, Controls.KeySet keys, float x, float y) {
        this.index = index;
        this.role = role;
        this.color = color;
        this.keys = keys;
        this.x = x;
        this.y = y;
    }

    public float cx() {
        return x + W / 2;
    }

    public float cy() {
        return y + H / 2;
    }

    public float distanceTo(float px, float py) {
        float dx = cx() - px, dy = cy() - py;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public String tag() {
        return role == null ? "P" + (index + 1) : role.label.substring(0, 1);
    }
}
