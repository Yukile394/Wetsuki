package dev.wetsuki.mixin.player;

import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.wetsuki.WetsukiClient;
import dev.wetsuki.manager.IMinecraft;

@Mixin(Keyboard.class)
public class MixinKeyBoard implements IMinecraft {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action == 1 && !(mc.currentScreen instanceof Screen)) {
            // ESKI HATA: burada her tusa basista "new WetsukiClient()" yapiliyordu.
            // Bu, singleton'i eziyor, init()'in tekrar calismasina ve yukleme
            // ekraninda (Manager'lar henuz null iken) NPE ile cokmeye yol aciyordu.
            WetsukiClient main = WetsukiClient.getInstance();
            if (main != null && main.isReady()) {
                main.keyPress(key);
            }
        }
    }
}