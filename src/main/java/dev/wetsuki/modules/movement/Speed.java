package dev.wetsuki.modules.movement;

import dev.wetsuki.modules.combat.TargetStrafe;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.move.EventMotion;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.util.move.MoveUtil;
import dev.wetsuki.util.player.TimerUtil;

@FunctionAnnotation(name = "Speed", desc = "Helps you die faster", type = Type.Move)
public class Speed extends Function {

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla");

    private final SliderSetting speed = new SliderSetting("Speed",1f,0.1f,3f,0.1f);
    private final TimerUtil timerUtil = new TimerUtil();

    public Speed() {
        addSettings(mode,speed);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMotion eventMotion) {
            if (mc.player == null || mc.world == null) return;

            switch (mode.get()) {
                case "Vanilla" -> vanilla();
            }
        }
    }

    private void vanilla() {
        if (MoveUtil.isMoving() && !mc.player.isGliding()) {
            MoveUtil.setSpeed(speed.get().floatValue());
        }
    }


    @Override
    protected void onEnable() {
        TargetStrafe targetStrafe = Manager.FUNCTION_MANAGER.targetStrafe;
        if (targetStrafe.state) {
            targetStrafe.setState(false);
        }
        timerUtil.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        ClientManager.TICK_TIMER = 1.0f;
        super.onDisable();
    }
}
