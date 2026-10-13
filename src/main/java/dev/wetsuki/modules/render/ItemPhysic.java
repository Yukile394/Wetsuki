package dev.wetsuki.modules.render;

import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.ModeSetting;

@FunctionAnnotation(name = "ItemPhysic",desc  = "Items lie nicely on the ground", type = Type.Render)
public class ItemPhysic extends Function {

    public final ModeSetting mode = new ModeSetting("Physics","Normal","Normal","2D");
    public ItemPhysic() {
        addSettings(mode);
    }

    @Override
    public void onEvent(Event event) {
    }
}