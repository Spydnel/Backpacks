package com.spydnel.backpacks.blocks;

import com.mojang.serialization.MapCodec;
import com.spydnel.backpacks.registry.BPBlockEntities;
import com.spydnel.backpacks.registry.BPItems;
import com.spydnel.backpacks.registry.BPSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class BackpackBlock extends BaseEntityBlock implements Equipable, EntityBlock, SimpleWaterloggedBlock {
    protected static final VoxelShape SHAPE_X;
    protected static final VoxelShape SHAPE_Z;
    protected static final VoxelShape FLOATING_SHAPE_X;
    protected static final VoxelShape FLOATING_SHAPE_Z;
    public static final DirectionProperty FACING;
    public static final BooleanProperty FLOATING;
    public static final BooleanProperty WATERLOGGED;

    public BackpackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FLOATING, false)
                .setValue(WATERLOGGED, false));
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(FLOATING, level.getFluidState(pos.below()).isSource() && !level.getFluidState(pos).isSource())
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if ((Boolean)state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(FLOATING, level.getFluidState(currentPos.below()).isSource());
    }

    protected FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BPBlockEntities.BACKPACK.get(), BackpackBlockEntity::tick);
    }

    public EquipmentSlot getEquipmentSlot() { return EquipmentSlot.CHEST; }

    public Holder<SoundEvent> getEquipSound() {
        return BPSounds.BACKPACK_EQUIP;
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (player.isSpectator()) {
            return InteractionResult.CONSUME;
        } else {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BackpackBlockEntity backpackBlockEntity) {

                player.openMenu(backpackBlockEntity);
                backpackBlockEntity.onOpen(player);

                return InteractionResult.CONSUME;
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = (Direction)state.getValue(FACING);
        if (state.getValue(FLOATING)) {
            return direction.getAxis() == Direction.Axis.X ? FLOATING_SHAPE_Z : FLOATING_SHAPE_X;
        } else {
            return direction.getAxis() == Direction.Axis.X ? SHAPE_Z : SHAPE_X;
        }

    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
    }

    public MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(FLOATING);
        builder.add(WATERLOGGED);
    }

    static {
        FACING = HorizontalDirectionalBlock.FACING;
        FLOATING = BooleanProperty.create("floating");
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        SHAPE_X = Block.box(3.0, 0.0, 4.0, 13.0, 11.0, 12.0);
        SHAPE_Z = Block.box(4.0, 0.0, 3.0, 12.0, 11.0, 13.0);
        FLOATING_SHAPE_X = Block.box(3.0, 0.0, 4.0, 13.0, 8.0, 12.0);
        FLOATING_SHAPE_Z = Block.box(4.0, 0.0, 3.0, 12.0, 8.0, 13.0);
    }
}
