package dev.wetsuki.modules.render;

import org.lwjgl.glfw.GLFW;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.MultiSetting;
import dev.wetsuki.modules.setting.SliderSetting;

import java.awt.*;
import java.util.Arrays;

@FunctionAnnotation(name = "ClickGUI" ,desc  = "GUI control/customization", type = Type.Render, key = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGUI extends Function {
    public final ModeSetting colorGUI = new ModeSetting("Theme","Light black","Light black","Dark");

    public final SliderSetting alpha = new SliderSetting("Transparency",200f,120f,255f,1f);
    public final BooleanSetting blur = new BooleanSetting("Blur",true);
    public final MultiSetting blurSetting = new MultiSetting(
            () -> blur.get(),
            "Elements",
            Arrays.asList("Search","Panels"),
            new String[]{"Search","Themes", "Panels", "Description","Theme creation"}
    );
    public final BooleanSetting strike = new BooleanSetting("Module outline",true);
    public final BooleanSetting filling = new BooleanSetting("Module fill",true);
    public final SliderSetting rounding = new SliderSetting("Rounding",4,0,6,1,() -> strike.get() || filling.get());
    public final SliderSetting alphaModules = new SliderSetting("Module transparency",20f,10f,40f,1f,() -> strike.get() || filling.get());

    public ClickGUI() {
        addSettings(colorGUI,alpha,blur,blurSetting,strike,filling,rounding,alphaModules);
    }

    public Color getGuiColor() {
        switch (colorGUI.get()) {
            case "Dark":
                return new Color(0, 0, 0, alpha.get().intValue());
            case "Light black":
            default:
                return new Color(17, 15, 28, alpha.get().intValue());
        }
    }

    @Override
    public void onEvent(Event event) {}

    @Override
    public void onEnable() {
        setState(false);
        super.onEnable();
    }
}