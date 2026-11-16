package com.missionlab.model;

/**
 * A vehicle that carries the satellite from one place to another: a rocket from the ground or a tug in orbit.
 * It is the WEIGHT of an edge of the flight graph ({@link FlightNetwork}). It carries several resources at once:
 * money (fixed + per kilogram), maximum mass and travel time.
 * Replaces the old Rocket class: "which orbit it goes to" is now given by the graph edges.
 */
public final class Transfer {
    public final String id;
    public final String name;
    /** Short name for drawing the graph. */
    public final String shortName;
    public final String description;
    public final double baseCostK;
    public final double costPerKgK;
    /** Maximum mass it can carry. If the satellite is heavier, this edge cannot be used. */
    public final double maxKg;
    /** Hours the leg takes (for the flight timeline). */
    public final double hours;
    /**
     * Months before launch by which the finished satellite must be delivered.
     * A free NASA ride (CSLI) requires earlier delivery: you save money but lose time.
     */
    public final double deliveryMonths;

    public Transfer(String id, String name, String shortName, String description, double baseCostK, double costPerKgK,
                    double maxKg, double hours, double deliveryMonths) {
        this.id = id; this.name = name; this.shortName = shortName; this.description = description; this.baseCostK = baseCostK;
        this.costPerKgK = costPerKgK; this.maxKg = maxKg; this.hours = hours; this.deliveryMonths = deliveryMonths;
    }

    /** Price of the leg for a satellite of this mass. */
    public double costFor(double massKg) {
        return baseCostK + costPerKgK * massKg;
    }

    public boolean carries(double massKg) {
        return massKg <= maxKg;
    }

    /** true = a fixed price regardless of mass (or free). */
    public boolean flatPrice() {
        return costPerKgK == 0;
    }

    @Override
    public String toString() {
        return name;
    }
}
