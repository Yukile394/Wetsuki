package dev.wetsuki.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.wetsuki.WetsukiClient;
import dev.wetsuki.manager.*;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements IMinecraft {
    @Inject(method = "isMultiplayerEnabled", at = @At("HEAD"), cancellable = true)
    private void isMultiplayerEnabled(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void getWindowTitle(CallbackInfoReturnable<String> cir) {
        if (!ClientManager.legitMode) {
            cir.setReturnValue("WetsukiClient 1.21.4 Fabric | " + Manager.USER_PROFILE.getName());
        }
    }
    @Inject(at = @At("HEAD"), method = "stop")
    private void stop(CallbackInfo ci) {
        WetsukiClient.getInstance().shutDown();
    }
}
