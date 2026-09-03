package com.tanishisherewith.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum CurtainStyle implements StringRepresentable {
    DRAPES("drapes"),
    BLINDS("blinds"),
    SHUTTERS("shutters"),
    ROLLER("roller");

    public static final Codec<CurtainStyle> CODEC = StringRepresentable.fromEnum(CurtainStyle::values);
    public static final StreamCodec<ByteBuf, CurtainStyle> STREAM_CODEC = ByteBufCodecs.idMapper(
            id -> CurtainStyle.values()[id],
            CurtainStyle::ordinal
    );

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