package dev.wetsuki.modules.player;

import net.minecraft.text.Text;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.modules.setting.TextSetting;

@SuppressWarnings("All")
@FunctionAnnotation(name = "AutoLeave", desc = "Automatically leaves the server", type = Type.Player)
public class AutoLeave extends Function {

    private final ModeSetting mode = new ModeSetting("Mode",
            "Player nearby", "Player nearby", "Low HP");

    private final SliderSetting heal = new SliderSetting("Health",
            3, 1, 20, 1, () -> mode.is("Low HP"));

    private final SliderSetting radius = new SliderSetting("Distance to player",
            60, 20, 150, 1, () -> mode.is("Player nearby"));

    private final ModeSetting run = new ModeSetting("What to do",
            "Leave the server", "Leave the server", "/hub", "teleport home");

    private final TextSetting homeName = new TextSetting("Home point name",
            "home", () -> run.is("teleport home"));

    private final BooleanSetting pvpNoLeave =
            new BooleanSetting("Don't leave in PVP mode", true,
                    () -> run.is("Leave the server") || run.is("/hub"));

    private boolean triggered = false;

    public AutoLeave() {
        addSettings(mode, heal, radius, run, homeName, pvpNoLeave);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventUpdate)) return;
        if (mc.player == null || mc.world == null) return;

        if (pvpNoLeave.get() && ClientManager.playerIsPVP()) return;

        boolean shouldTrigger = false;

        if (mode.is("Low HP") && mc.player.getHealth() <= heal.get().floatValue()) {
            shouldTrigger = true;
        } else if (mode.is("Player nearby")) {
            shouldTrigger = Manager.SYNC_MANAGER.getPlayers().stream().anyMatch(other -> other != mc.player && mc.player.distanceTo(other) <= radius.get().floatValue());
        }

        if (shouldTrigger && !triggered) {
            executeAction();
            triggered = true;
        } else if (!shouldTrigger) {
            triggered = false;
        }
    }

    private void executeAction() {
        if (mc.player == null) return;

        if (run.is("Leave the server")) {
            mc.player.networkHandler.getConnection().disconnect(Text.literal("AutoLeave"));
        } else if (run.is("/hub")) {
            mc.player.networkHandler.sendChatCommand("hub");
        } else if (run.is("teleport home")) {
            mc.player.networkHandler.sendChatCommand("home " + homeName.getValue());
        }
        toggle();
    }
}
