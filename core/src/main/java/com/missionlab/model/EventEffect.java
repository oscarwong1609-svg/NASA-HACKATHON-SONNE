package com.missionlab.model;

/** What happens if the team accepts the risk of a build problem and the risk materialises. */
public enum EventEffect {
    /** The antenna fails: there is never contact with the satellite. */
    NO_CONTACT,
    /** The camera ends up out of focus: sharpness drops by half. */
    BLUR,
    /** The computer reboots often: half of the data arrives. */
    RESETS,
    /** The panels do not fully deploy: half the power. */
    PANEL_JAM,
    /** Not a risk: accepting costs months instead of money. */
    DELAY
}
