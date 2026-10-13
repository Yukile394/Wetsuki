package dev.wetsuki.modules.misc;

import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "NoCommands", desc = "Disables dot commands", type = Type.Misc)
public class NoCommands extends Function {
    public NoCommands() {
    }

    @Override
    public void onEvent(Event event) {

    }
}