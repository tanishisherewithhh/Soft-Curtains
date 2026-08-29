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
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class CurtainBlockEntity extends BlockEntity {
    public static final int NODES_PER_BLOCK = 10;
    public static final int GRID_H = 10;
    public static final float STOPPER_MARGIN = 0.125f;

    public DyeColor color = DyeColor.WHITE;
    public final List<DyeColor> segmentColors = new ArrayList<>();
    public int span = 1;
    public boolean expandRight = true;
    public boolean isAnchor = true;
    public BlockPos anchorPos = null;

    public int length = 1;
    public float openProgress = 1.0f;
    public float prevOpenProgress = 1.0f;

    public float[][] posX;
    public float[][] posY;
    public float[][] posZ;
    public float[][] prevX;
    public float[][] prevY;
    public float[][] prevZ;

    public float swayVelocityX = 0.0f;
    private int allocatedW = 0;

    public CurtainBlockEntity(BlockPos pos, BlockState state) {
        super(CurtainsBlockEntities.CURTAIN, pos, state);
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
        this.swayVelocityX = 0.0f;
        this.ensureGrid();
        this.resetGrid();
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void setupAsSlice(BlockPos anchor) {
        this.isAnchor = false;
        this.anchorPos = anchor;
        this.span = 1;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public CurtainBlockEntity getMasterAnchor() {
        if (this.isAnchor || this.worldPosition.equals(this.anchorPos)) {
            return this;
        }

        if (this.level != null) {
            if (this.anchorPos != null) {
                BlockEntity be = this.level.getBlockEntity(this.anchorPos);
                if (be instanceof CurtainBlockEntity master && master.isAnchor) {
                    return master;
                }
            }

            // Fallback track search along rod directions
            BlockState state = this.getBlockState();
            if (state.getBlock() instanceof CurtainRodBlock) {
                Direction facing = state.getValue(CurtainRodBlock.FACING);
                for (Direction checkDir : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
                    for (int i = 1; i <= 16; i++) {
                        BlockPos checkPos = this.worldPosition.relative(checkDir, i);
                        BlockEntity be = this.level.getBlockEntity(checkPos);
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
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
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
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
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
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void toggle() {
        CurtainBlockEntity master = this.getMasterAnchor();
        if (master != this) {
            master.toggle();
            return;
        }

        this.openProgress = (this.openProgress > 0.5f) ? 0.15f : 1.0f;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
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

    private float getLeftMargin() {
        if (this.level == null) return 0.0f;
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof CurtainRodBlock)) return 0.0f;
        CurtainRodType type = state.getValue(CurtainRodBlock.ROD_TYPE);
        return (type == CurtainRodType.STRAIGHT || type == CurtainRodType.END_LEFT) ? STOPPER_MARGIN : 0.0f;
    }

    private float getRightMargin() {
        if (this.level == null) return 0.0f;
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof CurtainRodBlock)) return 0.0f;
        CurtainRodType type = state.getValue(CurtainRodBlock.ROD_TYPE);
        return (type == CurtainRodType.STRAIGHT || type == CurtainRodType.END_RIGHT) ? STOPPER_MARGIN : 0.0f;
    }

    public void resetGrid() {
        if (!this.isAnchor || this.posX == null) return;
        int gw = this.allocatedW;
        float height = (float) this.length;

        float leftMargin = this.getLeftMargin();
        float rightMargin = this.getRightMargin();

        float totalSpanWidth = Math.max(0.01f, (float) this.span - leftMargin - rightMargin);
        float currentWidth = totalSpanWidth * this.openProgress;
        float foldDepth = 0.025f * this.openProgress;

        float startX = this.expandRight ? leftMargin : (1.0f - rightMargin);

        for (int ix = 0; ix < gw; ix++) {
            float u = (float) ix / (gw - 1);
            float x = this.expandRight
                    ? (startX + (u * currentWidth))
                    : (startX - (u * currentWidth));

            float z = (float) Math.sin(u * this.span * 4.0 * Math.PI) * foldDepth;

            for (int iy = 0; iy < GRID_H; iy++) {
                float v = (float) iy / (GRID_H - 1);
                float y = 0.85f - (v * height);

                this.posX[ix][iy] = this.prevX[ix][iy] = x;
                this.posY[ix][iy] = this.prevY[ix][iy] = y;
                this.posZ[ix][iy] = this.prevZ[ix][iy] = z;
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CurtainBlockEntity be) {
        if (!state.getValue(CurtainRodBlock.HAS_CURTAIN) || !be.isAnchor) return;

        Direction facing = state.getValue(CurtainRodBlock.FACING);
        Direction stepDir = be.expandRight ? facing.getClockWise() : facing.getCounterClockWise();
        int activeSpan = 1;

        for (int i = 1; i < be.span; i++) {
            BlockPos nextPos = pos.relative(stepDir, i);
            BlockState nextState = level.getBlockState(nextPos);
            if (nextState.getBlock() instanceof CurtainRodBlock &&
                    nextState.getValue(CurtainRodBlock.FACING) == facing &&
                    nextState.getValue(CurtainRodBlock.HAS_CURTAIN)) {
                activeSpan++;
            } else {
                break;
            }
        }

        if (be.span != activeSpan) {
            be.span = activeSpan;
            be.allocatedW = 0;
            be.ensureGrid();
            be.resetGrid();
        }

        be.ensureGrid();

        int gw = be.allocatedW;
        float height = (float) be.length;

        float leftMargin = be.getLeftMargin();
        float rightMargin = be.getRightMargin();

        float totalSpanWidth = Math.max(0.01f, (float) be.span - leftMargin - rightMargin);
        float currentWidth = totalSpanWidth * be.openProgress;
        float foldDepth = 0.025f * be.openProgress;

        float dragVelocity = (be.openProgress - be.prevOpenProgress) * totalSpanWidth;
        be.prevOpenProgress = be.openProgress;

        be.swayVelocityX += dragVelocity * 0.45f;
        be.swayVelocityX *= 0.82f;

        float startX = be.expandRight ? leftMargin : (1.0f - rightMargin);

        for (int ix = 0; ix < gw; ix++) {
            float u = (float) ix / (gw - 1);
            float columnTargetX = be.expandRight
                    ? (startX + (u * currentWidth))
                    : (startX - (u * currentWidth));

            float targetZ = (float) Math.sin(u * be.span * 4.0 * Math.PI) * foldDepth;

            for (int iy = 0; iy < GRID_H; iy++) {
                float v = (float) iy / (GRID_H - 1);
                float targetY = 0.85f - (v * height);

                be.prevX[ix][iy] = be.posX[ix][iy];
                be.prevY[ix][iy] = be.posY[ix][iy];
                be.prevZ[ix][iy] = be.posZ[ix][iy];

                float swayAmount = be.expandRight ? (-be.swayVelocityX * v * 0.85f) : (be.swayVelocityX * v * 0.85f);
                float targetX = columnTargetX + swayAmount;

                be.posX[ix][iy] = Mth.lerp(0.35f, be.posX[ix][iy], targetX);
                be.posY[ix][iy] = targetY;
                be.posZ[ix][iy] = targetZ;
            }
        }
    }

    public float getMeshX(int ix, int iy, float tickDelta) {
        if (posX == null || posX.length == 0) return ix / (float) (span * 10) * span;
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
        this.openProgress = input.getFloatOr("OpenProgress", 1.0f);
        this.prevOpenProgress = this.openProgress;

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
    }
}