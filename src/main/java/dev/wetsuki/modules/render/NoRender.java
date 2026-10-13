package dev.wetsuki.modules.render;

import dev.wetsuki.modules.setting.MultiSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

import java.util.Arrays;

@FunctionAnnotation(name = "NoRender", type = Type.Render, desc = "Removes various overlays from the screen")
public class NoRender extends Function {
    public MultiSetting mods = new MultiSetting(
            "Remove",
            Arrays.asList("Camera shake", "Fire overlay", "Water overlay","Suffocation","Bad effects"),
            new String[]{"Camera shake", "Fire overlay", "Water overlay", "Suffocation", "Scoreboard","Bad effects"}
    );

    public NoRender() {
        addSettings(mods);
    }
    @Override
    public void onEvent(Event event) {

    }
}