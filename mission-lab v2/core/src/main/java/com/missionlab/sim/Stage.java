package com.missionlab.sim;

/** Las etapas del vuelo, en orden. La última (la foto) se juega en Mission Control. */
public enum Stage {
    LIFTOFF("Liftoff"),
    ORBIT("Orbit reached"),
    CONTACT("First radio contact"),
    PHOTO("Science operations");

    public final String label;

    Stage(String label) {
        this.label = label;
    }
}
