package com.tanishisherewith.client;

import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.entity.CurtainBlockEntity;
import com.tanishisherewith.entity.CurtainStyle;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CurtainDragController {
    private static BlockPos draggingMasterPos = null;
    private static int lastSentTick = 0;
    private static double lastMouseX = 0;
    private static boolean isDragging = false;
    private static double lockedDragSign = 1.0;
    private static boolean wasHoldingUse = false;
    private static float lastSoundProgress = 1.0f;

    private static final double DRAG_SENSITIVITY = 0.0035;
    private static final float SOUND_STEP_INTERVAL = 0.20f;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CurtainDragController::onClientTick);

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (isDragging && player.getItemInHand(hand).isEmpty()) {
                return InteractionResult.FAIL;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player == player && level.isClientSide()) {
                CurtainBlockEntity hitCurtain = raycastCurtainCloth(client, client.player.entityInteractionRange());
                if (hitCurtain != null && player.getItemInHand(hand).isEmpty()) {
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            stopDragging();
            wasHoldingUse = false;
            return;
        }

        boolean isHoldingUse = client.options.keyUse.isDown();
        boolean justPressedUse = isHoldingUse && !wasHoldingUse;
        wasHoldingUse = isHoldingUse;

        double currentMouseX = client.mouseHandler.xpos();

        ItemStack mainHand = client.player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean isToolOrModifier = mainHand.is(Items.SHEARS)
                || mainHand.is(ItemTags.WOOL)
                || mainHand.getItem() instanceof DyeItem;
        
        if (justPressedUse && client.player.isShiftKeyDown() && !isToolOrModifier) {
            CurtainBlockEntity target = raycastCurtainCloth(client, client.player.entityInteractionRange());
            if (target != null) {
                CurtainBlockEntity master = target.getMasterAnchor();
                master.toggle();
                ClientPlayNetworking.send(new CurtainDragPayload(master.getBlockPos(), master.targetOpenProgress));

                client.player.swing(InteractionHand.MAIN_HAND);
                stopDragging();
                return;
            }
        }

        if (client.player.isShiftKeyDown()) {
            stopDragging();
            return;
        }
        
        if (isHoldingUse && !isToolOrModifier && draggingMasterPos == null) {
            CurtainBlockEntity foundCurtain = null;

            HitResult hit = client.hitResult;
            if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                BlockPos clickedPos = blockHit.getBlockPos();
                BlockState state = client.level.getBlockState(clickedPos);

                if (state.getBlock() instanceof CurtainRodBlock && state.getValue(CurtainRodBlock.HAS_CURTAIN)) {
                    BlockEntity be = client.level.getBlockEntity(clickedPos);
                    if (be instanceof CurtainBlockEntity clickedCurtain) {
                        foundCurtain = clickedCurtain.getMasterAnchor();
                    }
                }
            }

            if (foundCurtain == null) {
                foundCurtain = raycastCurtainCloth(client, client.player.entityInteractionRange());
            }

            if (foundCurtain != null) {
                CurtainBlockEntity master = foundCurtain.getMasterAnchor();
                BlockPos masterPos = master.getBlockPos();

                draggingMasterPos = masterPos;
                lastMouseX = currentMouseX;
                isDragging = true;
                master.isAnimating = false;
                lastSoundProgress = master.openProgress;

                
                master.playCurtainSound(master.openProgress < 0.5f);

                BlockState masterState = client.level.getBlockState(masterPos);
                Direction masterFacing = masterState.getValue(CurtainRodBlock.FACING);
                lockedDragSign = computeTrackDragSign(client, master, masterFacing);
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

            if (Math.abs(rawDelta) > 0.001) {
                double deltaProgress = rawDelta * DRAG_SENSITIVITY * lockedDragSign;
                float newProgress = Mth.clamp((float) (curtain.openProgress + deltaProgress), 0.15f, 1.0f);

                curtain.openProgress = newProgress;
                curtain.targetOpenProgress = newProgress;
                curtain.isAnimating = false;

                
                if (Math.abs(newProgress - lastSoundProgress) >= SOUND_STEP_INTERVAL) {
                    boolean opening = newProgress > lastSoundProgress;
                    curtain.playCurtainSound(opening);
                    lastSoundProgress = newProgress;
                }

                if (client.player.tickCount - lastSentTick >= 1) {
                    lastSentTick = client.player.tickCount;
                    ClientPlayNetworking.send(new CurtainDragPayload(draggingMasterPos, curtain.openProgress));
                }
            }
        }
    }

    private static CurtainBlockEntity raycastCurtainCloth(Minecraft client, double reachDistance) {
        if (client.level == null || client.player == null) {
            return null;
        }

        Vec3 eyePos = client.player.getEyePosition(1.0f);
        Vec3 viewVec = client.player.getViewVector(1.0f);
        Vec3 reachEnd = eyePos.add(viewVec.scale(reachDistance));

        double maxAllowedDistSq = reachDistance * reachDistance;
        if (client.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState hitState = client.level.getBlockState(blockHit.getBlockPos());
            if (!(hitState.getBlock() instanceof CurtainRodBlock)) {
                maxAllowedDistSq = eyePos.distanceToSqr(blockHit.getLocation());
            }
        }

        CurtainBlockEntity closestCurtain = null;
        double closestDistSq = maxAllowedDistSq;

        int steps = (int) Math.ceil(reachDistance * 2.0);
        Set<Long> checkedColumns = new HashSet<>();

        for (int i = 0; i <= steps; i++) {
            double fraction = (double) i / steps;
            Vec3 samplePoint = eyePos.lerp(reachEnd, fraction);

            int x = Mth.floor(samplePoint.x);
            int z = Mth.floor(samplePoint.z);
            long columnKey = (((long) x) << 32) | (z & 0xFFFFFFFFL);

            if (!checkedColumns.add(columnKey)) {
                continue;
            }

            int startY = Mth.floor(samplePoint.y);
            int maxY = Math.min(client.level.getMaxY(), startY + CurtainRodBlock.MAX_LENGTH);

            for (int y = startY; y <= maxY; y++) {
                BlockPos checkPos = new BlockPos(x, y, z);
                BlockState state = client.level.getBlockState(checkPos);

                if (state.getBlock() instanceof CurtainRodBlock && state.getValue(CurtainRodBlock.HAS_CURTAIN)) {
                    BlockEntity be = client.level.getBlockEntity(checkPos);
                    if (be instanceof CurtainBlockEntity curtain) {
                        CurtainBlockEntity master = curtain.getMasterAnchor();
                        AABB bounds = getCurtainBounds(master);
                        Optional<Vec3> clip = bounds.clip(eyePos, reachEnd);

                        if (clip.isPresent()) {
                            double distSq = eyePos.distanceToSqr(clip.get());
                            if (distSq < closestDistSq) {
                                closestDistSq = distSq;
                                closestCurtain = master;
                            }
                        }
                    }
                    break;
                }

                if (state.isSolidRender()) {
                    break;
                }
            }
        }

        return closestCurtain;
    }

    private static AABB getCurtainBounds(CurtainBlockEntity master) {
        BlockPos pos = master.getBlockPos();
        BlockState state = master.getBlockState();
        if (!(state.getBlock() instanceof CurtainRodBlock)) {
            return new AABB(pos);
        }

        Direction facing = state.getValue(CurtainRodBlock.FACING);
        Direction stepDir = master.expandRight ? facing.getClockWise() : facing.getCounterClockWise();

        float[] usable = master.getUsableHorizontalBounds();

        float activeSpan = (float) master.getSpan();
        if (master.getStyle() == CurtainStyle.DRAPES) {
            activeSpan = Math.max(0.20f, (float) master.getSpan() * master.openProgress);
        }

        double startX = pos.getX() + 0.5;
        double startZ = pos.getZ() + 0.5;
        double endX = startX + (stepDir.getStepX() * activeSpan);
        double endZ = startZ + (stepDir.getStepZ() * activeSpan);

        double minX = Math.min(startX, endX);
        double maxX = Math.max(startX, endX);
        double minZ = Math.min(startZ, endZ);
        double maxZ = Math.max(startZ, endZ);

        double topY = pos.getY() + CurtainBlockEntity.CURTAIN_TOP_Y;
        double bottomY = getBottomY(master, topY, pos);

        double depthPadding = 0.08;
        if (facing.getAxis() == Direction.Axis.Z) {
            double centerZ = facing == Direction.NORTH ? pos.getZ() + 0.875 : pos.getZ() + 0.125;
            minZ = centerZ - depthPadding;
            maxZ = centerZ + depthPadding;
            minX -= 0.05;
            maxX += 0.05;
        } else {
            double centerX = facing == Direction.WEST ? pos.getX() + 0.875 : pos.getX() + 0.125;
            minX = centerX - depthPadding;
            maxX = centerX + depthPadding;
            minZ -= 0.05;
            maxZ += 0.05;
        }

        return new AABB(minX, bottomY, minZ, maxX, topY, maxZ);
    }

    private static double getBottomY(CurtainBlockEntity master, double topY, BlockPos pos) {
        double bottomY;

        if (master.getStyle() == CurtainStyle.ROLLER) {
            float progress = Mth.clampedMap(master.openProgress, 0.15f, 1.0f, 0.0f, 1.0f);
            float deployFactor = 1.0f - progress;
            float fullTravelDistance = CurtainBlockEntity.CURTAIN_TOP_Y - (1.0f - (float) master.getLength());
            float visibleLength = fullTravelDistance * deployFactor;
            bottomY = topY - visibleLength;
        } else {
            bottomY = pos.getY() - (master.getLength() - 1);
        }
        return bottomY;
    }

    public static boolean isDragging() {
        return isDragging;
    }

    private static double computeTrackDragSign(Minecraft client, CurtainBlockEntity masterCurtain, Direction facing) {
        Direction expDir = masterCurtain.expandRight ? facing.getClockWise() : facing.getCounterClockWise();

        float yawRad = (float) Math.toRadians(client.player.getYRot());
        double screenRightX = -Math.cos(yawRad);
        double screenRightZ = -Math.sin(yawRad);

        double dot = screenRightX * expDir.getStepX() + screenRightZ * expDir.getStepZ();
        return (dot >= 0.0) ? 1.0 : -1.0;
    }

    private static void stopDragging() {
        draggingMasterPos = null;
        isDragging = false;
        lockedDragSign = 1.0;
    }
}