package dev.wetsuki.modules.player;

import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "NoInteract", desc = "Stops you from opening containers with right click", type = Type.Player)
public class NoInteract extends Function {
    public final BooleanSetting onlyAura = new BooleanSetting("Only with AttackAura",false);

    public NoInteract() {
        addSettings(onlyAura);
    }
    @Override
    public void onEvent(Event event) {

    }
}