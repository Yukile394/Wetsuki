package dev.wetsuki.modules.player;

import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.ModeSetting;

@FunctionAnnotation(
        name = "ClickAction",
        keywords = "SwapAction",
        desc = "Sets swaps for the selected server type (to avoid bans)",
        type = Type.Player
)
public class ClickAction extends Function {

    public final ModeSetting type =
            new ModeSetting("Type", "ReallyWorld", "ReallyWorld", "FunTime", "HollyWorld");

    @Override
    public void onEvent(Event event) {
    }

    public final boolean nonBatch() {
        return type.is("ReallyWorld");
    }

    public final boolean batch() {
        return type.is("FunTime") || type.is("HollyWorld");
    }
}
