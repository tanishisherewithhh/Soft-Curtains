package com.tanishisherewith.entity;

import com.tanishisherewith.block.CurtainRodBlock;
import com.tanishisherewith.block.CurtainRodType;
import com.tanishisherewith.registry.CurtainsBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;

public class CurtainBlockEntity extends BlockEntity {
    public static final int NODES_PER_BLOCK = 16;
    public static final int GRID_H = 14;
    public static final float STOPPER_MARGIN = 0.09375f;
    public static final float CURTAIN_TOP_Y = 0.8125f;
    public static final float PROGRESS_CLAMP = 0.01f;

    // gnerated phyiscs factors for tweaking
    public static float BUNCH_WIDTH_MIN = PROGRESS_CLAMP + 0.1f;
    public static float BUNCH_WIDTH_RATIO = 0.15f;

    public static int ANIM_MIN_TICKS = 10;
    public static float ANIM_SCALE_DIVISOR = 0.92f;
    public static int ANIM_TOGGLE_TICKS = 26;
    public static int ANIM_REDSTONE_CLOSE_TICKS = 24;
    public static int ANIM_REDSTONE_MAX_TICKS = 45;
    public static int ANIM_REDSTONE_SPEED_FACTOR = 2;
    public static int ANIM_REDSTONE_MIN_TICKS = 6;

    public static float DRAPE_SWAY_DAMPING = 0.88f;
    public static float DRAPE_SWAY_ACCEL = 0.22f;
    public static float DRAPE_SWAY_MAX = 0.12f;
    public static float DRAPE_BUNCH_WIND_REDUCTION = 0.80f;
    public static float DRAPE_WIND_BASE_SCALE = 0.20f;
    public static float DRAPE_FOLD_FREQ_BASE = 2.0f;
    public static float DRAPE_FOLD_FREQ_SPAN = 0.6f;
    public static float DRAPE_FOLD_DEPTH = 0.035f;
    public static float DRAPE_VELOCITY_DAMPING = 0.84f;
    public static float DRAPE_VELOCITY_LIMIT = 0.12f;
    public static float DRAPE_VERTICAL_RESTORE_FORCE = 0.18f;
    public static float DRAPE_WIND_TIME_SPEED = 0.03f;
    public static float DRAPE_WIND_SPATIAL_FREQ = 0.35f;
    public static float DRAPE_FOLD_RESTORE_FORCE = 0.08f;
    public static float DRAPE_INERTIA_SWAY_WEIGHT = 0.16f;
    public static float DRAPE_HORIZONTAL_RESTORE_FORCE = 0.06f;
    public static int DRAPE_SOLVER_ITERATIONS = 6;

    public static float ROLLER_MIN_HEIGHT_FACTOR = 0.08f;
    public static float ROLLER_WIND_SCALE = 0.45f;
    public static float ROLLER_HEM_SWAY_TARGET_SCALE = 1.5f;
    public static float ROLLER_HEM_SWAY_ACCEL = 0.08f;
    public static float ROLLER_HEM_SWAY_DAMPING = 0.90f;
    public static float ROLLER_BILLOW_FORCE = 0.05f;

    public static float PENDULUM_WIND_SCALE = 0.5f;
    public static float PENDULUM_OPEN_DAMPING = 0.5f;
    public static float PENDULUM_LERP_FACTOR = 0.12f;

    public static float WEATHER_CLEAR_FACTOR = 0.10f;
    public static float WEATHER_RAIN_FACTOR = 0.56f;
    public static float WEATHER_STORM_FACTOR = 1.0f;
    public static float EXPOSURE_SKY = 0.70f;
    public static float EXPOSURE_TUNNEL = 0.35f;
    public static float EXPOSURE_ONE_SIDE = 0.175f;
    public static float EXPOSURE_CLOSED = 0.1f;
    public static float WIND_MIN_EXPOSURE = 0.015f;
    public static float WIND_GUST_AMPLITUDE = 0.1f;

    public static float SOUND_VOLUME = 0.45f;
    public static float SOUND_PITCH_OPEN = 0.95f;
    public static float SOUND_PITCH_CLOSE = 0.85f;
    public static float SOUND_PITCH_VARIANCE = 0.08f;

    protected CurtainStyle style = CurtainStyle.DRAPES;
    public @Nullable String customTexture = null;

    public DyeColor color = DyeColor.WHITE;
    public final List<DyeColor> segmentColors = new ArrayList<>();
    public int span = 1;
    public boolean expandRight = true;
    public boolean isAnchor = true;
    public BlockPos anchorPos = null;

    public int length = 1;
    private int lastRedstonePower = 0;

    public float openProgress = 1.0f;
    public float prevOpenProgress = 1.0f;
    public float targetOpenProgress = 1.0f;
    public float progressVelocity = 0.0f;
    public float animOmega = 0.18f;
    public boolean isAnimating = false;

