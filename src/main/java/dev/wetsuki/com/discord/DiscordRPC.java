package dev.wetsuki.com.discord;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface DiscordRPC extends Library {
    DiscordRPC INSTANCE = load();

    static DiscordRPC load() {
        try {
            return Native.load("discord-rpc", DiscordRPC.class);
        } catch (Throwable t) {
            System.err.println("[WetsukiClient] Discord RPC kutuphanesi yuklenemedi, Discord RPC ozelligi devre disi birakildi.");
            t.printStackTrace();
            return null;
        }
    }
    
    void Discord_UpdatePresence(final DiscordRichPresence p0);
    
    void Discord_Shutdown();
    
    void Discord_RunCallbacks();
    
    void Discord_Initialize(final String p0, final DiscordEventHandlers p1, final boolean p2, final String p3);
}
