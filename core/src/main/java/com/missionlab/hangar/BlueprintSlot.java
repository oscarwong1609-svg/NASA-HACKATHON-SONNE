package com.missionlab.hangar;

import com.missionlab.model.Slot;

/** Un hueco del plano en el suelo del hangar. */
public final class BlueprintSlot {
    public static final float W = 30, H = 24;

    public final Slot slot;
    public final float x, y;
    public Item installed;

    public BlueprintSlot(Slot slot, float x, float y) {
        this.slot = slot;
        this.x = x;
        this.y = y;
    }

    public float cx() {
        return x + W / 2;
    }

    public float cy() {
        return y + H / 2;
    }
}
