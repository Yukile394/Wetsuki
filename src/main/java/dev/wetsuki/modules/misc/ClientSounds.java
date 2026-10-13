package dev.wetsuki.modules.misc;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.MultiSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

import java.util.Arrays;

@FunctionAnnotation(name = "ClientSounds", desc = "Sounds", type = Type.Misc)
public class ClientSounds extends Function {
    public final MultiSetting check = new MultiSetting(
            "Select",
            Arrays.asList("Client join"),
            new String[]{"Client join"}
    );
    public final ModeSetting mode = new ModeSetting("Mode", "Type-1", "Type-1", "Type-2", "Type-3","Type-4");
    public final SliderSetting volume = new SliderSetting("Volume", 100f, 1f, 100f,1f);


    public ClientSounds() {
        addSettings(check,mode,volume);
    }

    @Override
    public void onEvent(Event event) {
    }
}
