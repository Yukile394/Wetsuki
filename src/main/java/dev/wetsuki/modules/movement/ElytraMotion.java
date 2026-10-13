package dev.wetsuki.modules.movement;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.events.impl.move.EventMotion;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.combat.AttackAura;
import dev.wetsuki.modules.setting.SliderSetting;

@FunctionAnnotation(name = "ElytraMotion", desc = "Freezes the player while flying on elytra and targeting", type = Type.Move)
public class ElytraMotion extends Function {
    private final SliderSetting distance = new SliderSetting("Working distance", 3.0F, 0.1F, 5.0F, 0.1F);

    private boolean shouldFreeze;
    private Vec3d freezePosition = Vec3d.ZERO;
    public ElytraMotion() {
        addSettings(distance);
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;
        if (event instanceof EventUpdate) {
            if (mc.player.isGliding()) {
                freezePosition = mc.player.getPos();
            }
            shouldFreeze = shouldFreeze();
        }

        if (event instanceof EventMotion motion && shouldFreeze) {
            mc.player.setPosition(freezePosition);
            mc.player.setVelocity(Vec3d.ZERO);

            motion.setX(0);
            motion.setY(0);
            motion.setZ(0);
        }
    }

    private boolean shouldFreeze() {
        if (!mc.player.isGliding()) return false;
        AttackAura aura = Manager.FUNCTION_MANAGER.attackAura;
        if (aura == null) return false;

        LivingEntity target = aura.target;
        if (target == null) return false;

        return target.distanceTo(mc.player) < distance.get().floatValue();
    }

    @Override
    public void onDisable() {
        shouldFreeze = false;
        super.onDisable();
    }
}
