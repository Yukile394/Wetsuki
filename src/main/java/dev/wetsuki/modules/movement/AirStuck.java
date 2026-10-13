package dev.wetsuki.modules.movement;

import net.minecraft.util.math.Vec3d;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.move.EventMotion;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.BooleanSetting;

@FunctionAnnotation(name = "AirStuck", desc = "Lets you hover in the air", type = Type.Move)
public class AirStuck extends Function {
    private BooleanSetting packet = new BooleanSetting("Cancel movement",true,"Cancels the movement packet sent to the server");
    private Vec3d freezePosition = Vec3d.ZERO;

    public AirStuck() {
        addSettings(packet);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            freezePosition = mc.player.getPos();
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMotion eventMotion) {
            if (mc.player != null && freezePosition != Vec3d.ZERO) {
                if (packet.get()) {
                    eventMotion.setCancel(true);
                }
                mc.player.setPosition(freezePosition);
                mc.player.setVelocity(Vec3d.ZERO);
            }
        }
    }
}