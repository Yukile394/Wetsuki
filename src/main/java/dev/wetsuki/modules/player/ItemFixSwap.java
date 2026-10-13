package dev.wetsuki.modules.player;

import net.minecraft.network.packet.c2s.play.SlotChangedStateC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventPacket;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(
        name = "ItemFixSwap",
        keywords = {"NoSlotChange", "NoServerDesync", "SlotFixer"},
        desc = "Removes anticheat slot switching",
        type = Type.Player
)
public class ItemFixSwap extends Function {
    public ItemFixSwap() {
        addSettings();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventPacket e) {
            if (e.isReceivePacket()) {
               if (e.getPacket() instanceof UpdateSelectedSlotC2SPacket packetHeldItemChange) {
                   event.setCancel(true);
               }
            }
        }
    }
}
