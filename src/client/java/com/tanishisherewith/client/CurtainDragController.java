package com.tanishisherewith.client;

import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.entity.CurtainBlockEntity;
import com.tanishisherewith.network.CurtainDragPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CurtainDragController {
    private static BlockPos draggingMasterPos = null;
    private static int lastSentTick = 0;
    private static double lastMouseX = 0;
    private static boolean isDragging = false;
    private static double lockedDragSign = 1.0;

    private static final double DRAG_SENSITIVITY = 0.005;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CurtainDragController::onClientTick);

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (isDragging && player.getItemInHand(hand).isEmpty()) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            stopDragging();
            return;
        }

        if (client.player.isShiftKeyDown()) {
            stopDragging();
            return;
        }

        boolean isHoldingUse = client.options.keyUse.isDown();
        double currentMouseX = client.mouseHandler.xpos();

        ItemStack mainHand = client.player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean isToolOrModifier = mainHand.is(Items.SHEARS) || mainHand.is(ItemTags.WOOL) || mainHand.getItem() instanceof DyeItem;

        if (isHoldingUse && !isToolOrModifier && draggingMasterPos == null) {
            HitResult hit = client.hitResult;
            if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                BlockPos clickedPos = blockHit.getBlockPos();
                BlockState state = client.level.getBlockState(clickedPos);

                if (state.getBlock() instanceof CurtainRodBlock && state.getValue(CurtainRodBlock.HAS_CURTAIN)) {
                    BlockEntity be = client.level.getBlockEntity(clickedPos);
                    if (be instanceof CurtainBlockEntity clickedCurtain) {
                        CurtainBlockEntity masterCurtain = clickedCurtain.getMasterAnchor();
                        BlockPos masterPos = masterCurtain.getBlockPos();

                        draggingMasterPos = masterPos;
                        lastMouseX = currentMouseX;
                        isDragging = true;
                        masterCurtain.isAnimating = false;

                        BlockState masterState = client.level.getBlockState(masterPos);
                        Direction masterFacing = masterState.getValue(CurtainRodBlock.FACING);

                        lockedDragSign = -computeTrackDragSign(client, masterCurtain, masterFacing);
                    }
                }
            }
        }

        if (!isHoldingUse && draggingMasterPos != null) {
            stopDragging();
            return;
        }

        if (isDragging && draggingMasterPos != null) {
            BlockEntity be = client.level.getBlockEntity(draggingMasterPos);
            if (!(be instanceof CurtainBlockEntity curtain)) {
                stopDragging();
                return;
            }

            double rawDelta = currentMouseX - lastMouseX;
            lastMouseX = currentMouseX;

            if (Math.abs(rawDelta) > 0.05) {
                double deltaProgress = rawDelta * DRAG_SENSITIVITY * lockedDragSign;

                float newProgress = Mth.clamp((float) (curtain.openProgress + deltaProgress), 0.15f, 1.0f);

                if (Math.abs(newProgress - curtain.openProgress) > 0.0001f) {
                    curtain.openProgress = newProgress;
                    curtain.targetOpenProgress = newProgress;
                    curtain.isAnimating = false;

                    if (client.player.tickCount - lastSentTick >= 2) {
                        lastSentTick = client.player.tickCount;
                        ClientPlayNetworking.send(new CurtainDragPayload(draggingMasterPos, curtain.openProgress));
                    }
                }
            }
        }
    }

    private static double computeTrackDragSign(Minecraft client, CurtainBlockEntity masterCurtain, Direction facing) {
        Direction expDir = masterCurtain.expandRight ? facing.getClockWise() : facing.getCounterClockWise();

        float yawRad = (float) Math.toRadians(client.player.getYRot());
        double screenRightX = -Math.sin(yawRad);
        double screenRightZ = Math.cos(yawRad);

        double dot = screenRightX * expDir.getStepX() + screenRightZ * expDir.getStepZ();
        return (dot >= 0.0) ? 1.0 : -1.0;
    }

    private static void stopDragging() {
        draggingMasterPos = null;
        isDragging = false;
        lockedDragSign = 1.0;
    }
}