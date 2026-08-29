package com.tanishisherewith.block;

import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

public enum CurtainRodType implements StringRepresentable {
    STRAIGHT("straight"),
    END_LEFT("end_left"),
    END_RIGHT("end_right"),
    MIDDLE_STOPPER("middle_stopper"),
    NONE("none");

    private final String name;

    CurtainRodType(String name) {
        this.name = name;
    }

    @Override
    public @NonNull String getSerializedName() {
        return this.name;
    }
}