package dev.wetsuki.modules.movement;

import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.PlayerInput;
import org.lwjgl.glfw.GLFW;

@FunctionAnnotation(name = "SprintSneakToggle", desc = "Periodically toggles sprint and sneak state packets", type = Type.Move, key = GLFW.GLFW_KEY_KP_5)
public class SprintSneakToggle extends Function {

    private final BooleanSetting sprint = new BooleanSetting("Sprint", true);
    private final SliderSetting sprintDelay = new SliderSetting("Sprint delay", 2, 1, 20, 1, sprint::get);
    private final BooleanSetting sneak = new BooleanSetting("Sneak", true);
    private final SliderSetting sneakDelay = new SliderSetting("Sneak delay", 10, 1, 40, 1, sneak::get);

    private int tickCounter;
    private boolean sprintState;
    private boolean sneakState;

    public SprintSneakToggle() {
        addSettings(sprint, sprintDelay, sneak, sneakDelay);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventUpdate)) return;
        if (mc.player == null || mc.world == null || mc.player.networkHandler == null) return;

        tickCounter++;

        if (sprint.get() && tickCounter % sprintDelay.get().intValue() == 0) {
            sendSprint();
        }
        if (sneak.get() && tickCounter % sneakDelay.get().intValue() == 0) {
            sendSneak();
        }
    }

    private void sendSprint() {
        ClientPlayNetworkHandler handler = mc.player.networkHandler;
        sprintState = !sprintState;
        handler.sendPacket(new ClientCommandC2SPacket(mc.player,
                sprintState ? ClientCommandC2SPacket.Mode.START_SPRINTING
                            : ClientCommandC2SPacket.Mode.STOP_SPRINTING));
    }

    private void sendSneak() {
        sneakState = !sneakState;
        sendInput(sneakState);
    }

    /** Sends the real movement input with only the sneak flag replaced (1.21.2+ uses PlayerInputC2SPacket). */
    private void sendInput(boolean sneaking) {
        PlayerInput current = mc.player.input.playerInput;
        mc.player.networkHandler.sendPacket(new PlayerInputC2SPacket(new PlayerInput(
                current.forward(),
                current.backward(),
                current.left(),
                current.right(),
                current.jump(),
                sneaking,
                current.sprint()
        )));
    }

    @Override
    protected void onEnable() {
        tickCounter = 0;
        sprintState = mc.player != null && mc.player.isSprinting();
        sneakState = mc.player != null && mc.player.isSneaking();
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        // Put the server back in sync with the player's real state so nothing stays stuck.
        if (mc.player != null && mc.player.networkHandler != null) {
            if (sneak.get()) {
                sendInput(mc.player.input.playerInput.sneak());
            }
            if (sprint.get()) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player,
                        mc.player.isSprinting() ? ClientCommandC2SPacket.Mode.START_SPRINTING
                                                : ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }
        tickCounter = 0;
        sprintState = false;
        sneakState = false;
        super.onDisable();
    }
}
