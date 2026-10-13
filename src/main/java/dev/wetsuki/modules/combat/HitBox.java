package dev.wetsuki.modules.combat;

import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "HitBox", type = Type.Combat, desc = "Lets you enlarge player hitboxes")
public class HitBox extends Function {

    public SliderSetting size = new SliderSetting("Size", 0.4f, 0.1f, 5.5f, 0.1f);

    public HitBox() {
        addSettings(size);
    }
    @Override
    public void onEvent(Event event) {
    }

}