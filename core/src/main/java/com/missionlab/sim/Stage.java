package com.missionlab.sim;

/** The flight stages, in order. The last one (the photo) is played in Mission Control. */
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
