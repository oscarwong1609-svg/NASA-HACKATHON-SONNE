package com.missionlab.sim;

/** Cómo sale la foto final. La pantalla de resultado usa estos números para dibujarla. */
public final class PhotoQuality {
    /** true si el satélite llegó a tomar y enviar la foto. */
    public final boolean received;
    /** 0..1: 1 = tanto detalle como piden los científicos; 0.5 = píxeles el doble de grandes. */
    public final double resolution;
    /** 0..1: parte de la foto que llegó a Tierra (el resto sale en negro). */
    public final double completeness;
    /** false = la foto sale negra (cámara normal de noche). */
    public final boolean lit;
    /** false = la órbita no pasa sobre el objetivo y la foto muestra el océano. */
    public final boolean onTarget;
    /** 0..1: parte de la misión que el satélite sobrevive en órbita. */
    public final double lifeFactor;

    public PhotoQuality(boolean received, double resolution, double completeness, boolean lit, boolean onTarget, double lifeFactor) {
        this.received = received;
        this.resolution = resolution;
        this.completeness = completeness;
        this.lit = lit;
        this.onTarget = onTarget;
        this.lifeFactor = lifeFactor;
    }

    public static PhotoQuality none() {
        return new PhotoQuality(false, 0, 0, false, false, 0);
    }

    public PhotoQuality withCompleteness(double c) {
        return new PhotoQuality(received, resolution, c, lit, onTarget, lifeFactor);
    }
}
