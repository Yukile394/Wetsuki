package dev.wetsuki.modules.movement;

import net.minecraft.client.option.Perspective;
import dev.wetsuki.modules.setting.BindSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.input.EventKey;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.movement.freelook.FreeLookState;
import dev.wetsuki.manager.ClientManager;

@FunctionAnnotation(name = "FreeLook", desc = "Lets you rotate the camera without changing your movement direction", type = Type.Move)
public class FreeLook extends Function {
    private final BindSetting bind = new BindSetting("Key", 0);
    private Perspective previousPerspective;

    public FreeLook() {
        addSettings(bind);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventKey keyEvent)) return;
        if (keyEvent.key != bind.getKey()) return;
        if (mc == null || mc.options == null) return;
        var attackAura = Manager.FUNCTION_MANAGER.attackAura;
        if (attackAura != null && attackAura.state && attackAura.target != null) {
            ClientManager.message("Cannot be used with " + attackAura.name);
            return;
        }

        FreeLookState.active = !FreeLookState.active;

        if (FreeLookState.active) {
            previousPerspective = mc.options.getPerspective();
            if (previousPerspective != Perspective.THIRD_PERSON_FRONT) {
                mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
            }
        } else {
            mc.options.setPerspective(previousPerspective != null ? previousPerspective : Perspective.FIRST_PERSON);
        }
    }
    @Override
    public void onDisable() {
        FreeLookState.active = false;
        if (mc != null && mc.options != null) {
            mc.options.setPerspective(previousPerspective != null ? previousPerspective : Perspective.FIRST_PERSON);
        }
        previousPerspective = null;
    }

}