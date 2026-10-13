package dev.wetsuki.modules.movement;

import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "Timer", type = Type.Move, desc = "Game speed-up")
public class Timer extends Function {

    private final SliderSetting timerAmount = new SliderSetting("Speed", 2, 0f, 10, 0.01f);
    private final BooleanSetting smart = new BooleanSetting("Smart", true);
    private final SliderSetting ticks = new SliderSetting("Decay speed", 3.8f, 0.15f, 5.0f, 0.1f,() -> smart.get());

    private float maxViolation = 100.0F;
    private float violation = 0.0F;
    private boolean isCooldown = false;

    public Timer() {
        addSettings(timerAmount, smart, ticks);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (!smart.get()) {
                ClientManager.TICK_TIMER = timerAmount.get().floatValue();
                return;
            }

            if (isCooldown) {
                violation -= ticks.get().floatValue();
                ClientManager.TICK_TIMER = 1.0F;

                if (violation <= 0) {
                    violation = 0;
                    isCooldown = false;
                }
            } else {
                ClientManager.TICK_TIMER = timerAmount.get().floatValue();
                violation += ticks.get().floatValue();

                if (violation >= maxViolation) {
                    violation = maxViolation;
                    isCooldown = true;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        ClientManager.TICK_TIMER = 1.0F;
        violation = 0.0F;
        isCooldown = false;
        super.onDisable();
    }

    @Override
    public void onEnable() {
        violation = 0.0F;
        isCooldown = false;
        super.onEnable();
    }
}