package com.tanishisherewith.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CurtainDragPayload(BlockPos pos, float openProgress) implements CustomPacketPayload {
    public static final Type<CurtainDragPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("softcurtains", "curtain_drag"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CurtainDragPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CurtainDragPayload::pos,
            ByteBufCodecs.FLOAT, CurtainDragPayload::openProgress,
            CurtainDragPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
