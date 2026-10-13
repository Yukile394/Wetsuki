package dev.wetsuki.modules.player;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.TextSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventPacket;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "AutoRespawn" ,desc  = "Automatically respawns you on death", type = Type.Player)
public class AutoRespawn extends Function {
    private BooleanSetting autohome = new BooleanSetting("Automatically teleport home",true);
    private TextSetting home = new TextSetting("Home point name","home",() -> autohome.get());

    public AutoRespawn() {
        addSettings(autohome,home);
    }
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventPacket eventPacket) {
            if (eventPacket.getPacket() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();

                if (message.contains("Вы были убиты") || message.contains("убиты") || message.contains("потеряно монет") || message.contains("потеряно") || message.contains("насмерть")) {
                    if (autohome.get()) {
                        mc.player.networkHandler.sendChatMessage("/home " + home.getValue());;
                    }
                }
            }
        }

        if (event instanceof EventUpdate eventUpdate) {
            if (mc.currentScreen instanceof DeathScreen) {
                mc.player.requestRespawn();
                mc.setScreen(null);
            }
        }
    }
}
