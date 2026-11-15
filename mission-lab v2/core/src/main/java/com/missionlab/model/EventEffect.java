package com.missionlab.model;

/** Qué pasa si el equipo acepta el riesgo de un problema de construcción y el riesgo ocurre. */
public enum EventEffect {
    /** La antena falla: nunca hay contacto con el satélite. */
    NO_CONTACT,
    /** La cámara queda desenfocada: la nitidez baja a la mitad. */
    BLUR,
    /** El ordenador se reinicia a menudo: llega la mitad de los datos. */
    RESETS,
    /** Los paneles no se abren del todo: la mitad de la energía. */
    PANEL_JAM,
    /** No es un riesgo: aceptar cuesta meses en vez de dinero. */
    DELAY
}
