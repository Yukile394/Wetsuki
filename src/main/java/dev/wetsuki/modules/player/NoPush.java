package dev.wetsuki.modules.player;

import dev.wetsuki.modules.setting.MultiSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import java.util.Arrays;

@FunctionAnnotation(name = "NoPush" ,desc  = "Removes collision from various sources", type = Type.Player)
public class NoPush extends Function {
    public MultiSetting mods = new MultiSetting(
            "Types",
            Arrays.asList("Players", "Blocks"),
            new String[]{"Water", "Players", "Blocks"}
    );
    public NoPush() {
        addSettings(mods);
    }

    @Override
    public void onEvent(Event event) {
    }
}
