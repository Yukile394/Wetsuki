package dev.wetsuki.modules.render;


import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.SliderSetting;

@FunctionAnnotation(name = "AspectRatio" ,desc  = "Lets you change the screen aspect ratio", type = Type.Render)
public class AspectRatio extends Function {
    public final ModeSetting mods = new ModeSetting("Mode","16:9","4:3","16:9","1:1","16:10","Custom");
    public final SliderSetting slider = new SliderSetting("Ratio", 1.8f, 0.1f, 5.0f,0.1f,() -> mods.is("Custom"));

    public AspectRatio() {
        addSettings(mods,slider);
    }

    @Override
    public void onEvent(Event event) {
    }

}
