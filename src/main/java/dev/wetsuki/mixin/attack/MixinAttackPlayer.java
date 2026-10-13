package dev.wetsuki.mixin.attack;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.player.EventAttack;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.util.math.RayTraceUtil;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinAttackPlayer {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    public void attackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (Manager.FUNCTION_MANAGER.noFriendDamage.state) {
            if (Manager.FRIEND_MANAGER.isFriend(target.getName().getString())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "attackEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", shift = At.Shift.AFTER, ordinal = 0))
    private void afterSendPacket(PlayerEntity player, Entity target, CallbackInfo ci) {
        Event.call(new EventAttack(player,target));
        RayTraceUtil.markHit(target);
    }
}