    public float[][] posX;
    public float[][] posY;
    public float[][] posZ;
    public float[][] prevX;
    public float[][] prevY;
    public float[][] prevZ;

    public float swayVelocityX = 0.0f;
    public float swayVelocityZ = 0.0f;
    private int allocatedW = 0;

    public CurtainBlockEntity(BlockPos pos, BlockState state) {
        super(CurtainsBlockEntities.CURTAIN, pos, state);
    }

    public void animateTo(float target, int durationTicks) {
        CurtainBlockEntity master = this.getMasterAnchor();
        if (master != this) {
            master.animateTo(target, durationTicks);
            return;
        }

        float clampedTarget = Mth.clamp(target, PROGRESS_CLAMP, 1.0f);
        if (this.isAnimating && Math.abs(this.targetOpenProgress - clampedTarget) < 0.0001f) {
            return;
        }

        this.targetOpenProgress = clampedTarget;
        float distance = Math.abs(this.targetOpenProgress - this.openProgress);
        float durationScale = Mth.sqrt(distance) / ANIM_SCALE_DIVISOR;
        int totalTicks = Math.max(ANIM_MIN_TICKS, Math.round(durationTicks * durationScale));

        this.animOmega = 4.5f / (float) totalTicks;
        this.isAnimating = true;

        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void setupAsAnchor(DyeColor newColor, int span, boolean expandRight, Direction facing) {
        this.isAnchor = true;
        this.anchorPos = this.worldPosition;
        this.color = (newColor != null) ? newColor : DyeColor.WHITE;
        this.span = Math.max(1, span);
        this.expandRight = expandRight;
        this.length = 1;
        this.segmentColors.clear();
        this.segmentColors.add(this.color);
        this.openProgress = 1.0f;
        this.prevOpenProgress = 1.0f;
        this.targetOpenProgress = 1.0f;
        this.progressVelocity = 0.0f;
        this.isAnimating = false;
        this.swayVelocityX = 0.0f;
        this.swayVelocityZ = 0.0f;
        this.ensureGrid();
        this.resetGrid();
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void setupAsSlice(BlockPos anchor) {
        this.isAnchor = false;
        this.anchorPos = anchor;
        this.span = 1;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public CurtainBlockEntity getMasterAnchor() {
        if (this.isAnchor || this.worldPosition.equals(this.anchorPos)) {
            return this;
        }

        if (this.level != null) {
            BlockEntity be = this.level.getBlockEntity(this.anchorPos);
            if (be instanceof CurtainBlockEntity master && master.isAnchor) {
                return master;
            }

            BlockState state = this.getBlockState();
            if (state.getBlock() instanceof CurtainRodBlock) {
                Direction facing = state.getValue(CurtainRodBlock.FACING);
                for (Direction checkDir : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
                    for (int i = 1; i <= 16; i++) {
                        BlockPos checkPos = this.worldPosition.relative(checkDir, i);
                        BlockEntity target = this.level.getBlockEntity(checkPos);
                        if (target instanceof CurtainBlockEntity curtain && curtain.isAnchor) {
                            this.anchorPos = checkPos;
                            return curtain;
                        }
                    }
                }
            }
        }
        return this;
    }

    public void addSegment(DyeColor woolColor) {
        CurtainBlockEntity master = this.getMasterAnchor();
        if (master != this) {
            master.addSegment(woolColor);
            return;
        }

        this.length++;
        this.segmentColors.add(woolColor != null ? woolColor : DyeColor.WHITE);
        this.resetGrid();
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public DyeColor removeSegment() {
        CurtainBlockEntity master = this.getMasterAnchor();
        if (master != this) {
            return master.removeSegment();
        }

        if (this.length > 1 && !this.segmentColors.isEmpty()) {
            this.length--;
            DyeColor removed = this.segmentColors.removeLast();
            this.resetGrid();
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
            }
            return removed;
        }
        return this.getColor();
    }

    public void setBaseColor(DyeColor newColor) {
        CurtainBlockEntity master = this.getMasterAnchor();
        if (master != this) {
            master.setBaseColor(newColor);
            return;
        }

        this.color = (newColor != null) ? newColor : DyeColor.WHITE;
        this.segmentColors.replaceAll(ignored -> this.color);
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void toggle() {
        CurtainBlockEntity master = this.getMasterAnchor();
        if (master != this) {
            master.toggle();
            return;
        }

        float nextTarget = (this.targetOpenProgress > 0.5f) ? PROGRESS_CLAMP : 1.0f;
        this.animateTo(nextTarget, ANIM_TOGGLE_TICKS);
        this.playCurtainSound(this.targetOpenProgress < 0.5f);
    }

    public void ensureGrid() {
        if (!this.isAnchor) return;
        int totalNodes = this.span * NODES_PER_BLOCK + 1;
        if (this.posX == null || this.allocatedW != totalNodes) {
            this.allocatedW = totalNodes;
            this.posX = new float[totalNodes][GRID_H];
            this.posY = new float[totalNodes][GRID_H];
            this.posZ = new float[totalNodes][GRID_H];
            this.prevX = new float[totalNodes][GRID_H];
            this.prevY = new float[totalNodes][GRID_H];
            this.prevZ = new float[totalNodes][GRID_H];
            this.resetGrid();
        }
    }

    public float getLeftMargin(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof CurtainRodBlock)) return 0.0f;
        CurtainRodType type = state.getValue(CurtainRodBlock.ROD_TYPE);
        return (type == CurtainRodType.STRAIGHT || type == CurtainRodType.END_LEFT) ? STOPPER_MARGIN : 0.0f;
    }

    public float getRightMargin(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof CurtainRodBlock)) return 0.0f;
        CurtainRodType type = state.getValue(CurtainRodBlock.ROD_TYPE);
        return (type == CurtainRodType.STRAIGHT || type == CurtainRodType.END_RIGHT) ? STOPPER_MARGIN : 0.0f;
    }

