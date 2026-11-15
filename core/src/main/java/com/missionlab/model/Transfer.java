package com.missionlab.model;

/**
 * Un vehículo que lleva el satélite de un sitio a otro: un cohete desde tierra o un remolcador en órbita.
 * Es el PESO de una arista del grafo de vuelo ({@link FlightNetwork}). Lleva varios recursos a la vez:
 * dinero (fijo + por kilo), peso máximo y horas de viaje.
 * Sustituye a la antigua clase Rocket: ahora "a qué órbita va" lo dicen las aristas del grafo.
 */
public final class Transfer {
    public final String id;
    public final String name;
    /** Nombre corto para dibujar el grafo. */
    public final String shortName;
    public final String description;
    public final double baseCostK;
    public final double costPerKgK;
    /** Peso máximo que puede llevar. Si el satélite pesa más, esta arista no sirve. */
    public final double maxKg;
    /** Horas que tarda el tramo (para la línea de tiempo del vuelo). */
    public final double hours;
    /**
     * Meses antes del lanzamiento en los que hay que entregar el satélite terminado.
     * Un viaje gratis de la NASA (CSLI) pide entregarlo antes: se gana dinero pero se pierde tiempo.
     */
    public final double deliveryMonths;

    public Transfer(String id, String name, String shortName, String description, double baseCostK, double costPerKgK,
                    double maxKg, double hours, double deliveryMonths) {
        this.id = id; this.name = name; this.shortName = shortName; this.description = description; this.baseCostK = baseCostK;
        this.costPerKgK = costPerKgK; this.maxKg = maxKg; this.hours = hours; this.deliveryMonths = deliveryMonths;
    }

    /** Precio del tramo para un satélite de este peso. */
    public double costFor(double massKg) {
        return baseCostK + costPerKgK * massKg;
    }

    public boolean carries(double massKg) {
        return massKg <= maxKg;
    }

    /** true = se paga un precio fijo sin importar el peso (o es gratis). */
    public boolean flatPrice() {
        return costPerKgK == 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
