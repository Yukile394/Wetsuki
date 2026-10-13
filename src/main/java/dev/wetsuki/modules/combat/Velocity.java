package dev.wetsuki.modules.combat;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventPacket;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "Velocity",keywords = {"AKB","AntiKnockBack"}, type = Type.Combat, desc = "Disables knockback")
public class Velocity extends Function {
    public ModeSetting mode = new ModeSetting("Type", "Cancel", "Cancel");

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventPacket eventPacket) {
            if (mode.is("Cancel")) {
                if (eventPacket.getPacket() instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket) {
                    if (entityVelocityUpdateS2CPacket.getEntityId() == mc.player.getId()) {
                        eventPacket.setCancel(true);
                    }
                }
            }
        }
    }
}