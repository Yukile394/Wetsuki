package dev.wetsuki.modules.misc;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.modules.setting.BindSetting;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.input.EventKey;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.util.player.InventoryUtil;
import dev.wetsuki.util.player.TimerUtil;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("All")
@FunctionAnnotation(name = "HWHelper", desc = "Quick item interaction on HollyWorld", type = Type.Misc)
public class HWHelper extends Function {

    private final BindSetting trapka = new BindSetting("Trap key", 0);
    private final BindSetting trapkaBax = new BindSetting("Explosive trap key", 0);
    private final BindSetting stan = new BindSetting("Stun key", 0);
    private final BindSetting snow = new BindSetting("Snowball key", 0);
    private final BindSetting babax = new BindSetting("Explosive item key", 0);

    private final BooleanSetting bypass = new BooleanSetting("Bypass", true, "Slows you down when swapping");
    private final BooleanSetting inventoryUse = new BooleanSetting("Use from inventory", true);

    private final TimerUtil timer = new TimerUtil();
    private boolean bypassActive = false;
    private boolean awaitingSwap = false;

    private int hotbarSlot = -1;
    private int invSlot = -1;

    private final Map<BindSetting, Item> binds = new LinkedHashMap<>();

    public HWHelper() {
        addSettings(trapka, trapkaBax, stan, snow, babax, bypass, inventoryUse);
        binds.put(trapka, Items.POPPED_CHORUS_FRUIT);
        binds.put(trapkaBax, Items.PRISMARINE_SHARD);
        binds.put(stan, Items.NETHER_STAR);
        binds.put(snow, Items.SNOWBALL);
        binds.put(babax, Items.FIRE_CHARGE);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventKey eventKey) {
            handleKey(eventKey.key);
        }

        if (event instanceof EventUpdate) {
            handleBypass();
        }
    }

    private void handleKey(int pressedKey) {
        for (Map.Entry<BindSetting, Item> entry : binds.entrySet()) {
            if (pressedKey == entry.getKey().getKey()) {
                int[] slots = findSlots(entry.getValue());

                if (bypass.get()) {
                    timer.reset();
                    bypassActive = true;
                    awaitingSwap = true;
                    hotbarSlot = slots[0];
                    invSlot = slots[1];
                } else {
                    InventoryUtil.use(slots[0], slots[1], inventoryUse.get());
                }
                return;
            }
        }
    }

    private void handleBypass() {
        if (!bypassActive) return;

        setMovementKeys(false);

        if (awaitingSwap && timer.hasTimeElapsed(90)) {
            awaitingSwap = false;
            if (hotbarSlot != -1 || invSlot != -1) {
                InventoryUtil.use(hotbarSlot, invSlot, inventoryUse.get());
            }
        }

        if (timer.hasTimeElapsed(150)) {
            bypassActive = false;
            awaitingSwap = false;
            setMovementKeys(true);
        }
    }

    private int[] findSlots(Item item) {
        if (mc.player == null) return new int[]{-1, -1};

        int hotbarSlot = -1;
        int inventorySlot = -1;

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || stack.getItem() != item) continue;

            if (i < 9) hotbarSlot = i;
            else inventorySlot = i;

            if (hotbarSlot != -1 && inventorySlot != -1) break;
        }
        return new int[]{hotbarSlot, inventorySlot};
    }

    private void setMovementKeys(boolean restore) {
        KeyBinding[] keys = {
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.sprintKey
        };

        for (KeyBinding key : keys) {
            if (restore) {
                updateKeyBinding(key);
            } else {
                key.setPressed(false);
            }
        }
    }

    private void updateKeyBinding(KeyBinding keyMapping) {
        keyMapping.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyMapping.getDefaultKey().getCode()));
    }
}
