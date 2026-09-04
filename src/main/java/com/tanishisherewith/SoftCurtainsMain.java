package com.tanishisherewith;

import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.block.RodMaterial;
import com.tanishisherewith.entity.CurtainBlockEntity;
import com.tanishisherewith.entity.CurtainStyle;
import com.tanishisherewith.network.CurtainDragPayload;
import com.tanishisherewith.registry.CurtainsBlockEntities;
import com.tanishisherewith.registry.CurtainsBlocks;
import com.tanishisherewith.registry.CurtainsComponents;
import com.tanishisherewith.registry.CurtainsItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoftCurtainsMain implements ModInitializer {
    public static final String MOD_ID = "softcurtains";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CurtainsBlocks.register();
        CurtainsItems.register();
        CurtainsComponents.register();
        CurtainsBlockEntities.register();

        PayloadTypeRegistry.serverboundPlay().register(CurtainDragPayload.TYPE, CurtainDragPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CurtainDragPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    Level level = context.player().level();
                    BlockPos targetPos = payload.pos();

                    if (!level.isLoaded(targetPos)) return;

                    BlockEntity rawBe = level.getBlockEntity(targetPos);
                    if (rawBe instanceof CurtainBlockEntity initialBe) {
                        CurtainBlockEntity anchorCurtain = initialBe.getMasterAnchor();
                        BlockPos anchorPos = anchorCurtain.getBlockPos();
                        BlockState anchorState = level.getBlockState(anchorPos);

                        if (anchorState.getBlock() instanceof CurtainRodBlock) {
                            Direction facing = anchorState.getValue(CurtainRodBlock.FACING);
                            Direction stepDir = anchorCurtain.expandRight ? facing.getClockWise() : facing.getCounterClockWise();
                            int span = anchorCurtain.span;
                            float globalProgress = Mth.clamp(payload.openProgress(), 0.15f, 1.0f);

                            anchorCurtain.openProgress = globalProgress;
                            anchorCurtain.setChanged();
                            level.sendBlockUpdated(anchorPos, anchorState, anchorState, Block.UPDATE_CLIENTS);

                            for (int i = 1; i < span; i++) {
                                BlockPos slicePos = anchorPos.relative(stepDir, i);
                                BlockEntity sliceBe = level.getBlockEntity(slicePos);
                                BlockState sliceState = level.getBlockState(slicePos);

                                if (sliceBe instanceof CurtainBlockEntity slice) {
                                    slice.openProgress = globalProgress;
                                    slice.setChanged();
                                    level.sendBlockUpdated(slicePos, sliceState, sliceState, Block.UPDATE_CLIENTS);
                                }
                            }
                        }
                    }
                }));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(CurtainsItems.TAILORING_SHEARS);
        });

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            for (RodMaterial material : RodMaterial.values()) {
                entries.accept(CurtainsItems.ROD_ITEMS.get(material));
            }

            for (CurtainStyle style : CurtainStyle.values()) {
                for (Item curtainItem : CurtainsItems.CURTAINS.values()) {
                    ItemStack stack = new ItemStack(curtainItem);
                    stack.set(CurtainsComponents.CURTAIN_STYLE.get(), style);
                    entries.accept(stack);
                }
            }
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}