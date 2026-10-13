package dev.wetsuki.modules.combat;


import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;
import dev.wetsuki.modules.setting.BindSetting;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.input.EventKey;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.util.player.InventoryUtil;
import dev.wetsuki.util.player.TimerUtil;

@FunctionAnnotation(name = "AutoSwap", type = Type.Combat,desc = "Lets you swap items with a keybind")
public class AutoSwap extends Function {
    private final BindSetting itemSwapKey = new BindSetting("Swap item key", 0);
    private final ModeSetting firstItem = new ModeSetting("First item", "Shield", "Shield", "Apple", "Totem", "Ball", "Firework");
    private final ModeSetting secondItem = new ModeSetting("Second item", "Shield", "Shield", "Apple", "Totem", "Ball", "Firework");

    private final BooleanSetting swapSwordWithAxe = new BooleanSetting("Swap axe and sword", false);

    private final BooleanSetting funTimeAndHolyWorldBypass = new BooleanSetting("FT/HW bypass", false);
    private final TimerUtil timer = new TimerUtil();
    private boolean bypassActive = false;
    private boolean awaitingSwap = false;
    private int pendingSlot = -1;

    public AutoSwap() {
        addSettings(itemSwapKey, firstItem, secondItem, swapSwordWithAxe, funTimeAndHolyWorldBypass);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventKey eventKey && eventKey.key == itemSwapKey.getKey()) {
            Item itemA = getItem(firstItem.getIndex());
            Item itemB = getItem(secondItem.getIndex());
            if (itemA == null || itemB == null) {
                return;
            }

            int inventorySlot = findItemInInventory((mc.player.getOffHandStack().getItem() == itemA) ? itemB : itemA);
            if (inventorySlot == -1) {
                return;
            }

            if (funTimeAndHolyWorldBypass.get()) {
                timer.reset();
                bypassActive = true;
                awaitingSwap = true;
                pendingSlot = inventorySlot;
            } else {
                mc.interactionManager.clickSlot(0, (inventorySlot < 9) ? inventorySlot + 36 : inventorySlot, 40, SlotActionType.SWAP, mc.player);
                if (swapSwordWithAxe.get()) {
                    handleWeaponSwap();
                }
            }
        }

        if (bypassActive) {
               mc.options.forwardKey.setPressed(false);
               mc.options.backKey.setPressed(false);
               mc.options.leftKey.setPressed(false);
               mc.options.rightKey.setPressed(false);
               mc.options.sprintKey.setPressed(false);

            if (awaitingSwap && timer.hasTimeElapsed(90)) {
                awaitingSwap = false;

                if (pendingSlot != -1) {
                    mc.interactionManager.clickSlot(0, (pendingSlot < 9) ? pendingSlot + 36 : pendingSlot, 40, SlotActionType.SWAP, mc.player);
                    if (swapSwordWithAxe.get()) {
                        handleWeaponSwap();
                    }

                    pendingSlot = -1;
                }
            }

            if (timer.hasTimeElapsed(150)) {
                bypassActive = false;
                awaitingSwap = false;
                pendingSlot = -1;

             updateKeyBinding(mc.options.forwardKey);
                  updateKeyBinding(mc.options.backKey);
                  updateKeyBinding(mc.options.leftKey);
                  updateKeyBinding(mc.options.rightKey);
                  updateKeyBinding(mc.options.sprintKey);
            }
        }
    }

    private int findItemInInventory(Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (!itemStack.isEmpty() && itemStack.getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    private Item getItem(int index) {
        if (index == 0) {
            return Items.SHIELD;
        } else if (index == 1) {
            return Items.GOLDEN_APPLE;
        } else if (index == 2) {
            return Items.TOTEM_OF_UNDYING;
        } else if (index == 3) {
            return Items.PLAYER_HEAD;
        } else if (index == 4) {
            return Items.FIREWORK_ROCKET;
        } else {
            return null;
        }
    }

    private void handleWeaponSwap() {
        int swordSlot = InventoryUtil.getItem(SwordItem.class, true);
        if (swordSlot == -1) {
            swordSlot = InventoryUtil.getItem(SwordItem.class, false);
        }

        int axeSlot = InventoryUtil.getItem(AxeItem.class, true);
        if (axeSlot == -1) {
            axeSlot = InventoryUtil.getItem(AxeItem.class, false);
        }

        if (swordSlot != -1 && axeSlot != -1) {
            InventoryUtil.swapSlots(swordSlot, axeSlot);
        }
    }

    private void updateKeyBinding(KeyBinding keyMapping) {
        keyMapping.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyMapping.getDefaultKey().getCode()));
    }
}
