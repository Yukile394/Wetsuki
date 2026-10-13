package dev.wetsuki.modules.combat;

import dev.wetsuki.events.Event;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.movement.Speed;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.SliderSetting;

@FunctionAnnotation(name = "TargetStrafe", type = Type.Combat,desc = "Strafe like in Nurik")
public class TargetStrafe extends Function {
    public final SliderSetting speedSlider = new SliderSetting("Speed",0.095f,0.01f,1.2f,0.01f);

    public final ModeSetting ptytag = new ModeSetting("Pull method","Vector","Vector","Motion / Velocity");
    public final SliderSetting blocks = new SliderSetting("Pull distance",7f,0.01f,12f,0.01f);
    public final SliderSetting hitbox = new SliderSetting("Boost hitbox",0.095f,0.01f,50.0f,0.01f);
    public final BooleanSetting predictCheck = new BooleanSetting("Predict",true);
    public final SliderSetting predict = new SliderSetting("Predict value",2.5f,0.1f,4.0f,0.1f,() -> predictCheck.get());

    public final BooleanSetting predictView = new BooleanSetting("Show predict",false,"On your screen you will appear to overtake the opponent");


    public TargetStrafe() {
        addSettings(speedSlider,ptytag,blocks,hitbox,predictCheck,predict,predictView);
    }

    @Override
    public void onEvent(Event event) {
    }
    @Override
    protected void onDisable() {
        if (mc.options.forwardKey.isPressed()) {
            mc.options.forwardKey.setPressed(false);
        }
        super.onDisable();
    }
    @Override
    public void onEnable() {
        Speed speed = Manager.FUNCTION_MANAGER.speed;
        if (speed.state) {
            speed.setState(false);
        }
        super.onEnable();
    }
}