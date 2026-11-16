package com.missionlab.model;

/** The team's four roles. Each one owns part of the hangar. */
public enum Role {
    SCIENTIST("Scientist", "Get the detail the science needs"),
    ENGINEER("Engineer", "Keep 10% spare weight, power and battery"),
    LAUNCH("Launch lead", "Reach the target by the cheapest route"),
    GROUND("Ground control", "Bring down 90% of the picture, keep 10% of the budget");

    public final String label;
    public final String goal;

    Role(String label, String goal) {
        this.label = label;
        this.goal = goal;
    }
}
