package com.missionlab.sim;

import com.missionlab.model.Orbit;

import java.util.Locale;

/**
 * Un suceso de la fase de lanzamiento y primera órbita (LEOP, Launch and Early Orbit Phase), con la hora
 * a la que pasa. El simulador los mete en una cola (ver SimResult.timeline()) en orden cronológico y la
 * pantalla de lanzamiento los saca uno a uno. Las fotos y la bajada de datos se juegan después, en Mission Control.
 */
public final class FlightEvent {
    public enum Status { OK, WARN, FAILED }

    public enum Kind {
        CANCELLED(null),
        LIFTOFF(Stage.LIFTOFF),
        /** Un tramo intermedio de la ruta: un vehículo deja el satélite y otro lo recoge. */
        TRANSFER(null),
        ORBIT(Stage.ORBIT),
        PANELS(null),
        /** Primer contacto por radio: termina la fase de lanzamiento y empieza Mission Control. */
        CONTACT(Stage.CONTACT);

        /** La etapa del vuelo que este suceso decide, o null si es solo informativo. */
        public final Stage stage;

        Kind(Stage stage) {
            this.stage = stage;
        }
    }

    public final Kind kind;
    public final Status status;
    /** Horas desde el despegue. */
    public final double hours;
    public final String text;
    /** Dónde está el satélite después de este suceso (para dibujarlo), o null si no cambia. */
    public final Orbit orbit;

    public FlightEvent(Kind kind, Status status, double hours, String text, Orbit orbit) {
        this.kind = kind;
        this.status = status;
        this.hours = hours;
        this.text = text;
        this.orbit = orbit;
    }

    public boolean failed() {
        return status == Status.FAILED;
    }

    /** Reloj de la misión: "T-0", "T+1.5 h" o "Day 31". */
    public String clock() {
        if (kind == Kind.CANCELLED) return "--";
        if (hours <= 0) return "T-0";
        if (hours < 48) return String.format(Locale.ROOT, "T+%.1f h", hours);
        return String.format(Locale.ROOT, "Day %d", (int) (hours / 24) + 1);
    }

    @Override
    public String toString() {
        return clock() + " " + kind + " " + status + ": " + text;
    }
}
