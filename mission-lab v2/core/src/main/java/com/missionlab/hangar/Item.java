package com.missionlab.hangar;

import com.missionlab.model.Part;

import java.util.ArrayList;
import java.util.List;

/** Una pieza física en el hangar: en su estante, en las manos de alguien, en el suelo o en el plano. */
public final class Item {
    public static final float SIZE = 12;

    public final Part part;
    public final float homeX, homeY;
    public float x, y;
    public BlueprintSlot installedIn;
    public final List<Player> carriers = new ArrayList<>(2);

    public Item(Part part, float homeX, float homeY) {
        this.part = part;
        this.homeX = homeX;
        this.homeY = homeY;
        this.x = homeX;
        this.y = homeY;
    }

    public float cx() {
        return x + SIZE / 2;
    }

    public float cy() {
        return y + SIZE / 2;
    }

    public boolean atHome() {
        return installedIn == null && carriers.isEmpty() && x == homeX && y == homeY;
    }

    public void sendHome() {
        x = homeX;
        y = homeY;
    }

    public void releaseAll() {
        for (Player p : carriers) p.holding = null;
        carriers.clear();
    }
}