    public float[] getUsableHorizontalBounds() {
        if (this.level == null) {
            float x0 = this.expandRight ? STOPPER_MARGIN : 1.0f - STOPPER_MARGIN;
            float x1 = this.expandRight ? (float) this.span - STOPPER_MARGIN : STOPPER_MARGIN - (float) (this.span - 1);
            return new float[]{Math.min(x0, x1), Math.max(x0, x1)};
        }

        Direction facing = this.getBlockState().getValue(CurtainRodBlock.FACING);
        Direction stepDir = this.expandRight ? facing.getClockWise() : facing.getCounterClockWise();

        BlockPos anchorBlockPos = this.worldPosition;
        BlockState anchorBlockState = this.getBlockState();

        BlockPos endBlockPos = this.worldPosition.relative(stepDir, this.span - 1);
        BlockState endBlockState = this.level.getBlockState(endBlockPos);

        float startX;
        float endTargetX;

        if (this.expandRight) {
            float leftM = this.getLeftMargin(anchorBlockPos, anchorBlockState);
            float rightM = this.getRightMargin(endBlockPos, endBlockState);
            startX = leftM;
            endTargetX = (float) this.span - rightM;
        } else {
            float rightM = this.getRightMargin(anchorBlockPos, anchorBlockState);
            float leftM = this.getLeftMargin(endBlockPos, endBlockState);
            startX = 1.0f - rightM;
            endTargetX = leftM - (float) (this.span - 1);
        }

        return new float[]{Math.min(startX, endTargetX), Math.max(startX, endTargetX)};
    }

