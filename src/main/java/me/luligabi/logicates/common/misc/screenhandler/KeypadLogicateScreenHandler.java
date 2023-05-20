package me.luligabi.logicates.common.misc.screenhandler;

import me.luligabi.logicates.common.block.logicate.inputless.keypad.KeypadLogicateBlock;
import me.luligabi.logicates.common.block.logicate.inputless.keypad.KeypadLogicateBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

public class KeypadLogicateScreenHandler extends ScreenHandler {


    public KeypadLogicateScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId);
        clientPos = buf.readBlockPos();
    }

    public KeypadLogicateScreenHandler(int syncId) {
        super(ScreenHandlingRegistry.KEYPAD_LOGICATE_SCREEN_HANDLER, syncId);
        clientPos = BlockPos.ORIGIN;
    }


    public BlockPos clientPos;

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }



    public static void initPacketReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(KeypadLogicateBlock.KEYPAD_PASSWORD, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            String password = buf.readString();

            server.execute(() -> {
                modifyKeypad(player, pos, (blockEntity) -> {
                    blockEntity.insertNumber(password);
                });
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(KeypadLogicateBlock.KEYPAD_DELETE, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();

            server.execute(() -> {
                modifyKeypad(player, pos, KeypadLogicateBlockEntity::removeNumber);
            });
        });


        ServerPlayNetworking.registerGlobalReceiver(KeypadLogicateBlock.KEYPAD_CONFIRM, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();

            server.execute(() -> {
                modifyKeypad(player, pos, (blockEntity) -> {
                    if(StringUtils.isEmpty(blockEntity.getActivePassword())) return;

                    if(blockEntity.hasPassword) { // Check password
                        if(blockEntity.currentPassword.equals(blockEntity.password)) {
                            player.sendMessage(
                                    Text.translatable(
                                            blockEntity.passwordReset ?
                                                    "message.logicates.keypad_logicate.reset_password" :
                                                    "message.logicates.keypad_logicate.correct_password",
                                            player.getName()
                                    )
                                    .formatted(Formatting.GREEN),
                                    true
                            );
                            if(!blockEntity.passwordReset) {
                                BlockState state = player.world.getBlockState(pos);
                                ((KeypadLogicateBlock) state.getBlock()).powerOn(state, player.world, pos);
                            } else {
                                modifyKeypad(player, pos, KeypadLogicateBlockEntity::reset);
                            }
                        } else {
                            player.sendMessage(
                                    Text.translatable(
                                            blockEntity.passwordReset ?
                                                    "message.logicates.keypad_logicate.reset_password.fail" :
                                                    "message.logicates.keypad_logicate.incorrect_password"
                                    ).formatted(Formatting.RED),
                                    true
                            );
                        }
                    } else { // Set password
                        modifyKeypad(player, pos, KeypadLogicateBlockEntity::enableHasPassword);
                        player.sendMessage(
                                Text.translatable(
                                        "message.logicates.keypad_logicate.set_password"
                                ).formatted(Formatting.YELLOW),
                                true
                        );
                    }
                    modifyKeypad(player, pos, KeypadLogicateBlockEntity::resetCurrentPassword);
                    player.closeHandledScreen();
                });
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(KeypadLogicateBlock.KEYPAD_TOGGLE_PASSWORD_RESET, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();

            server.execute(() -> {
                modifyKeypad(player, pos, KeypadLogicateBlockEntity::togglePasswordReset);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(KeypadLogicateBlock.KEYPAD_CLOSING_DELAY, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            boolean decrease = buf.readBoolean();

            server.execute(() -> {
                modifyKeypad(player, pos, (blockEntity) -> {
                    blockEntity.changeClosingDelay(decrease);
                });
            });
        });
    }

    private static void modifyKeypad(ServerPlayerEntity player, BlockPos pos, Consumer<KeypadLogicateBlockEntity> consumer) {
        ServerWorld world = (ServerWorld) player.world;

        if(world.isChunkLoaded(pos)) {
            KeypadLogicateBlockEntity blockEntity = (KeypadLogicateBlockEntity) player.world.getBlockEntity(pos);
            if(blockEntity == null) return;

            consumer.accept(blockEntity);
            blockEntity.markDirty();
            blockEntity.sync();
        }

    }
}