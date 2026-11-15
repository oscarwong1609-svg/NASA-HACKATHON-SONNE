package com.missionlab.model;

/**
 * Un problema que aparece mientras se construye, como en las pruebas reales de CubeSat 101.
 * El equipo elige: arreglarlo (cuesta dinero y meses) o arriesgarse.
 */
public final class BuildEvent {
    public final String id;
    public final String title;
    public final String text;
    public final String fixLabel;
    public final String riskLabel;
    public final double fixCostK;
    public final double fixMonths;
    public final EventEffect effect;
    /** Probabilidad de que el riesgo ocurra si no se arregla (0..1). */
    public final double chance;
    /** Solo para DELAY: meses perdidos si no se paga. */
    public final double delayMonths;
    /** Si no es null, el problema solo aparece cuando una de estas piezas está instalada (ids separados por |). */
    public final String requiresPartId;

    public BuildEvent(String id, String title, String text, String fixLabel, String riskLabel,
                      double fixCostK, double fixMonths, EventEffect effect, double chance,
                      double delayMonths, String requiresPartId) {
        this.id = id; this.title = title; this.text = text; this.fixLabel = fixLabel; this.riskLabel = riskLabel;
        this.fixCostK = fixCostK; this.fixMonths = fixMonths; this.effect = effect; this.chance = chance;
        this.delayMonths = delayMonths; this.requiresPartId = requiresPartId;
    }

    public boolean canHappen(Design design) {
        if (requiresPartId == null) return true;
        for (Part p : design.parts()) {
            for (String id : requiresPartId.split("\\|")) if (p.id.equals(id)) return true;
        }
        return false;
    }
}
