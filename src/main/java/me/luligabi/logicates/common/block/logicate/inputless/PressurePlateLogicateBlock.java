package me.luligabi.logicates.common.block.logicate.inputless;

import me.luligabi.logicates.common.block.logicate.Logicatable;
import me.luligabi.logicates.common.block.logicate.LogicateType;
import me.luligabi.logicates.common.block.logicate.inputless.weather.property.PlateType;
import me.luligabi.logicates.common.block.logicate.inputless.weather.property.PropertyRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

public class PressurePlateLogicateBlock extends PressurePlateBlock implements Logicatable {

    public PressurePlateLogicateBlock() {
        super(
                null,
                FabricBlockSettings.of(Material.STONE).requiresTool().noCollision().strength(0.5F),
                SoundEvents.BLOCK_STONE_PRESSURE_PLATE_CLICK_OFF,
                SoundEvents.BLOCK_STONE_PRESSURE_PLATE_CLICK_ON
        );
        setDefaultState(stateManager.getDefaultState().with(POWERED, false).with(FACING, Direction.NORTH));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(!player.getAbilities().allowModifyWorld) return ActionResult.PASS;
        world.setBlockState(pos, state.cycle(PLATE_TYPE));
        return ActionResult.success(world.isClient);
    }

    @Override
    protected int getRedstoneOutput(World world, BlockPos pos) {
        PlateType plateType = world.getBlockState(pos).get(PLATE_TYPE);
        boolean hasValidEntityInput = world.getNonSpectatingEntities(
                plateType.getFilterEntity(),
                BOX.offset(pos)
            ).stream().anyMatch(plateType.getEntityPredicate());
        return hasValidEntityInput ? 15 : 0;
    }


    @Override
    public LogicateType getLogicateType() {
        return LogicateType.INPUTLESS;
    }

    @Override
    public List<MutableText> getLogicateTooltip() {
        return List.of(
                Text.literal("placeholder")
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(PLATE_TYPE, FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getPlayerFacing());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<PlateType> PLATE_TYPE = PropertyRegistry.PLATE_TYPE;

}
