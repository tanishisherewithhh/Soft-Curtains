package com.tanishisherewith.item;

import com.tanishisherewith.entity.CurtainBlockEntity;
import com.tanishisherewith.entity.CurtainStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;

public class TailoringShearsItem extends Item {
    public TailoringShearsItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof CurtainBlockEntity curtain) {
            CurtainBlockEntity anchor = curtain.getMasterAnchor();
            if (anchor != null) {
                if (!level.isClientSide()) {
                    CurtainStyle nextStyle = anchor.getStyle().next();
                    anchor.setStyle(nextStyle);
                    anchor.setChanged();
                    level.sendBlockUpdated(anchor.getBlockPos(), anchor.getBlockState(), anchor.getBlockState(), Block.UPDATE_CLIENTS);

                    ItemStack stack = context.getItemInHand();
                    if (context.getPlayer() != null) {
                        stack.hurtAndBreak(1, context.getPlayer(), EquipmentSlot.MAINHAND);
                    }
                }
                level.playSound(context.getPlayer(), pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }
}