    public void resetGrid() {
        if (!this.isAnchor || this.posX == null || this.level == null) return;
        int gw = this.allocatedW;
        float[] bounds = this.getUsableHorizontalBounds();
        float minX = bounds[0];
        float maxX = bounds[1];
        float usableWidth = maxX - minX;
        float totalHeight = (float) this.length - (1.0f - CURTAIN_TOP_Y);
        float restV = totalHeight / (GRID_H - 1);
        float bunchWidth = Math.max(BUNCH_WIDTH_MIN, usableWidth * BUNCH_WIDTH_RATIO);
        float currentTopWidth = Mth.lerp(this.openProgress, bunchWidth, usableWidth);
        float topStart = this.expandRight ? minX : maxX - currentTopWidth;
        float topEnd = this.expandRight ? minX + currentTopWidth : maxX;

        float compression = 1.0f - this.openProgress;
        float foldFreq = DRAPE_FOLD_FREQ_BASE + this.span * DRAPE_FOLD_FREQ_SPAN;
        float foldDepth = compression * DRAPE_FOLD_DEPTH;

        for (int ix = 0; ix < gw; ix++) {
            float u = (float) ix / (gw - 1);
            float x = Mth.lerp(u, topStart, topEnd);
            float z = (float) Math.sin(u * foldFreq * Math.PI) * foldDepth;

            for (int iy = 0; iy < GRID_H; iy++) {
                float v = (float) iy / (GRID_H - 1);
                float y = CURTAIN_TOP_Y - (v * totalHeight);

                this.posX[ix][iy] = x;
                this.posY[ix][iy] = y;
                this.posZ[ix][iy] = z;
                this.prevX[ix][iy] = x;
                this.prevY[ix][iy] = y;
                this.prevZ[ix][iy] = z;
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CurtainBlockEntity be) {
        if (!state.getValue(CurtainRodBlock.HAS_CURTAIN) || !be.isAnchor) return;

        be.prevOpenProgress = be.openProgress;

        if (be.isAnimating) {
            float delta = be.targetOpenProgress - be.openProgress;
            if (Math.abs(delta) < 0.0002f && Math.abs(be.progressVelocity) < 0.0002f) {
                be.openProgress = be.targetOpenProgress;
                be.progressVelocity = 0.0f;
                be.isAnimating = false;
                be.setChanged();
            } else {
                float omega = be.animOmega;
                float exp = (float) Math.exp(-omega);

                float c1 = be.openProgress - be.targetOpenProgress;
                float c2 = be.progressVelocity + omega * c1;

                float newDelta = (c1 + c2) * exp;
                float newVel = (c2 - omega * (c1 + c2)) * exp;

                be.openProgress = Mth.clamp(be.targetOpenProgress + newDelta, PROGRESS_CLAMP, 1.0f);
                be.progressVelocity = newVel;

                if ((be.openProgress <= PROGRESS_CLAMP && be.progressVelocity < 0.0f)
                        || (be.openProgress >= 1.0f && be.progressVelocity > 0.0f)) {
                    be.progressVelocity = 0.0f;
                    if (Math.abs(be.openProgress - be.targetOpenProgress) < 0.01f) {
                        be.openProgress = be.targetOpenProgress;
                        be.isAnimating = false;
                        be.setChanged();
                    }
                }
            }
        }

        if (level.isClientSide()) {
            if (be.style == CurtainStyle.DRAPES) {
                be.tickDrapeClothPhysics(level, pos, state);
            } else if (be.style == CurtainStyle.ROLLER) {
                be.tickRollerClothPhysics(level, pos, state);
            } else {
                be.tickRigidPendulumPhysics(level, pos, state);
            }
        }
    }

    private void tickRollerClothPhysics(Level level, BlockPos pos, BlockState state) {
        if (!this.isAnchor || this.posX == null) return;

        float[] bounds = this.getUsableHorizontalBounds();
        float minX = bounds[0];
        float maxX = bounds[1];
        int gw = this.allocatedW;
        int gh = GRID_H;

        float totalMaxHeight = (float) this.length - (1.0f - CURTAIN_TOP_Y);
        float deployFactor = Mth.clampedMap(this.openProgress, PROGRESS_CLAMP, 1.0f, 0.0f, 1.0f);
        float currentHeight = totalMaxHeight * Math.max(ROLLER_MIN_HEIGHT_FACTOR, deployFactor);

        float exposure = calculateExposure(level, pos, state.getValue(CurtainRodBlock.FACING));
        float windZ = getWindZ(level, pos, exposure) * ROLLER_WIND_SCALE * deployFactor;
        long time = level.getGameTime();

        float targetHemZ = windZ * ROLLER_HEM_SWAY_TARGET_SCALE;
        this.swayVelocityZ = (this.swayVelocityZ + (targetHemZ - this.swayVelocityZ) * ROLLER_HEM_SWAY_ACCEL) * ROLLER_HEM_SWAY_DAMPING;

        for (int ix = 0; ix < gw; ix++) {
            float u = (float) ix / (gw - 1);
            float targetX = Mth.lerp(u, minX, maxX);

            for (int iy = 0; iy < gh; iy++) {
                float v = (float) iy / (gh - 1);
                float targetY = CURTAIN_TOP_Y - (v * currentHeight);

                this.prevX[ix][iy] = this.posX[ix][iy];
                this.prevY[ix][iy] = this.posY[ix][iy];
                this.prevZ[ix][iy] = this.posZ[ix][iy];

                if (iy == 0) {
                    this.posX[ix][iy] = targetX;
                    this.posY[ix][iy] = CURTAIN_TOP_Y;
                    this.posZ[ix][iy] = 0.0f;
                    continue;
                }

                float billowShape = (float) Math.sin(v * Math.PI) * (1.0f - 0.3f * Math.abs(u - 0.5f));
                float microFlutter = (float) Math.sin((time * 0.12f) + ix * 0.4f) * 0.002f * exposure * v;
                float targetZ = (windZ * billowShape * ROLLER_BILLOW_FORCE) + (this.swayVelocityZ * v) + microFlutter;

                this.posX[ix][iy] = targetX;
                this.posY[ix][iy] = targetY;
                this.posZ[ix][iy] = Mth.lerp(0.18f, this.posZ[ix][iy], targetZ);
            }
        }
    }

    private void tickDrapeClothPhysics(Level level, BlockPos pos, BlockState state) {
        if (!this.isAnchor || this.posX == null) return;
        int gw = this.allocatedW;
        int gh = GRID_H;

        float[] bounds = this.getUsableHorizontalBounds();
        float minX = bounds[0];
        float maxX = bounds[1];
        float usableWidth = maxX - minX;
        float totalHeight = (float) this.length - (1.0f - CURTAIN_TOP_Y);
        float restV = totalHeight / (gh - 1);

        float bunchFactor = 1.0f - this.openProgress;
        float bunchWidth = Math.max(BUNCH_WIDTH_MIN, usableWidth * BUNCH_WIDTH_RATIO);
        float currentTopWidth = Mth.lerp(this.openProgress, bunchWidth, usableWidth);
        float topStart = this.expandRight ? minX : maxX - currentTopWidth;
        float topEnd = this.expandRight ? minX + currentTopWidth : maxX;

        float dragDelta = this.openProgress - this.prevOpenProgress;
        float edgeVelocityX = (this.expandRight ? 1.0f : -1.0f) * dragDelta * (usableWidth - bunchWidth);
        this.swayVelocityX = Mth.clamp((this.swayVelocityX * DRAPE_SWAY_DAMPING) + (edgeVelocityX * DRAPE_SWAY_ACCEL), -DRAPE_SWAY_MAX, DRAPE_SWAY_MAX);

        float restH = currentTopWidth / (gw - 1);

        float exposure = calculateExposure(level, pos, state.getValue(CurtainRodBlock.FACING));
        long time = level.getGameTime();
        int seed = pos.hashCode();

        float windExposure = exposure * (1.0f - bunchFactor * DRAPE_BUNCH_WIND_REDUCTION);
        float windBase = getWindZ(level, pos, windExposure) * DRAPE_WIND_BASE_SCALE;

        float foldFreq = DRAPE_FOLD_FREQ_BASE + this.span * DRAPE_FOLD_FREQ_SPAN;
        float foldDepth = bunchFactor * DRAPE_FOLD_DEPTH;

        for (int ix = 0; ix < gw; ix++) {
            float u = (float) ix / (gw - 1);
            float pinX = Mth.lerp(u, topStart, topEnd);
            float movingFactor = this.expandRight ? u : (1.0f - u);

            for (int iy = 0; iy < gh; iy++) {
                if (iy == 0) {
                    this.prevX[ix][iy] = this.posX[ix][iy];
                    this.prevY[ix][iy] = this.posY[ix][iy];
                    this.prevZ[ix][iy] = this.posZ[ix][iy];

                    this.posX[ix][iy] = pinX;
                    this.posY[ix][iy] = CURTAIN_TOP_Y;
                    this.posZ[ix][iy] = 0.0f;
                    continue;
                }

                float tempX = this.posX[ix][iy];
                float tempY = this.posY[ix][iy];
                float tempZ = this.posZ[ix][iy];

                float vx = (this.posX[ix][iy] - this.prevX[ix][iy]) * DRAPE_VELOCITY_DAMPING;
                float vy = (this.posY[ix][iy] - this.prevY[ix][iy]) * DRAPE_VELOCITY_DAMPING;
                float vz = (this.posZ[ix][iy] - this.prevZ[ix][iy]) * DRAPE_VELOCITY_DAMPING;

                float v = (float) iy / (gh - 1);

                vx = Mth.clamp(vx, -DRAPE_VELOCITY_LIMIT, DRAPE_VELOCITY_LIMIT);
                vy = Mth.clamp(vy, -DRAPE_VELOCITY_LIMIT, DRAPE_VELOCITY_LIMIT);
                vz = Mth.clamp(vz, -DRAPE_VELOCITY_LIMIT, DRAPE_VELOCITY_LIMIT);

                float idealY = CURTAIN_TOP_Y - (v * totalHeight);
                vy += (idealY - this.posY[ix][iy]) * DRAPE_VERTICAL_RESTORE_FORCE;

                float windPhase = (time + seed) * DRAPE_WIND_TIME_SPEED;
                float flutter = (float) Math.sin(windPhase + ix * DRAPE_WIND_SPATIAL_FREQ) * windBase * v;
                vz += flutter;

                float targetZ = (float) Math.sin(u * foldFreq * Math.PI) * foldDepth;
                vz += (targetZ - this.posZ[ix][iy]) * DRAPE_FOLD_RESTORE_FORCE;

                float idealX = pinX - (this.swayVelocityX * movingFactor * v * DRAPE_INERTIA_SWAY_WEIGHT);
                vx += (idealX - this.posX[ix][iy]) * DRAPE_HORIZONTAL_RESTORE_FORCE;

                this.prevX[ix][iy] = tempX;
                this.prevY[ix][iy] = tempY;
                this.prevZ[ix][iy] = tempZ;

                this.posX[ix][iy] += vx;
                this.posY[ix][iy] += vy;
                this.posZ[ix][iy] += vz;
            }
        }

        for (int iter = 0; iter < DRAPE_SOLVER_ITERATIONS; iter++) {
            for (int ix = 0; ix < gw - 1; ix++) {
                for (int iy = 1; iy < gh; iy++) {
                    float dx = this.posX[ix + 1][iy] - this.posX[ix][iy];
                    float dy = this.posY[ix + 1][iy] - this.posY[ix][iy];
                    float dz = this.posZ[ix + 1][iy] - this.posZ[ix][iy];
                    float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist < 0.0001f) continue;
                    float diff = (dist - restH) / dist * 0.5f;

                    if (this.expandRight && ix == 0) {
                        this.posY[ix][iy] += dy * diff;
                        this.posZ[ix][iy] += dz * diff;
                        this.posX[ix + 1][iy] -= dx * diff * 2.0f;
                    } else if (!this.expandRight && ix + 1 == gw - 1) {
                        this.posX[ix][iy] += dx * diff * 2.0f;
                        this.posY[ix][iy] += dy * diff;
                        this.posZ[ix][iy] += dz * diff;
                    } else {
                        this.posX[ix][iy] += dx * diff;
                        this.posY[ix][iy] += dy * diff;
                        this.posZ[ix][iy] += dz * diff;
                        this.posX[ix + 1][iy] -= dx * diff;
                    }

                    this.posY[ix + 1][iy] -= dy * diff;
                    this.posZ[ix + 1][iy] -= dz * diff;
                }
            }

            for (int ix = 0; ix < gw; ix++) {
                for (int iy = 0; iy < gh - 1; iy++) {
                    float dx = this.posX[ix][iy + 1] - this.posX[ix][iy];
                    float dy = this.posY[ix][iy + 1] - this.posY[ix][iy];
                    float dz = this.posZ[ix][iy + 1] - this.posZ[ix][iy];
                    float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist < 0.0001f) continue;
                    float diff = (dist - restV) / dist * 0.5f;

                    if (iy > 0) {
                        this.posX[ix][iy] += dx * diff;
                        this.posY[ix][iy] += dy * diff;
                        this.posZ[ix][iy] += dz * diff;
                    }
                    this.posX[ix][iy + 1] -= dx * diff;
                    this.posY[ix][iy + 1] -= dy * diff;
                    this.posZ[ix][iy + 1] -= dz * diff;
                }
            }

            for (int ix = 0; ix < gw; ix++) {
                float u = (float) ix / (gw - 1);
                this.posX[ix][0] = Mth.lerp(u, topStart, topEnd);
                this.posY[ix][0] = CURTAIN_TOP_Y;
                this.posZ[ix][0] = 0.0f;
            }

            for (int iy = 0; iy < gh; iy++) {
                if (this.expandRight) {
                    this.posX[0][iy] = minX;
                } else {
                    this.posX[gw - 1][iy] = maxX;
                }
            }
        }
    }

