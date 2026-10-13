package dev.wetsuki.modules.misc;

import dev.wetsuki.events.Event;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "IRC", desc = "Chat between users of other clients", type = Type.Misc)
public class IRC extends Function {

    @Override
    public void onEvent(Event event) {
    }


    @Override
    protected void onDisable() {
        Manager.IRC_MANAGER.shutdown();
        super.onDisable();
    }
}