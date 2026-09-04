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
import net.minecraft.util.EasingType;
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
    protected CurtainStyle style = CurtainStyle.DRAPES;

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

    public boolean isAnimating = false;
    public float animStartProgress = 1.0f;
    public float animTargetProgress = 1.0f;
    public int animCurrentTick = 0;
    public int animTotalTicks = 24;

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

        this.animStartProgress = this.openProgress;
        this.animTargetProgress = Mth.clamp(target, 0.15f, 1.0f);
        this.targetOpenProgress = this.animTargetProgress;
        float distance = Math.abs(this.animTargetProgress - this.animStartProgress) / 0.85f;
        this.animTotalTicks = Math.max(12, Math.round(durationTicks * distance));
        this.animCurrentTick = 0;
        this.isAnimating = true;

        this.setChanged();
        if (this.level != null) {
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
            DyeColor removed = this.segmentColors.remove(this.segmentColors.size() - 1);
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

        float nextTarget = (this.targetOpenProgress > 0.5f) ? 0.15f : 1.0f;
        this.animateTo(nextTarget, 30);
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
        float startX = this.expandRight ? bounds[0] : bounds[1];
        float endTargetX = this.expandRight ? bounds[1] : bounds[0];

        float totalTravel = endTargetX - startX;
        float currentTravel = (this.style == CurtainStyle.DRAPES) ? totalTravel * this.openProgress : totalTravel;
        float compressionFactor = (this.style == CurtainStyle.DRAPES) ? (1.0f - this.openProgress) : 0.0f;
        float foldDepth = 0.02f + compressionFactor * 0.05f;
        float totalHeight = (float) this.length - (1.0f - CURTAIN_TOP_Y);

        for (int ix = 0; ix < gw; ix++) {
            float u = (float) ix / (gw - 1);
            float x = startX + (u * currentTravel);
            float z = (this.style == CurtainStyle.DRAPES)
                    ? (float) Math.sin(u * this.span * (3.0 + compressionFactor * 3.0) * Math.PI) * foldDepth
                    : 0.0f;

            for (int iy = 0; iy < GRID_H; iy++) {
                float v = (float) iy / (GRID_H - 1);
                float y = CURTAIN_TOP_Y - (v * totalHeight);
                float clampedZ = z * (float) Math.sqrt(v);

                this.posX[ix][iy] = this.prevX[ix][iy] = x;
                this.posY[ix][iy] = this.prevY[ix][iy] = y;
                this.posZ[ix][iy] = this.prevZ[ix][iy] = clampedZ;
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CurtainBlockEntity be) {
        if (!state.getValue(CurtainRodBlock.HAS_CURTAIN) || !be.isAnchor) return;

        be.prevOpenProgress = be.openProgress;

        if (be.isAnimating) {
            be.animCurrentTick++;
            float t = (float) be.animCurrentTick / (float) be.animTotalTicks;
            t = Mth.clamp(t, 0.0f, 1.0f);
            be.openProgress = Mth.lerp(EasingType.IN_OUT_QUAD.apply(t), be.animStartProgress, be.animTargetProgress);

            if (be.animCurrentTick >= be.animTotalTicks) {
                be.openProgress = be.animTargetProgress;
                be.isAnimating = false;
                be.setChanged();
            }
        }

        if (level.isClientSide()) {
            if (be.style == CurtainStyle.DRAPES) {
                be.tickDrapeClothPhysics(level, pos, state);
            } else {
                be.tickRigidPendulumPhysics(level, pos, state);
            }
        }
    }

    private void tickDrapeClothPhysics(Level level, BlockPos pos, BlockState state) {
        float[] bounds = this.getUsableHorizontalBounds();
        float startX = this.expandRight ? bounds[0] : bounds[1];
        float endTargetX = this.expandRight ? bounds[1] : bounds[0];

        float totalTravel = endTargetX - startX;
        float currentTravel = totalTravel * this.openProgress;
        float compression = 1.0f - this.openProgress;
        float foldDepth = 0.015f + compression * 0.045f;

        int gw = this.allocatedW;
        float totalHeight = (float) this.length - (1.0f - CURTAIN_TOP_Y);

        float dragVelocity = (this.openProgress - this.prevOpenProgress) * Math.abs(totalTravel);
        float dragScale = this.openProgress < 0.5f ? (this.openProgress / 0.5f) : 1.0f;
        this.swayVelocityX = (this.swayVelocityX + dragVelocity * 0.25f * dragScale) * 0.80f;

        float exposure = calculateExposure(level, pos, state.getValue(CurtainRodBlock.FACING));
        float windMultiplier = 0.0f;
        if (this.openProgress > 0.25f) {
            float windRamp = (this.openProgress - 0.25f) / 0.75f;
            windMultiplier = windRamp * windRamp;
        }

        float windZ = getWindZ(level, pos, exposure) * windMultiplier;
        float foldScale = this.openProgress < 0.5f
                ? 1.0f + (0.5f - this.openProgress) * 2.0f
                : (1.0f - (this.openProgress - 0.5f) * 0.6f);

        for (int ix = 0; ix < gw; ix++) {
            float u = (float) ix / (gw - 1);
            float columnBaseX = startX + (u * currentTravel);
            float foldPhase = u * this.span * (3.0f + compression * 3.0f) * (float) Math.PI;
            float baseZ = (float) Math.sin(foldPhase) * foldDepth * foldScale;

            for (int iy = 0; iy < GRID_H; iy++) {
                float v = (float) iy / (GRID_H - 1);
                float targetY = CURTAIN_TOP_Y - (v * totalHeight);

                this.prevX[ix][iy] = this.posX[ix][iy];
                this.prevY[ix][iy] = this.posY[ix][iy];
                this.prevZ[ix][iy] = this.posZ[ix][iy];

                float mobility = v * v;
                float topConstraint = (float) Math.sqrt(v);

                float swayOffsetX = this.expandRight ? (-this.swayVelocityX * mobility * 0.50f) : (this.swayVelocityX * mobility * 0.50f);
                float targetX = columnBaseX + swayOffsetX;
                float targetZ = (baseZ * topConstraint) + (windZ * mobility);

                this.posX[ix][iy] = targetX;
                this.posY[ix][iy] = targetY;
                this.posZ[ix][iy] = Mth.lerp(0.20f, this.posZ[ix][iy], targetZ);
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
        float windZ = getWindZ(level, pos, exposure) * (1.0f - this.openProgress * 0.5f);

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
                this.posZ[ix][iy] = Mth.lerp(0.15f, this.posZ[ix][iy], targetZ);
            }
        }
    }

    private static float calculateExposure(Level level, BlockPos pos, Direction facing) {
        boolean canSeeSky = level.canSeeSky(pos);
        BlockPos frontPos = pos.relative(facing);
        BlockPos backPos = pos.relative(facing.getOpposite());
        boolean frontOpen = !level.getBlockState(frontPos).isSolidRender();
        boolean backOpen = !level.getBlockState(backPos).isSolidRender();

        float exposure = 0.15f;
        if (canSeeSky) {
            exposure = 1.0f;
        } else if (frontOpen && backOpen) {
            exposure = 0.65f;
        } else if (frontOpen || backOpen) {
            exposure = 0.35f;
        }

        if (level.isThundering()) {
            exposure *= 2.4f;
        } else if (level.isRaining()) {
            exposure *= 1.6f;
        }
        return exposure;
    }

    private static float getWindZ(Level level, BlockPos pos, float exposure) {
        long time = level.getGameTime();
        int seed = pos.hashCode();

        float gustCycle = ((time + (seed & 0xFF)) % 240) / 240.0f;
        float gustStrength = 0.0f;
        if (gustCycle > 0.30f && gustCycle < 0.85f) {
            float ramp = (gustCycle - 0.30f) / 0.55f;
            float envelope = (float) Math.sin(ramp * Math.PI);
            float microFlutter = (float) Math.sin((time * 0.12f) + (seed % 17)) * 0.35f + 0.65f;
            gustStrength = envelope * microFlutter;
        }

        return gustStrength * exposure * 0.075f;
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
                int speedTicks = Math.max(6, 50 - (maxPower * 2));
                this.animateTo(1.0f, speedTicks);
            } else {
                this.animateTo(0.15f, 25);
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

        float volume = 0.45f;
        float pitch = opening ? 0.95f : 0.85f;
        pitch += (this.level.getRandom().nextFloat() - 0.5f) * 0.08f;

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

        output.putBoolean("IsAnimating", this.isAnimating);
        output.putFloat("AnimStartProgress", this.animStartProgress);
        output.putFloat("AnimTargetProgress", this.animTargetProgress);
        output.putInt("AnimCurrentTick", this.animCurrentTick);
        output.putInt("AnimTotalTicks", this.animTotalTicks);
        output.putString("Style", this.style.getSerializedName());

        output.store("Segments", DyeColor.CODEC.listOf(), this.getSegmentColors());
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

        if (newAnimating && (!this.isAnimating || this.targetOpenProgress != newTarget)) {
            this.isAnimating = true;
            this.animStartProgress = input.getFloatOr("AnimStartProgress", this.openProgress);
            this.animTargetProgress = input.getFloatOr("AnimTargetProgress", newTarget);
            this.animCurrentTick = input.getIntOr("AnimCurrentTick", 0);
            this.animTotalTicks = input.getIntOr("AnimTotalTicks", 24);
            this.targetOpenProgress = newTarget;
        } else if (!newAnimating && !this.isAnimating) {
            this.openProgress = input.getFloatOr("OpenProgress", 1.0f);
            this.targetOpenProgress = newTarget;
            this.prevOpenProgress = this.openProgress;
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

        tag.putBoolean("IsAnimating", this.isAnimating);
        tag.putFloat("AnimStartProgress", this.animStartProgress);
        tag.putFloat("AnimTargetProgress", this.animTargetProgress);
        tag.putInt("AnimCurrentTick", this.animCurrentTick);
        tag.putInt("AnimTotalTicks", this.animTotalTicks);
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
        this.isAnimating = false;
    }
}