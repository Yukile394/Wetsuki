package dev.wetsuki.modules.misc;

import net.minecraft.entity.player.PlayerEntity;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.manager.apiManager.ClientAPI;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@FunctionAnnotation(name = "Globals", type = Type.Misc)
public class Globals extends Function {

    private ClientAPI clientAPI;
    private final int port = 13599;
    private String playerName;

    public final Map<UUID, Boolean> isClientUserCache = new ConcurrentHashMap<>();

    public Globals() {
        addSettings();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clientAPI = new ClientAPI("1.4.3", port);
        isClientUserCache.clear();

        if (mc.player != null) {
            playerName = mc.player.getGameProfile().getName();
            clientAPI.addPlayer(playerName);
            ClientManager.message("[Globals] ClientAPI enabled");
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clear();
        ClientManager.message("[Globals] ClientAPI disabled");
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventUpdate)) return;
        if (mc.player == null || clientAPI == null) return;

        if (mc.player.age % 30 == 0) {
            Set<UUID> active = ConcurrentHashMap.newKeySet();

            for (PlayerEntity player : Manager.SYNC_MANAGER.getPlayers()) {
                if (player == null) continue;

                UUID uuid = player.getUuid();
                active.add(uuid);

                String name = player.getGameProfile().getName();
                clientAPI.isClientUserAsync(name, result -> {
                    if (result) {
                        isClientUserCache.put(uuid, true);
                    } else {
                        isClientUserCache.remove(uuid);
                    }
                });
            }

            isClientUserCache.keySet().removeIf(uuid -> !active.contains(uuid));
        }
    }

    public void clear() {
        removePlayer();
        if (clientAPI != null)
            clientAPI.shutdown();
        clientAPI = null;
        isClientUserCache.clear();
    }
    private void removePlayer() {
        if (clientAPI != null && playerName != null) {
            clientAPI.removePlayer(playerName);
            playerName = null;
        }
    }
}
