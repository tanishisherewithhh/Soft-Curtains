package com.tanishisherewith.block;

import com.mojang.serialization.MapCodec;
import com.tanishisherewith.entity.CurtainBlockEntity;
import com.tanishisherewith.item.CurtainItem;
import com.tanishisherewith.registry.CurtainsBlockEntities;
import com.tanishisherewith.registry.CurtainsItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CurtainRodBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CurtainRodBlock> CODEC = simpleCodec(CurtainRodBlock::new);
    public static final EnumProperty<CurtainRodType> ROD_TYPE = EnumProperty.create("type", CurtainRodType.class);
    public static final BooleanProperty HAS_CURTAIN = BooleanProperty.create("has_curtain");
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

    private static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 12.0D, 12.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 12.0D, 0.0D,  16.0D, 16.0D, 4.0D);
    private static final VoxelShape SHAPE_WEST  = Block.box(12.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_EAST  = Block.box(0.0D,  12.0D, 0.0D, 4.0D,  16.0D, 16.0D);

    public static final int MAX_LENGTH = 30;

    public CurtainRodBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ROD_TYPE, CurtainRodType.STRAIGHT)
                .setValue(HAS_CURTAIN, false)
                .setValue(LOCKED, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        boolean sneak = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        if (sneak) {
            return this.defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(ROD_TYPE, CurtainRodType.NONE)
                    .setValue(LOCKED, true);
        }

        return resolveShape(level, pos, facing, this.defaultBlockState().setValue(FACING, facing));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
        if (state.getValue(LOCKED)) {
            return state;
        }

        Direction facing = state.getValue(FACING);
        if (dir == facing.getClockWise() || dir == facing.getCounterClockWise()) {
            if (level instanceof Level lvl) {
                return resolveShape(lvl, pos, facing, state);
            }
        }
        return super.updateShape(state, level, tickAccess, pos, dir, neighborPos, neighborState, random);
    }

    private BlockState resolveShape(Level level, BlockPos pos, Direction facing, BlockState state) {
        Direction leftDir = facing.getCounterClockWise();
        Direction rightDir = facing.getClockWise();

        boolean hasLeft = isMatching(level, pos.relative(leftDir), facing);
        boolean hasRight = isMatching(level, pos.relative(rightDir), facing);

        CurtainRodType type;
        if (!hasLeft && !hasRight) {
            type = CurtainRodType.STRAIGHT;
        } else if (!hasLeft) {
            type = CurtainRodType.END_LEFT;
        } else if (!hasRight) {
            type = CurtainRodType.END_RIGHT;
        } else {
            type = CurtainRodType.NONE;
        }

        return state.setValue(ROD_TYPE, type);
    }

    private boolean isMatching(Level level, BlockPos p, Direction facing) {
        BlockState s = level.getBlockState(p);
        return s.getBlock() instanceof CurtainRodBlock && s.getValue(FACING) == facing;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        //Shift-Click empty hand to cycle rod shape
        if (stack.isEmpty() && player.isShiftKeyDown() && !state.getValue(HAS_CURTAIN)) {
            if (!level.isClientSide()) {
                CurtainRodType nextType = switch (state.getValue(ROD_TYPE)) {
                    case STRAIGHT -> CurtainRodType.END_LEFT;
                    case END_LEFT -> CurtainRodType.MIDDLE_STOPPER;
                    case MIDDLE_STOPPER -> CurtainRodType.END_RIGHT;
                    case END_RIGHT -> CurtainRodType.NONE;
                    case NONE -> CurtainRodType.STRAIGHT;
                };
                level.setBlock(pos, state.setValue(ROD_TYPE, nextType).setValue(LOCKED, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);

        if (state.getValue(HAS_CURTAIN) && be instanceof CurtainBlockEntity targetBe) {
            CurtainBlockEntity master = targetBe.getMasterAnchor();
            BlockPos masterPos = master.getBlockPos();
            if (stack.getItem() instanceof DyeItem) {
                DyeColor newColor = stack.get(DataComponents.DYE);
                if (newColor != null && master.getColor() != newColor) {
                    if (!level.isClientSide()) {
                        master.setBaseColor(newColor);
                        level.playSound(null, masterPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.CONSUME;
            }

            if (stack.is(Items.SHEARS)) {
                if (master.getLength() > 1) {
                    if (!level.isClientSide()) {
                        DyeColor droppedColor = master.removeSegment();
                        level.playSound(null, masterPos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);

                        ItemStack droppedWool = new ItemStack(Items.WOOL.pick(droppedColor), 1);
                        Containers.dropItemStack(level, masterPos.getX() + 0.5, masterPos.getY() + 0.5, masterPos.getZ() + 0.5, droppedWool);

                        EquipmentSlot slot = player.getUsedItemHand().asEquipmentSlot();
                        stack.hurtAndBreak(1, player, slot);
                    }
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.CONSUME;
            }


            if (stack.is(ItemTags.WOOL)) {
                if (master.getLength() < MAX_LENGTH) {
                    if (!level.isClientSide()) {
                        DyeColor woolColor = getColorFromWoolItem(stack.getItem());
                        master.addSegment(woolColor);
                        level.playSound(null, masterPos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.CONSUME;
            }

            // Shift-Click direct Open/Close
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide()) {
                    master.toggle();
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }


        if (stack.getItem() instanceof CurtainItem curtainItem && !state.getValue(HAS_CURTAIN)) {
            if (!level.isClientSide()) {
                Direction facing = state.getValue(FACING);

                Vec3 hitRel = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                Direction rightDir = facing.getClockWise();
                double hitAlongRod = hitRel.x * rightDir.getStepX() + hitRel.z * rightDir.getStepZ();

                boolean targetLeft = hitAlongRod < 0.0;
                Direction searchDir = targetLeft ? facing.getCounterClockWise() : facing.getClockWise();
                BlockPos targetNeighborPos = pos.relative(searchDir);
                BlockState targetNeighborState = level.getBlockState(targetNeighborPos);

                if (targetNeighborState.getBlock() instanceof CurtainRodBlock &&
                        targetNeighborState.getValue(FACING) == facing &&
                        targetNeighborState.getValue(HAS_CURTAIN)) {

                    BlockEntity targetBe = level.getBlockEntity(targetNeighborPos);
                    if (targetBe instanceof CurtainBlockEntity neighborCurtain) {
                        CurtainBlockEntity master = neighborCurtain.getMasterAnchor();
                        BlockPos masterAnchorPos = master.getBlockPos();

                        Direction expDir = master.expandRight ? facing.getClockWise() : facing.getCounterClockWise();
                        BlockPos expectedNextPos = masterAnchorPos.relative(expDir, master.getSpan());

                        if (pos.equals(expectedNextPos)) {
                            master.setSpan(master.getSpan() + 1);
                            master.ensureGrid();
                            master.resetGrid();
                            master.setChanged();
                            level.sendBlockUpdated(masterAnchorPos, level.getBlockState(masterAnchorPos), level.getBlockState(masterAnchorPos), Block.UPDATE_ALL);

                            level.setBlock(pos, state.setValue(HAS_CURTAIN, true), Block.UPDATE_ALL);
                            BlockEntity newBe = level.getBlockEntity(pos);
                            if (newBe instanceof CurtainBlockEntity slice) {
                                slice.setupAsSlice(masterAnchorPos);
                            }

                            if (!player.isCreative()) {
                                stack.shrink(1);
                            }
                            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }


                boolean expandRight = hitAlongRod < 0.0;
                level.setBlock(pos, state.setValue(HAS_CURTAIN, true), Block.UPDATE_ALL);
                BlockEntity newBe = level.getBlockEntity(pos);
                if (newBe instanceof CurtainBlockEntity newCurtain) {
                    newCurtain.setupAsAnchor(curtainItem.getColor(), 1, expandRight, facing);
                }

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(HAS_CURTAIN)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CurtainBlockEntity curtain) {
                CurtainBlockEntity master = curtain.getMasterAnchor();
                BlockPos masterPos = master.getBlockPos();

                Item droppedItem = CurtainsItems.CURTAINS.get(master.getColor());

                if (curtain.isAnchor) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(droppedItem));

                    List<DyeColor> segs = master.getSegmentColors();
                    if (segs.size() > 1) {
                        for (int i = 1; i < segs.size(); i++) {
                            ItemStack drop = new ItemStack(Items.WOOL.pick(segs.get(i)), 1);
                            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
                        }
                    }

                    Direction facing = state.getValue(FACING);
                    Direction stepDir = master.expandRight ? facing.getClockWise() : facing.getCounterClockWise();
                    if (master.getSpan() > 1) {
                        BlockPos nextPos = pos.relative(stepDir, 1);
                        BlockState nextState = level.getBlockState(nextPos);
                        if (nextState.getBlock() instanceof CurtainRodBlock && nextState.getValue(HAS_CURTAIN)) {
                            BlockEntity nextBe = level.getBlockEntity(nextPos);
                            if (nextBe instanceof CurtainBlockEntity nextAnchor) {
                                nextAnchor.setupAsAnchor(master.getColor(), master.getSpan() - 1, master.expandRight, facing);
                                nextAnchor.segmentColors.clear();
                                nextAnchor.segmentColors.addAll(master.getSegmentColors());
                                nextAnchor.setLength(master.getLength());
                                nextAnchor.setOpenProgress(master.getOpenProgress());

                                for (int i = 2; i < master.getSpan(); i++) {
                                    BlockPos subSlicePos = pos.relative(stepDir, i);
                                    BlockEntity subBe = level.getBlockEntity(subSlicePos);
                                    if (subBe instanceof CurtainBlockEntity subSlice) {
                                        subSlice.anchorPos = nextPos;
                                        subSlice.setChanged();
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (master.isAnchor && master.getSpan() > 1) {
                        master.setSpan(master.getSpan() - 1);
                        master.ensureGrid();
                        master.resetGrid();
                        master.setChanged();
                        level.sendBlockUpdated(masterPos, level.getBlockState(masterPos), level.getBlockState(masterPos), Block.UPDATE_ALL);
                    }
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(droppedItem));
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide() && state.getValue(HAS_CURTAIN)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CurtainBlockEntity curtain) {
                CurtainBlockEntity master = curtain.getMasterAnchor();
                master.handleRedstoneInput(level);
            }
        }
    }

    public static DyeColor getColorFromWoolItem(Item item) {
        for (DyeColor color : DyeColor.values()) {
            if (Items.WOOL.pick(color) == item) {
                return color;
            }
        }
        return DyeColor.WHITE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROD_TYPE, HAS_CURTAIN, LOCKED);
    }

    @Override
    public @NonNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NonNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CurtainBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == CurtainsBlockEntities.CURTAIN
                ? (lvl, p, s, be) -> CurtainBlockEntity.tick(lvl, p, s, (CurtainBlockEntity) be)
                : null;
    }
}