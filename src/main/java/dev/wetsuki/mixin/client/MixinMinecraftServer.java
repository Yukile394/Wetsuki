package dev.wetsuki.mixin.client;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.wetsuki.WetsukiClient;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Inject(method = "shutdown", at = @At("HEAD"))
    public void shutdown(CallbackInfo ci) {
        WetsukiClient.getInstance().shutDown();
    }
}
