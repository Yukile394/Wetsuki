package dev.wetsuki.modules.player;

import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "NoRayTrace",keywords = {"NoEntityTrace"}, desc = "Removes entity hitboxes", type = Type.Player)
public class NoRayTrace extends Function {

    @Override
    public void onEvent(Event event) {
    }
}