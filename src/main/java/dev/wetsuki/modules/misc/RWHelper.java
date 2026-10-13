package dev.wetsuki.modules.misc;

import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.move.EventMotion;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.BindBooleanSetting;
import dev.wetsuki.util.move.MoveUtil;

@SuppressWarnings("All")
@FunctionAnnotation(name = "RWHelper", desc = "", type = Type.Misc)
public class RWHelper extends Function {

    private final BindBooleanSetting dragonFly = new BindBooleanSetting("DragonFly", "Lets you fly faster on a dragon", true);

    public RWHelper() {
        addSettings(dragonFly);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventMotion)) return;
        if (!dragonFly.get() || !mc.player.getAbilities().flying) return;
        MoveUtil.setSpeed(1);

        float y = 0;

        boolean noForward = mc.player.forwardSpeed == 0 && !mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed();

        if (mc.options.jumpKey.isPressed()) y = noForward ? 0.5F : 0.25F;
        else if (mc.options.sneakKey.isPressed()) y = noForward ? -0.5F : -0.25F;

        mc.player.setVelocity(mc.player.getVelocity().x, y, mc.player.getVelocity().z);
    }
}
