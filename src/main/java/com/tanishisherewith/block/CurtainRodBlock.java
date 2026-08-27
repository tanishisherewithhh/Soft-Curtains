package com.tanishisherewith.block;

import com.mojang.serialization.MapCodec;
import com.tanishisherewith.entity.CurtainBlockEntity;
import com.tanishisherewith.registry.CurtainsBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class CurtainRodBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<CurtainRodBlock> CODEC = simpleCodec(CurtainRodBlock::new);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public static final BooleanProperty LEFT_STOPPER = BooleanProperty.create("left_stopper");
    public static final BooleanProperty RIGHT_STOPPER = BooleanProperty.create("right_stopper");

    private static final VoxelShape SHAPE_NORTH = Block.box(0.0D, 12.0D, 12.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 4.0D);
    private static final VoxelShape SHAPE_WEST  = Block.box(12.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_EAST  = Block.box(0.0D, 12.0D, 0.0D, 4.0D, 16.0D, 16.0D);

    public CurtainRodBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(COLOR, DyeColor.WHITE)
                .setValue(LEFT_STOPPER, true)
                .setValue(RIGHT_STOPPER, true));
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
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(LEFT_STOPPER, isEnd(level, pos, facing.getCounterClockWise(), facing))
                .setValue(RIGHT_STOPPER, isEnd(level, pos, facing.getClockWise(), facing));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader levelReader, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction facing = state.getValue(FACING);
        if (direction == facing.getCounterClockWise() || direction == facing.getClockWise()) {
            if (levelReader instanceof Level level) {
                return state
                        .setValue(LEFT_STOPPER, isEnd(level, pos, facing.getCounterClockWise(), facing))
                        .setValue(RIGHT_STOPPER, isEnd(level, pos, facing.getClockWise(), facing));
            }
        }
        return super.updateShape(state, levelReader, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    private boolean isEnd(Level level, BlockPos pos, Direction checkDir, Direction facing) {
        BlockState neighbor = level.getBlockState(pos.relative(checkDir));
        return !(neighbor.getBlock() instanceof CurtainRodBlock && neighbor.getValue(FACING) == facing);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (stack.getItem() instanceof DyeItem) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(COLOR, stack.get(DataComponents.DYE)), Block.UPDATE_ALL);
                if (!player.isCreative()) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof Block) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CurtainBlockEntity curtain) {
                if (curtain.length < 3) {
                    if (!level.isClientSide()) {
                        curtain.length++;
                        curtain.setChanged();
                        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                        if (!player.isCreative()) stack.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        if (stack.getItem() instanceof ShearsItem) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CurtainBlockEntity curtain) {
                if (curtain.length > 1) {
                    if (!level.isClientSide()) {
                        curtain.length--;
                        curtain.setChanged();
                        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        if (!level.isClientSide()) {
            boolean nextState = !state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, nextState), Block.UPDATE_ALL);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);

        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(OPEN), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, COLOR, LEFT_STOPPER, RIGHT_STOPPER);
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
        if (level.isClientSide()) {
            return type == CurtainsBlockEntities.CURTAIN_BE_TYPE
                    ? (lvl, pos, st, be) -> CurtainBlockEntity.clientTick(lvl, pos, st, (CurtainBlockEntity) be)
                    : null;
        }
        return null;
    }
}