    private void tickRigidPendulumPhysics(Level level, BlockPos pos, BlockState state) {
        float[] bounds = this.getUsableHorizontalBounds();
        float minX = bounds[0];
        float maxX = bounds[1];

        int gw = this.allocatedW;
        float totalHeight = (float) this.length - (1.0f - CURTAIN_TOP_Y);

        float exposure = calculateExposure(level, pos, state.getValue(CurtainRodBlock.FACING));
        float windZ = getWindZ(level, pos, exposure) * (1.0f - this.openProgress * PENDULUM_OPEN_DAMPING) * PENDULUM_WIND_SCALE;

        for (int ix = 0; ix < gw; ix++) {
            float u = (float) ix / (gw - 1);
            float targetX = Mth.lerp(u, minX, maxX);

            for (int iy = 0; iy < GRID_H; iy++) {
                float v = (float) iy / (GRID_H - 1);
                float targetY = CURTAIN_TOP_Y - (v * totalHeight);

                this.prevX[ix][iy] = this.posX[ix][iy];
                this.prevY[ix][iy] = this.posY[ix][iy];
                this.prevZ[ix][iy] = this.posZ[ix][iy];

                float targetZ = windZ * (v * v);

                this.posX[ix][iy] = targetX;
                this.posY[ix][iy] = targetY;
                this.posZ[ix][iy] = Mth.lerp(PENDULUM_LERP_FACTOR, this.posZ[ix][iy], targetZ);
            }
        }
    }

