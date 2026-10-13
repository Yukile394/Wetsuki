package dev.wetsuki.mixin.player;

import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.wetsuki.WetsukiClient;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.input.EventMouse;
import dev.wetsuki.manager.IMinecraft;
import dev.wetsuki.manager.Manager;

@Mixin(Mouse.class)
public class MixinMouse implements IMinecraft {
    @Inject(method = "onMouseButton", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", ordinal = 0))
    private void beforeSpectatorCheck(long window, int button, int action, int mods, CallbackInfo ci) {
        boolean bl = action == 1;
        if (!bl) return;
        WetsukiClient main = WetsukiClient.getInstance();
        if (main == null || !main.isReady()) return;
        main.keyPress(-100 + button);

        Event.call(new EventMouse(button));
    }
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (Manager.DRAG_MANAGER != null && mc.currentScreen instanceof ChatScreen) {
            Manager.DRAG_MANAGER.draggables.values().forEach(dragging -> {
                if (dragging.getModule() != null && dragging.getModule().state) {
                    dragging.onRelease(button);
                }
            });
        }

    }
}