package com.tanishisherewith.entity;

import com.tanishisherewith.registry.CurtainsBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CurtainBlockEntity extends BlockEntity {
    public float swayOffset = 0.0f;
    public float prevSwayOffset = 0.0f;
    public float swayVelocity = 0.0f;
    public int length = 2;

    public CurtainBlockEntity(BlockPos pos, BlockState state) {
        super(CurtainsBlockEntities.CURTAIN_BE_TYPE, pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CurtainBlockEntity blockEntity) {
        blockEntity.prevSwayOffset = blockEntity.swayOffset;

        float wind = (float) Math.sin((level.getGameTime() + pos.getX() * 7 + pos.getZ() * 13) * 0.08f) * 0.08f;
        if (level.isRaining()) {
            wind *= 2.0f;
        }

        AABB curtainBox = new AABB(
                pos.getX(), pos.getY() - (blockEntity.length - 1), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0
        );

        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, curtainBox);
        float playerForce = 0.0f;
        for (Player player : nearbyPlayers) {
            Vec3 motion = player.getDeltaMovement();
            playerForce += (float) (motion.x + motion.z) * 0.4f;
        }

        float targetSway = wind + playerForce;
        blockEntity.swayVelocity += (targetSway - blockEntity.swayOffset) * 0.12f;
        blockEntity.swayVelocity *= 0.85f;
        blockEntity.swayOffset += blockEntity.swayVelocity;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("swayOffset", swayOffset);
        tag.putInt("Length", this.length);
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putFloat("SwayOffset", this.swayOffset);
        output.putInt("Length", this.length);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        this.swayOffset = input.getFloatOr("SwayOffset",0.0f);
        this.length = input.getIntOr("Length", 2);
    }
}
