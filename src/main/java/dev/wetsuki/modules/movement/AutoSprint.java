package dev.wetsuki.modules.movement;

import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "AutoSprint" ,desc  = "Automatically enables sprint", type = Type.Move)
public class AutoSprint extends Function {
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            mc.options.sprintKey.setPressed(true);
        }
    }

    @Override
    protected void onDisable() {
        mc.options.sprintKey.setPressed(false);
        super.onDisable();
    }
}