    private static float calculateExposure(Level level, BlockPos pos, Direction facing) {
        float weatherFactor = WEATHER_CLEAR_FACTOR;
        if (level.isThundering()) {
            weatherFactor = WEATHER_STORM_FACTOR;
        } else if (level.isRaining()) {
            weatherFactor = WEATHER_RAIN_FACTOR;
        }

        boolean canSeeSky = level.canSeeSky(pos);
        BlockPos frontPos = pos.relative(facing);
        BlockPos backPos = pos.relative(facing.getOpposite());
        boolean frontOpen = !level.getBlockState(frontPos).isSolidRender();
        boolean backOpen = !level.getBlockState(backPos).isSolidRender();

        float exposure = EXPOSURE_CLOSED;
        if (canSeeSky) {
            exposure = EXPOSURE_SKY;
        } else if (frontOpen && backOpen) {
            exposure = EXPOSURE_TUNNEL;
        } else if (frontOpen || backOpen) {
            exposure = EXPOSURE_ONE_SIDE;
        }

        return exposure * weatherFactor;
    }

    private static float getWindZ(Level level, BlockPos pos, float exposure) {
        if (exposure < WIND_MIN_EXPOSURE) {
            return 0.0f;
        }

        long time = level.getGameTime();
        int seed = pos.hashCode();

        float gustCycle = ((time + (seed & 0xFF)) % 240) / 240.0f;
        float gustStrength = 0.0f;
        if (gustCycle > 0.35f && gustCycle < 0.80f) {
            float ramp = (gustCycle - 0.35f) / 0.45f;
            float envelope = (float) Math.sin(ramp * Math.PI);
            gustStrength = envelope * WIND_GUST_AMPLITUDE;
        }

        return gustStrength * exposure;
    }

