package com.missionlab.model;

/** The six blueprint slots. Each part fits exactly one. */
public enum Slot {
    INSTRUMENT("Instrument", Role.SCIENTIST),
    POWER("Solar panels", Role.ENGINEER),
    BATTERY("Battery", Role.ENGINEER),
    POINTING("Pointing", Role.ENGINEER),
    RADIO("Radio", Role.GROUND),
    STRUCTURE("Frame", Role.ENGINEER);

    public final String label;
    public final Role owner;

    Slot(String label, Role owner) {
        this.label = label;
        this.owner = owner;
    }
}
