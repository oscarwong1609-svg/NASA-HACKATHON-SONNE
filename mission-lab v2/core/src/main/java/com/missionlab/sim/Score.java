package com.missionlab.sim;

/**
 * Puntuación sobre 100, separada en partes para que el jugador vea de dónde sale.
 *
 *   Ciencia      50  la foto: resolución × parte recibida × luz × objetivo × vida en órbita
 *   Márgenes     20  5 por cada margen de al menos un 10%: peso, energía, batería y calendario
 *   Presupuesto  15  completo si sobra al menos un 10% del dinero (la reserva que guarda NASA)
 *   Fiabilidad   15  probabilidad de que todas las piezas funcionen, menos 3 por cada riesgo aceptado
 */
public final class Score {
    public static final int SCIENCE_MAX = 50;
    public static final int MARGIN_MAX = 20;
    public static final int BUDGET_MAX = 15;
    public static final int RELIABILITY_MAX = 15;

    public final int science;
    public final int margins;
    public final int budget;
    public final int reliability;

    public Score(double science, double margins, double budget, double reliability) {
        this.science = (int) Math.round(science);
        this.margins = (int) Math.round(margins);
        this.budget = (int) Math.round(budget);
        this.reliability = (int) Math.round(reliability);
    }

    public int total() {
        return science + margins + budget + reliability;
    }

    public static Score zero() {
        return new Score(0, 0, 0, 0);
    }

    /**
     * Puntos (0..1) de un margen: completo a partir del 10%, proporcional entre 0 y 10%, nada si falta.
     * Sobrar mucho no se castiga: ya cuesta peso y dinero por sí solo.
     */
    static double marginPoints(double margin) {
        if (margin < 0) return 0;
        return Math.min(1, margin / 0.10);
    }
}
