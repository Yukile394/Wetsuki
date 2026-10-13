package dev.wetsuki.modules.render;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "FullBright",desc  = "Lights up the surroundings", type = Type.Render)
public class FullBright extends Function {
    private final StatusEffectInstance nightVisionEffect = new StatusEffectInstance(
            StatusEffects.NIGHT_VISION,
            -1,
            255,
            false,
            false,
            true
    );
    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
          mc.player.addStatusEffect(nightVisionEffect,mc.player);
        }
    }

    @Override
    public void onDisable() {
        mc.player.removeStatusEffect(nightVisionEffect.getEffectType());
        super.onDisable();
    }
}