package dev.wetsuki.modules.combat;


import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventPacket;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "SuperBow", type = Type.Combat,desc = "Increases bow damage")
public class SuperBow extends Function {

    private final SliderSetting power = new SliderSetting("Power", 30, 1, 200, 1);

    public SuperBow() {
        addSettings(power);
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null || mc.world == null) return;

        if (event instanceof EventPacket e) {
            if (e.getPacket() instanceof PlayerActionC2SPacket p) {
                if (p.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                    mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player,ClientCommandC2SPacket.Mode.START_SPRINTING));
                    for (int i = 0; i < power.get().intValue(); i++) {
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 0.000000001, mc.player.getZ(), true,true));
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000000001, mc.player.getZ(), true,false));

                    }
                }
            }
        }
    }
}
