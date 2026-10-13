package dev.wetsuki.modules.misc;

import dev.wetsuki.com.discord.DiscordEventHandlers;
import dev.wetsuki.com.discord.DiscordRPC;
import dev.wetsuki.com.discord.DiscordRichPresence;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.events.impl.render.EventRender2D;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "DiscordRPC", desc = "Discord activity", type = Type.Misc)
public class DiscordRCP extends Function {
    private final DiscordRPC rpc = DiscordRPC.INSTANCE;
    private volatile boolean started = false;
    private Thread thread;
    private final DiscordRichPresence presence = new DiscordRichPresence();

    @Override
    public void onEvent(Event event) {
        if (rpc == null) return; // Discord RPC kutuphanesi yuklenemedi, ozellik devre disi
        if (event instanceof EventUpdate) {
            startRpc();
        }
    }

    public synchronized void startRpc() {
        if (rpc == null || started) return;
        started = true;
        try {
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize("1384873696375603281", handlers, true, "");
            presence.startTimestamp = System.currentTimeMillis() / 1000L;
            presence.largeImageText = "https://t.me/wetsukiclient";

            updatePresenceFields();

            rpc.Discord_UpdatePresence(presence);
        } catch (Throwable t) {
            System.err.println("[WetsukiClient] Discord RPC baslatilamadi:");
            t.printStackTrace();
            started = false;
            return;
        }

        thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    rpc.Discord_RunCallbacks();

                    updatePresenceFields();

                    rpc.Discord_UpdatePresence(presence);

                    Thread.sleep(2000L);
                }
            } catch (InterruptedException ignored) {
            } catch (Throwable t) {
                System.err.println("[WetsukiClient] Discord RPC dongusunde hata olustu, durduruluyor:");
                t.printStackTrace();
            } finally {
                started = false;
            }
        }, "TH-RPC-Handler");
        thread.setDaemon(true);
        thread.start();
    }

    private void updatePresenceFields() {
        presence.details = "User: " + Manager.USER_PROFILE.getName();
        presence.state = "Role: " + Manager.USER_PROFILE.getRole();

        presence.button_label_1 = "Buy";
        presence.button_url_1 = "https://wetsukiclient.ru";
        presence.button_label_2 = "Telegram";
        presence.button_url_2 = "https://t.me/wetsukiclient";

        presence.largeImageKey = "https://api.wetsukiclient.ru/api/loader/discord.gif";
    }

    @Override
    public void onDisable() {
        started = false;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        if (rpc != null) {
            try {
                rpc.Discord_Shutdown();
            } catch (Throwable t) {
                System.err.println("[WetsukiClient] Discord RPC kapatilirken hata olustu:");
                t.printStackTrace();
            }
        }
    }
}
