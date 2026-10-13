package dev.wetsuki.modules.player;

import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.move.EventMotion;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.util.move.MoveUtil;

@FunctionAnnotation(name = "HighJump", type = Type.Move)
public class HighJump extends Function {

    private final SliderSetting sila = new SliderSetting("Power", 2.0f, 0.0f, 5f, 0.1f);

    private boolean wasShulkerOpen = false;
    private long jumpStartTime = 0;

    private long guiOpenTime = 0;

    public HighJump() {
        addSettings(sila);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventMotion)) return;
        mc.player.setVelocity(mc.player.getVelocity().x, sila.get().floatValue(), mc.player.getVelocity().z);
        if (mc.options.sprintKey.isPressed()) {
            MoveUtil.setMotion(sila.get().floatValue());
        }
        long currentTime = System.currentTimeMillis();
        if (mc.currentScreen instanceof ShulkerBoxScreen && guiOpenTime == 0) {
            wasShulkerOpen = true;
            guiOpenTime = currentTime;
        }

        if (guiOpenTime != 0 && currentTime - guiOpenTime >= 800) {
            mc.player.closeScreen();
            mc.player.closeHandledScreen();
            guiOpenTime = 0;
        }

        if (wasShulkerOpen && mc.currentScreen == null) {
            wasShulkerOpen = false;
            jumpStartTime = currentTime;

            mc.player.setVelocity(mc.player.getVelocity().x, sila.get().floatValue(), mc.player.getVelocity().z);
            if (mc.options.sprintKey.isPressed()) {
                MoveUtil.setMotion(sila.get().floatValue());
            }
        }

        if (jumpStartTime != 0 && currentTime - jumpStartTime >= 3000) {
            jumpStartTime = 0;
        }
    }
}
