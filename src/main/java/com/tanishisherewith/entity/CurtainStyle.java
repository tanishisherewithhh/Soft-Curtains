package com.tanishisherewith.entity;

import net.minecraft.util.StringRepresentable;

public enum CurtainStyle implements StringRepresentable {
    DRAPES("drapes"),
    BLINDS("blinds"),
    SHUTTERS("shutters"),
    ROLLER("roller");

    private final String name;

    CurtainStyle(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public CurtainStyle next() {
        CurtainStyle[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }
}