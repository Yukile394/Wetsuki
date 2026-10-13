package dev.wetsuki.modules.combat;

import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.input.EventKeyBoard;
import dev.wetsuki.events.impl.player.EventAttack;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.util.move.MoveUtil;

@FunctionAnnotation(name = "WTap", type = Type.Combat, keywords = {"ExtendedAttack","ExtendedKnockBack"}, desc = "Lets you knock the opponent back further")
public class AttackExtend extends Function {
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only on ground", true);

    public AttackExtend() {
        addSettings(onlyOnGround);
    }

    private int sprintResetTicks;

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventKeyBoard e && sprintResetTicks > 0 && MoveUtil.isMoving()) {
            e.setMovementForward(0);
            sprintResetTicks--;
        }

        if (event instanceof EventAttack && (!onlyOnGround.get() || mc.player.isOnGround()) && !mc.player.isInFluid() && mc.player.isSprinting()) {
            sprintResetTicks = 1;
        }
    }
}
