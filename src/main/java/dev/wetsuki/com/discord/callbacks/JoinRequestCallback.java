package dev.wetsuki.com.discord.callbacks;

import com.sun.jna.Callback;
import dev.wetsuki.com.discord.DiscordUser;


public interface JoinRequestCallback extends Callback {
    void apply(final DiscordUser p0);
}
