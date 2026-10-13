package dev.wetsuki.mixin.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.commandManager.CommandManager;


@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void sendChatMessageHook(@NotNull String message, CallbackInfo ci) {
        if (!ClientManager.legitMode) {
            CommandManager commandManager = Manager.COMMAND_MANAGER;
            if (message.startsWith(commandManager.getPrefix())) {
                try {
                    commandManager.getDispatcher().execute(message.substring(commandManager.getPrefix().length()), commandManager.getSource());
                } catch (CommandSyntaxException ignored) {
                }
                ci.cancel();
            }
        }
    }
}