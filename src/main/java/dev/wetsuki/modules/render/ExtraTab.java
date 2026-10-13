package dev.wetsuki.modules.render;

import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "ExtraTab",desc  = "Increases the number of players in the tab list", type = Type.Render)
public class ExtraTab extends Function {

    @Override
    public void onEvent(Event event) {

    }
}