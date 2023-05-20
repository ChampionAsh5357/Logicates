package me.luligabi.logicates.common.block.logicate.inputless.keypad;

import me.luligabi.logicates.common.Logicates;
import me.luligabi.logicates.common.block.logicate.inputless.InputlessLogicateBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation")
public class KeypadLogicateBlock extends InputlessLogicateBlock implements BlockEntityProvider {


    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient()) return ActionResult.SUCCESS;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if(blockEntity instanceof KeypadLogicateBlockEntity keypadLogicateBlockEntity) {
            player.openHandledScreen(keypadLogicateBlockEntity);
            keypadLogicateBlockEntity.sync();
        }
        return ActionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new KeypadLogicateBlockEntity(pos, state);
    }

    @Override
    public boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
        super.onSyncedBlockEvent(state, world, pos, type, data);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity.onSyncedBlockEvent(type, data);
    }

    @SuppressWarnings("ConstantConditions")
    public void powerOn(BlockState state, World world, BlockPos pos) {
        KeypadLogicateBlockEntity blockEntity = ((KeypadLogicateBlockEntity) world.getBlockEntity(pos));

        world.setBlockState(pos, state.with(POWERED, true), 3);
        world.updateNeighborsAlways(pos, state.getBlock());
        world.scheduleBlockTick(pos, this, blockEntity.closingDelay * 20);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(state.get(POWERED)) {
            world.setBlockState(pos, state.with(POWERED, false), 3);
            world.updateNeighborsAlways(pos, state.getBlock());
        }
    }

    @Override
    protected int getPower(World world, BlockPos pos, BlockState state) {
        return world.getBlockState(pos).get(POWERED) ? 15 : 0;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return world.getBlockState(pos).get(POWERED) ? 15 : 0;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction direction = state.get(FACING);
        BlockPos blockPos = pos.offset(direction.getOpposite());
        BlockState blockState = world.getBlockState(blockPos);
        return direction.getAxis().isHorizontal() && blockState.isSideSolidFullSquare(world, blockPos, direction);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> VoxelShapes.empty();
        };
    }

    @Override
    public List<MutableText> getLogicateTooltip() {
        return List.of(
                Text.translatable("tooltip.logicates.keypad_logicate")
        );
    }

    public static final int MIN_CLOSING_DELAY = 1;
    public static final int MAX_CLOSING_DELAY = 30;
    public static final Identifier KEYPAD_PASSWORD = Logicates.id("keypad_password");
    public static final Identifier KEYPAD_DELETE = Logicates.id("keypad_delete");
    public static final Identifier KEYPAD_CONFIRM = Logicates.id("keypad_confirm");
    public static final Identifier KEYPAD_TOGGLE_PASSWORD_RESET = Logicates.id("keypad_toggle_password_reset");
    public static final Identifier KEYPAD_CLOSING_DELAY = Logicates.id("keypad_closing_delay");

    public static final VoxelShape NORTH_SHAPE = createCuboidShape(5.75, 5, 15, 10.25, 11, 16);
    public static final VoxelShape SOUTH_SHAPE = createCuboidShape(5.75, 5, 0, 10.25, 11, 1);
    public static final VoxelShape WEST_SHAPE = createCuboidShape(15, 5, 5.75, 16, 11, 10.25);
    public static final VoxelShape EAST_SHAPE = createCuboidShape(0, 5, 5.75, 1, 11, 10.25);
}