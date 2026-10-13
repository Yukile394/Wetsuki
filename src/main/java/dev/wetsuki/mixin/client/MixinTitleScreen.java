package dev.wetsuki.mixin.client;

import net.minecraft.client.MinecraftClient;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.IMinecraft;
import dev.wetsuki.screens.mainmenu.MainMenu;
import dev.wetsuki.WetsukiClient;

@Mixin(TitleScreen.class)
public class MixinTitleScreen implements IMinecraft {
    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        try {
            // WetsukiClient.init() artik burada cagriliyor (MinecraftClient'in
            // constructor'inda DEGIL). TitleScreen sadece vanilla'nin ilk
            // resource/registry yuklemesi TAMAMEN basarili bittikten SONRA
            // acilir, bu yuzden burada calisan (potansiyel olarak biraz zaman
            // alan, ornegin font yukleme gibi) kod, oyunun kendi ic
            // kayit/registry doldurma zamanlamasini asla bozamaz.
            WetsukiClient client = WetsukiClient.getInstance();
            if (client == null) return;
            client.init();

            // Kurulum basarisiz olduysa (ready=false) ozel menuyu acma,
            // vanilla TitleScreen ile devam et; oyun acilsin.
            if (!client.isReady()) return;

            // NOT: Artik @At("HEAD") + cancel() yerine @At("RETURN")
            // kullaniyoruz. Boylece orijinal TitleScreen.init() KENDI
            // mantigini (ne yapiyorsa) TAMAMEN calistiriyor, biz sadece
            // O BITTIKTEN SONRA ekrani degistiriyoruz. Bu, vanilla'nin
            // init() icinde yaptigi herhangi bir kurulumu atlamamizi
            // engelleyerek daha guvenli bir yaklasim (ThunderHack gibi
            // bilinen/stabil projelerin kullandigi yontem).
            if (!ClientManager.legitMode) {
                mc.setScreen(new MainMenu());
            }
        } catch (Throwable t) {
            // Beklenmedik bir hata olsa bile orijinal TitleScreen'in
            // acilmasina izin ver, oyunu tamamen bozma.
            t.printStackTrace();
        }
    }
}