    @Override
    public @Nullable Object getRenderData() {
        return super.getRenderData();
    }

    public void handleRedstoneInput(Level level) {
        if (!this.isAnchor) {
            CurtainBlockEntity master = this.getMasterAnchor();
            if (master != this) {
                master.handleRedstoneInput(level);
            }
            return;
        }

        Direction facing = this.getBlockState().getValue(CurtainRodBlock.FACING);
        Direction stepDir = this.expandRight ? facing.getClockWise() : facing.getCounterClockWise();

        int maxPower = 0;
        for (int i = 0; i < this.span; i++) {
            BlockPos checkPos = this.worldPosition.relative(stepDir, i);
            int power = level.getBestNeighborSignal(checkPos);
            if (power > maxPower) {
                maxPower = power;
            }
        }

        if (maxPower != this.lastRedstonePower) {
            this.lastRedstonePower = maxPower;
            if (maxPower > 0) {
                int speedTicks = Math.max(ANIM_REDSTONE_MIN_TICKS, ANIM_REDSTONE_MAX_TICKS - (maxPower * ANIM_REDSTONE_SPEED_FACTOR));
                this.animateTo(1.0f, speedTicks);
            } else {
                this.animateTo(PROGRESS_CLAMP, ANIM_REDSTONE_CLOSE_TICKS);
            }
        }
    }

    public void playCurtainSound(boolean opening) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        SoundEvent sound = switch (this.getStyle()) {
            case DRAPES, ROLLER -> SoundEvents.BUNDLE_INSERT;
            case BLINDS -> SoundEvents.SCAFFOLDING_STEP;
            case SHUTTERS -> SoundEvents.BAMBOO_WOOD_STEP;
        };

        float volume = SOUND_VOLUME;
        float pitch = opening ? SOUND_PITCH_OPEN : SOUND_PITCH_CLOSE;
        pitch += (this.level.getRandom().nextFloat() - 0.5f) * SOUND_PITCH_VARIANCE;

