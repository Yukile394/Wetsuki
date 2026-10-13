package dev.wetsuki.modules.player;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.MultiSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("All")
@FunctionAnnotation(name = "CustomCoolDown", type = Type.Player, desc = "Automatically sets item cooldowns")
public class CustomCoolDown extends Function {

    private final MultiSetting items = new MultiSetting(
            "Delay",
            Arrays.asList("Golden Apple", "Chorus Fruit", "Ender Pearl"),
            new String[]{"Golden Apple", "Chorus Fruit", "Ender Pearl"}
    );

    private final SliderSetting appleTime = new SliderSetting("Golden Apple cooldown", 4.6F, 1.0F, 16.0F, 0.1F, () -> items.get("Golden Apple"));
    private final SliderSetting pearlTime = new SliderSetting("Ender Pearl cooldown", 13.5F, 1.0F, 16.0F, 0.1F, () -> items.get("Ender Pearl"));
    private final SliderSetting horusTime = new SliderSetting("Chorus cooldown", 3.5F, 1.0F, 16.0F, 0.1F, () -> items.get("Chorus Fruit"));

    public BooleanSetting PVPonly = new BooleanSetting("Only in PVP mode", false);

    public final Map<Item, Long> lastUseMap = new HashMap<>();

    public CustomCoolDown() {
        addSettings(items, appleTime, pearlTime, horusTime, PVPonly);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventUpdate) || mc.player == null) return;
        if (PVPonly.get()) {
            if (!ClientManager.playerIsPVP()) {
                return;
            }
        }

        Iterator<Map.Entry<Item, Long>> iterator = lastUseMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Item, Long> entry = iterator.next();
            float delay = getCooldownForItem(entry.getKey()) * 1000F;
            if (System.currentTimeMillis() - entry.getValue() >= delay) {
                iterator.remove();
            }
        }

        if (mc.player.isUsingItem()) {
            ItemStack activeStack = mc.player.getActiveItem();
            Item item = activeStack.getItem();

            if (isItemEnabled(item) && lastUseMap.containsKey(item)) {
                mc.player.clearActiveItem();
            }
        }
    }


    public float getCooldownForItem(Item item) {
        if (item == Items.GOLDEN_APPLE) return ((Number) appleTime.get()).floatValue();
        if (item == Items.ENDER_PEARL) return ((Number) pearlTime.get()).floatValue();
        if (item == Items.CHORUS_FRUIT) return ((Number) horusTime.get()).floatValue();
        return 0;
    }

    public boolean isItemEnabled(Item item) {
        if (item == Items.GOLDEN_APPLE) return items.get("Golden Apple");
        if (item == Items.ENDER_PEARL) return items.get("Ender Pearl");
        if (item == Items.CHORUS_FRUIT) return items.get("Chorus Fruit");
        return false;
    }

    public void setCooldown(Item item) {
        lastUseMap.put(item, System.currentTimeMillis());
    }

}