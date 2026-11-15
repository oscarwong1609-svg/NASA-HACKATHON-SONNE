package com.missionlab.sim;

/** Una explicación para el informe final: qué pasó, qué probar la próxima vez y cuánto pesó. */
public final class Finding {
    public final String text;
    public final String tip;
    /** Puntos perdidos aproximados; sirve para ordenar las causas de mayor a menor. */
    public final double impact;
    public final boolean good;

    public Finding(String text, String tip, double impact, boolean good) {
        this.text = text;
        this.tip = tip;
        this.impact = impact;
        this.good = good;
    }

    public static Finding problem(String text, String tip, double impact) {
        return new Finding(text, tip, impact, false);
    }

    public static Finding good(String text) {
        return new Finding(text, null, 0, true);
    }
}
