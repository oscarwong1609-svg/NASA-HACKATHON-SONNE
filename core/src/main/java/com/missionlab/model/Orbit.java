package com.missionlab.model;

/**
 * Una órbita circular que el equipo elige en el mapa de órbitas.
 * Solo guarda los datos de partida; lo demás (periodo, eclipse, traza en el suelo, contactos)
 * lo calcula sim.OrbitMechanics con física de verdad, simplificada.
 */
public final class Orbit {
    public final String id;
    public final String name;
    public final String description;
    public final double altitudeKm;
    /** Inclinación en grados. Más de 90 = retrógrada, como las órbitas heliosíncronas (~98°). */
    public final double inclinationDeg;
    /**
     * Hora solar local a la que cruza el ecuador hacia el norte (nodo ascendente).
     * En una órbita heliosíncrona es fija todo el año; por eso pasa siempre a la misma hora por cada sitio.
     */
    public final double ltanHours;
    /** Años antes de que el aire lo frene y caiga, sin ayuda. */
    public final double lifetimeYears;
    /** La latitud más alta (norte o sur) sobre la que pasa. */
    public final double maxLatitude;
    /** true si es heliosíncrona (para los textos). */
    public final boolean sunSynchronous;

    public Orbit(String id, String name, String description, double altitudeKm, double inclinationDeg,
                 double ltanHours, double lifetimeYears, boolean sunSynchronous) {
        this.id = id; this.name = name; this.description = description; this.altitudeKm = altitudeKm;
        this.inclinationDeg = inclinationDeg; this.ltanHours = ltanHours; this.lifetimeYears = lifetimeYears;
        this.sunSynchronous = sunSynchronous;
        this.maxLatitude = inclinationDeg <= 90 ? inclinationDeg : 180 - inclinationDeg;
    }

    public boolean passesOver(double latitude) {
        return Math.abs(latitude) <= maxLatitude;
    }

    @Override
    public String toString() {
        return name;
    }
}
