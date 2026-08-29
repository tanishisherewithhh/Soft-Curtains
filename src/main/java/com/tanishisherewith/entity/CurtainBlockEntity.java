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

import java.util.ArrayList;
import java.util.List;

public class CurtainBlockEntity extends BlockEntity {
    public static final int NODES_PER_BLOCK = 16;
    public static final int GRID_H = 14;
    public static final float STOPPER_MARGIN = 0.15625f;
    public static final float CURTAIN_TOP_Y = 0.8125f;

    public DyeColor color = DyeColor.WHITE;
    public final List<DyeColor> segmentColors = new ArrayList<>();
    public int span = 1;
    public boolean expandRight = true;
    public boolean isAnchor = true;
    public BlockPos anchorPos = null;

    public int length = 1;

    // Progress values: 0.15f (Closed) to 1.0f (Open)
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
        this.animTotalTicks = Math.max(1, durationTicks);
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
                        if (be instanceof CurtainBlockEntity target && target.isAnchor) {
                            this.anchorPos = checkPos;
                            return target;
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
        for (int i = 0; i < this.segmentColors.size(); i++) {
            this.segmentColors.set(i, this.color);
        }
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

    private float getLeftMargin(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof CurtainRodBlock)) return 0.0f;
        CurtainRodType type = state.getValue(CurtainRodBlock.ROD_TYPE);
        return (type == CurtainRodType.STRAIGHT || type == CurtainRodType.END_LEFT) ? STOPPER_MARGIN : 0.0f;
    }

    private float getRightMargin(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof CurtainRodBlock)) return 0.0f;
        CurtainRodType type = state.getValue(CurtainRodBlock.ROD_TYPE);
        return (type == CurtainRodType.STRAIGHT || type == CurtainRodType.END_RIGHT) ? STOPPER_MARGIN : 0.0f;
    }

    public void resetGrid() {
        if (!this.isAnchor || this.posX == null || this.level == null) return;
        int gw = this.allocatedW;
        float height = (float) this.length;

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

        float totalTravel = endTargetX - startX;
        float currentTravel = totalTravel * this.openProgress;
        float compressionFactor = 1.0f - this.openProgress;
        float foldDepth = 0.02f + compressionFactor * 0.05f;

        for (int ix = 0; ix < gw; ix++) {
            float u = (float) ix / (gw - 1);
            float x = startX + (u * currentTravel);
            float z = (float) Math.sin(u * this.span * (3.0 + compressionFactor * 3.0) * Math.PI) * foldDepth;

            for (int iy = 0; iy < GRID_H; iy++) {
                float v = (float) iy / (GRID_H - 1);
                float y = CURTAIN_TOP_Y - (v * height);

                this.posX[ix][iy] = this.prevX[ix][iy] = x;
                this.posY[ix][iy] = this.prevY[ix][iy] = y;
                this.posZ[ix][iy] = this.prevZ[ix][iy] = z;
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
            Direction facing = state.getValue(CurtainRodBlock.FACING);
            Direction stepDir = be.expandRight ? facing.getClockWise() : facing.getCounterClockWise();

            BlockPos anchorBlockPos = be.worldPosition;
            BlockState anchorBlockState = be.getBlockState();

            BlockPos endBlockPos = be.worldPosition.relative(stepDir, be.span - 1);
            BlockState endBlockState = level.getBlockState(endBlockPos);

            float startX;
            float endTargetX;

            if (be.expandRight) {
                float leftM = be.getLeftMargin(anchorBlockPos, anchorBlockState);
                float rightM = be.getRightMargin(endBlockPos, endBlockState);
                startX = leftM;
                endTargetX = (float) be.span - rightM;
            } else {
                float rightM = be.getRightMargin(anchorBlockPos, anchorBlockState);
                float leftM = be.getLeftMargin(endBlockPos, endBlockState);
                startX = 1.0f - rightM;
                endTargetX = leftM - (float) (be.span - 1);
            }

            float totalTravel = endTargetX - startX;
            float currentTravel = totalTravel * be.openProgress;

            float compression = 1.0f - be.openProgress;
            float foldDepth = 0.02f + compression * 0.05f;

            float dragVelocity = (be.openProgress - be.prevOpenProgress) * Math.abs(totalTravel);
            be.swayVelocityX = (be.swayVelocityX + dragVelocity * 0.35f) * 0.80f;
            be.swayVelocityZ = (be.swayVelocityZ + Math.abs(dragVelocity) * 0.20f * (float) Math.sin(be.openProgress * Math.PI)) * 0.80f;

            int gw = be.allocatedW;
            float height = (float) be.length;

            for (int ix = 0; ix < gw; ix++) {
                float u = (float) ix / (gw - 1);
                float columnBaseX = startX + (u * currentTravel);

                float foldPhase = u * be.span * (3.0f + compression * 3.0f) * (float) Math.PI;
                float baseZ = (float) Math.sin(foldPhase) * foldDepth;

                for (int iy = 0; iy < GRID_H; iy++) {
                    float v = (float) iy / (GRID_H - 1);
                    float targetY = CURTAIN_TOP_Y - (v * height);

                    be.prevX[ix][iy] = be.posX[ix][iy];
                    be.prevY[ix][iy] = be.posY[ix][iy];
                    be.prevZ[ix][iy] = be.posZ[ix][iy];

                    float bend = v * v;
                    float swayOffset = be.expandRight ? (-be.swayVelocityX * bend * 0.70f) : (be.swayVelocityX * bend * 0.70f);
                    float targetX = columnBaseX + swayOffset;
                    float targetZ = baseZ + (be.swayVelocityZ * (float) Math.sin(v * Math.PI) * 0.35f);

                    be.posX[ix][iy] = targetX;
                    be.posY[ix][iy] = targetY;
                    be.posZ[ix][iy] = targetZ;
                }
            }
        }
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

        ListTag segList = new ListTag();
        for (DyeColor c : this.getSegmentColors()) {
            segList.add(StringTag.valueOf(c.getName()));
        }
        tag.put("Segments", segList);
        return tag;
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

        if (this.segmentColors.isEmpty()) {
            this.segmentColors.add(this.color);
        }

        this.allocatedW = 0;
        if (this.isAnchor) {
            this.ensureGrid();
        }
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