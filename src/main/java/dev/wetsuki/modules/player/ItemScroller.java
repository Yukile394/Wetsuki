package dev.wetsuki.modules.player;

import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "ItemScroll", desc = "Fast movement", type = Type.Player)
public class ItemScroller extends Function {
    public SliderSetting scroll = new SliderSetting("Delay", 100f, 1f, 100f,1f);

    public ItemScroller() {
        addSettings(scroll);
    }

    @Override
    public void onEvent(Event event) {

    }
}