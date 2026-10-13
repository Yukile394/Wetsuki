package dev.wetsuki.modules.player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.input.EventMouse;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "MiddleClickFriend", keywords = {"MCF"}, desc = "Friend control", type = Type.Player)
public class MiddleClickFriend extends Function {
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMouse e) {
            if (e.getButton() == 2) {
                if (mc.crosshairTarget instanceof EntityHitResult entityHitResult) {
                    if (entityHitResult.getEntity() instanceof PlayerEntity player) {
                        final String name = player.getName().getString();
                        if (Manager.FRIEND_MANAGER.isFriend(name)) {
                            Manager.FRIEND_MANAGER.removeFriend(name);
                            ClientManager.message(Formatting.GRAY + name + Formatting.RED + " removed from friends");
                        } else {
                            Manager.FRIEND_MANAGER.addFriend(name);
                            ClientManager.message(Formatting.GRAY + name + Formatting.GREEN + " added to friends");
                        }
                    }
                }
            }
        }
    }
}