        this.level.playSound(
                null,
                this.worldPosition,
                sound,
                SoundSource.BLOCKS,
                volume,
                pitch
        );
    }

    public float getMeshX(int ix, int iy, float tickDelta) {
        if (posX == null || posX.length == 0) return ix / (float) (span * NODES_PER_BLOCK) * span;
        int clampedX = Mth.clamp(ix, 0, posX.length - 1);
        int clampedY = Mth.clamp(iy, 0, GRID_H - 1);
        return Mth.lerp(tickDelta, prevX[clampedX][clampedY], posX[clampedX][clampedY]);
    }

    public float getMeshY(int ix, int iy, float tickDelta) {
        if (posY == null || posY.length == 0) return -iy / (float) (GRID_H - 1) * length;
        int clampedX = Mth.clamp(ix, 0, posY.length - 1);
        int clampedY = Mth.clamp(iy, 0, GRID_H - 1);
        return Mth.lerp(tickDelta, prevY[clampedX][clampedY], posY[clampedX][clampedY]);
    }

    public float getMeshZ(int ix, int iy, float tickDelta) {
        if (posZ == null || posZ.length == 0) return 0.0f;
        int clampedX = Mth.clamp(ix, 0, posZ.length - 1);
        int clampedY = Mth.clamp(iy, 0, GRID_H - 1);
        return Mth.lerp(tickDelta, prevZ[clampedX][clampedY], posZ[clampedX][clampedY]);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putBoolean("IsAnchor", this.isAnchor);
        if (this.anchorPos != null) {
            output.putLong("AnchorPos", this.anchorPos.asLong());
        }
        output.putInt("Color", this.getColor().getId());
        output.putInt("Span", this.span);
        output.putBoolean("ExpandRight", this.expandRight);
        output.putInt("Length", this.length);
        output.putFloat("OpenProgress", this.openProgress);
        output.putFloat("TargetOpenProgress", this.targetOpenProgress);
        output.putFloat("ProgressVelocity", this.progressVelocity);
        output.putFloat("AnimOmega", this.animOmega);
        output.putBoolean("IsAnimating", this.isAnimating);
        output.putString("Style", this.style.getSerializedName());

        output.store("Segments", DyeColor.CODEC.listOf(), this.getSegmentColors());

        if (this.customTexture != null) {
            output.putString("CustomTexture", this.customTexture);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        this.isAnchor = input.getBooleanOr("IsAnchor", true);
        long storedAnchor = input.getLongOr("AnchorPos", 0L);
        if (storedAnchor != 0L) {
            this.anchorPos = BlockPos.of(storedAnchor);
        }
        this.color = DyeColor.byId(input.getIntOr("Color", 0));
        this.span = Math.max(1, input.getIntOr("Span", 1));
        this.expandRight = input.getBooleanOr("ExpandRight", true);
        this.length = Math.max(1, input.getIntOr("Length", 1));

        boolean newAnimating = input.getBooleanOr("IsAnimating", false);
        float newTarget = input.getFloatOr("TargetOpenProgress", 1.0f);

        if (newAnimating) {
            if (!this.isAnimating || Math.abs(this.targetOpenProgress - newTarget) > 0.0001f) {
                this.targetOpenProgress = newTarget;
                this.animOmega = input.getFloatOr("AnimOmega", 0.18f);
                this.isAnimating = true;
            }
        } else if (!this.isAnimating) {
            this.openProgress = input.getFloatOr("OpenProgress", 1.0f);
            this.targetOpenProgress = newTarget;
            this.prevOpenProgress = this.openProgress;
            this.progressVelocity = 0.0f;
        }

        this.segmentColors.clear();
        input.read("Segments", DyeColor.CODEC.listOf()).ifPresentOrElse(
                this.segmentColors::addAll,
                () -> this.segmentColors.add(this.color)
        );

        String styleStr = input.getStringOr("Style", "drapes");
        for (CurtainStyle s : CurtainStyle.values()) {
            if (s.getSerializedName().equalsIgnoreCase(styleStr)) {
                this.style = s;
                break;
            }
        }

        this.customTexture = input.getStringOr("CustomTexture", "");
        if (this.customTexture.isEmpty()) {
            this.customTexture = null;
        }

        if (this.segmentColors.isEmpty()) {
            this.segmentColors.add(this.color);
        }

        this.allocatedW = 0;
        if (this.isAnchor) {
            this.ensureGrid();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("IsAnchor", this.isAnchor);
        if (this.anchorPos != null) {
            tag.putLong("AnchorPos", this.anchorPos.asLong());
        }
        tag.putInt("Color", this.getColor().getId());
        tag.putInt("Span", this.span);
        tag.putBoolean("ExpandRight", this.expandRight);
        tag.putInt("Length", this.length);
        tag.putFloat("OpenProgress", this.openProgress);
        tag.putFloat("TargetOpenProgress", this.targetOpenProgress);
        tag.putFloat("ProgressVelocity", this.progressVelocity);
        tag.putFloat("AnimOmega", this.animOmega);
        tag.putBoolean("IsAnimating", this.isAnimating);
        tag.putString("Style", this.style.getSerializedName());

        ListTag segList = new ListTag();
        for (DyeColor c : this.getSegmentColors()) {
            segList.add(StringTag.valueOf(c.getName()));
        }
        tag.put("Segments", segList);
        return tag;
    }

    public DyeColor getColor() {
        return this.color != null ? this.color : DyeColor.WHITE;
    }

    public void setColor(DyeColor color) {
        this.color = (color != null) ? color : DyeColor.WHITE;
    }

    public List<DyeColor> getSegmentColors() {
        if (this.segmentColors.isEmpty()) {
            this.segmentColors.add(this.getColor());
        }
        return segmentColors;
    }

    public CurtainStyle getStyle() {
        return this.style;
    }

    public void setStyle(CurtainStyle style) {
        this.style = style;
        this.resetGrid();
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public @Nullable String getCustomTexture() {
        return this.customTexture;
    }

    public void setCustomTexture(@Nullable String customTexture) {
        this.customTexture = customTexture;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public int getSpan() { return Math.max(1, span); }
    public void setSpan(int span) { this.span = Math.max(1, span); }
    public boolean isExpandRight() { return expandRight; }
    public void setExpandRight(boolean expandRight) { this.expandRight = expandRight; }
    public int getLength() { return Math.max(1, length); }
    public void setLength(int length) { this.length = Math.max(1, length); }
    public float getOpenProgress() { return openProgress; }
    public void setOpenProgress(float openProgress) {
        this.openProgress = openProgress;
        this.targetOpenProgress = openProgress;
        this.progressVelocity = 0.0f;
        this.isAnimating = false;
    }
}