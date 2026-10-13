package dev.wetsuki.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventPacket;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.util.player.TimerUtil;

@SuppressWarnings("All")
@FunctionAnnotation(name = "GodMode", type = Type.Player, desc = "GodMode (copy-pasted junk)")
public class GodMode extends Function {
    TimerUtil timerUtil = new TimerUtil();
    private long lastClickTime = 0;
    private boolean firstClick = true;

    public GodMode() {
        addSettings();
    }

    public void onEvent(Event event) {
        if (event instanceof EventPacket eventPacket) {
            if (eventPacket.getPacket() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();

                if (message.contains("Вы не можете телепортироваться в PVP режиме") || message.contains("Вы успешно телепортированы на варп farm!")) {
                    eventPacket.setCancel(true);
                }
            }
        }
        if (event instanceof EventUpdate) {
            if (timerUtil.hasTimeElapsed(8000)) {
                ClientManager.message(Formatting.WHITE + "While GodMode is enabled" + Formatting.RED + " DO NOT " + Formatting.WHITE + "use any swaps");
            }
            timerUtil.reset();
            if (mc.currentScreen instanceof DeathScreen) {
                toggle();
            }
        }

        if (event instanceof EventUpdate) {
            if (ClientManager.playerIsPVP()) {
                long currentTime = System.currentTimeMillis();

                if (currentTime - lastClickTime >= 35) {
                    lastClickTime = currentTime;

                    MinecraftClient.getInstance().execute(() -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 1, 1, SlotActionType.PICKUP, mc.player));
                }

            }
        }
    }

    @Override
    public void onEnable() {
        firstClick = true;
        lastClickTime = System.currentTimeMillis();
        if (!ClientManager.playerIsPVP()) {
            mc.player.networkHandler.sendCommand("warp");

            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    MinecraftClient.getInstance().execute(() -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 21, 1, SlotActionType.PICKUP, mc.player));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try {
                    Thread.sleep(700);
                    MinecraftClient.getInstance().execute(() -> {
                        mc.currentScreen = null;
                        mc.mouse.lockCursor();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}