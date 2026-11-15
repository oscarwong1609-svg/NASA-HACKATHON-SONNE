package com.missionlab.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

/** Las teclas de cada jugador. Cuatro jugadores en un teclado van justos: mejor con dos mandos. */
public final class Controls {
    private Controls() {}

    public static final class KeySet {
        public final int up, down, left, right, action;
        public final String label;

        KeySet(int up, int down, int left, int right, int action, String label) {
            this.up = up; this.down = down; this.left = left; this.right = right; this.action = action;
            this.label = label;
        }

        public boolean actionPressed() {
            return Gdx.input.isKeyJustPressed(action);
        }

        /** true mientras se mantiene pulsada la tecla de acción (para avanzar el tiempo más rápido). */
        public boolean actionHeld() {
            return Gdx.input.isKeyPressed(action);
        }

        public float dx() {
            return (Gdx.input.isKeyPressed(right) ? 1 : 0) - (Gdx.input.isKeyPressed(left) ? 1 : 0);
        }

        public float dy() {
            return (Gdx.input.isKeyPressed(up) ? 1 : 0) - (Gdx.input.isKeyPressed(down) ? 1 : 0);
        }

        public int leftRightPressed() {
            if (Gdx.input.isKeyJustPressed(left)) return -1;
            if (Gdx.input.isKeyJustPressed(right)) return 1;
            return 0;
        }
    }

    public static final KeySet[] PLAYERS = {
            new KeySet(Keys.W, Keys.S, Keys.A, Keys.D, Keys.E, "W A S D to move, E to act"),
            new KeySet(Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT, Keys.ENTER, "Arrows to move, Enter to act"),
            new KeySet(Keys.I, Keys.K, Keys.J, Keys.L, Keys.O, "I J K L to move, O to act"),
            new KeySet(Keys.NUMPAD_8, Keys.NUMPAD_5, Keys.NUMPAD_4, Keys.NUMPAD_6, Keys.NUMPAD_0,
                    "Numpad 8 4 5 6 to move, Numpad 0 to act"),
    };

    /** En los menús, cualquier tecla de acción (o Espacio) confirma. */
    public static boolean confirm() {
        if (Gdx.input.isKeyJustPressed(Keys.SPACE)) return true;
        for (KeySet k : PLAYERS) if (k.actionPressed()) return true;
        return false;
    }

    public static boolean back() {
        return Gdx.input.isKeyJustPressed(Keys.ESCAPE);
    }

    /** -1, 0 o 1 con las flechas o A/D en los menús. */
    public static int menuHorizontal() {
        if (Gdx.input.isKeyJustPressed(Keys.LEFT) || Gdx.input.isKeyJustPressed(Keys.A)) return -1;
        if (Gdx.input.isKeyJustPressed(Keys.RIGHT) || Gdx.input.isKeyJustPressed(Keys.D)) return 1;
        return 0;
    }

    public static int menuVertical() {
        if (Gdx.input.isKeyJustPressed(Keys.UP) || Gdx.input.isKeyJustPressed(Keys.W)) return 1;
        if (Gdx.input.isKeyJustPressed(Keys.DOWN) || Gdx.input.isKeyJustPressed(Keys.S)) return -1;
        return 0;
    }
}
