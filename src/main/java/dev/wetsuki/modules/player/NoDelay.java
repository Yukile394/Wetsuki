package dev.wetsuki.modules.player;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.mixin.iface.MinecraftClientAccessor;
import dev.wetsuki.mixin.iface.MixinLivingEntityAccessor;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.BooleanSetting;

@FunctionAnnotation(name = "NoDelay" ,desc  = "Removes item delay", type = Type.Player)
public class NoDelay extends Function {

    private final BooleanSetting jump = new BooleanSetting("Jump",true);
    private final BooleanSetting xp = new BooleanSetting("Experience Bottle",true);
    private final BooleanSetting crystal = new BooleanSetting("Crystals",true);
    private final BooleanSetting place = new BooleanSetting("RMB",false);

    public NoDelay() {
        addSettings(jump,xp,crystal,place);
    }


    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (jump.get()) {
                ((MixinLivingEntityAccessor) mc.player).setLastJumpCooldown(0);
            }
            if (check(mc.player.getMainHandStack().getItem()))
                ((MinecraftClientAccessor) mc).setUseCooldown(0);
        }
    }
    private boolean check(Item item) {
        return (item instanceof BlockItem && place.get()) || (item == Items.END_CRYSTAL && crystal.get()) || (item == Items.EXPERIENCE_BOTTLE && xp.get());
    }
}
