package dev.wetsuki.modules.misc;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import dev.wetsuki.modules.setting.BindSetting;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.input.EventKey;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.manager.ClientManager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.util.player.InventoryUtil;

@FunctionAnnotation(name = "ElytraHelper", desc = "Quick elytra interaction", type = Type.Misc)
public class ElytraHelper extends Function {

    private final BindSetting elytraKey = new BindSetting("Elytra key", 0);
    private final BindSetting fireworkKey = new BindSetting("Firework key", 0);
    private final BooleanSetting autoTakeoff = new BooleanSetting("Auto takeoff", true);

    private int takeoffTicks = 0;
    private boolean waitingToGlide = false;

    public ElytraHelper() {
        addSettings(elytraKey, fireworkKey, autoTakeoff);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate && mc.player != null) {
            if (autoTakeoff.get()) handleAutoTakeoff();
        }

        if (event instanceof EventKey e && mc.player != null) {
            ItemStack equipped = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (e.key == elytraKey.getKey()) {
                int chestPlateSlot = findChestplate();
                int elytraSlot = findItemSlot(Items.ELYTRA);

                if (equipped.getItem() == Items.ELYTRA) {
                    if (chestPlateSlot != -1) {
                        swapSlots(chestPlateSlot, 6);
                        ClientManager.message("Swapped to " + Formatting.AQUA + "chestplate");
                    } else {
                        swapSlots(elytraSlot == -1 ? 6 : elytraSlot, 6);
                        ClientManager.message(Formatting.YELLOW + "Elytra removed (no chestplates)");
                    }
                } else {
                    if (elytraSlot != -1) {
                        swapSlots(elytraSlot, 6);
                        ClientManager.message("Swapped to " + Formatting.RED + "elytra");
                    } else {
                        ClientManager.message(Formatting.RED + "Elytra not found!");
                    }
                }
            }

            if (e.key == fireworkKey.getKey() && equipped.getItem() == Items.ELYTRA) {
                InventoryUtil.inventorySwapClick2(Items.FIREWORK_ROCKET, true, false);
            }
        }
    }

    private void handleAutoTakeoff() {
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() != Items.ELYTRA) {
            waitingToGlide = false;
            takeoffTicks = 0;
            return;
        }

        if (mc.player.isGliding()) {
            waitingToGlide = false;
            takeoffTicks = 0;
            return;
        }
        if (mc.player.isOnGround() && !waitingToGlide) {
            mc.player.jump();
            waitingToGlide = true;
            takeoffTicks = 0;
            return;
        }

        if (waitingToGlide) {
            takeoffTicks++;

            if (takeoffTicks >= 2 && mc.player.getVelocity().y < -0.08 && !mc.player.isGliding()) {
                InventoryUtil.startFly();
                waitingToGlide = false;
                takeoffTicks = 0;
            }

            if (takeoffTicks > 10) {
                waitingToGlide = false;
                takeoffTicks = 0;
            }
        }
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }

    private int findChestplate() {
        Item[] chestplates = {
                Items.NETHERITE_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE,
                Items.LEATHER_CHESTPLATE
        };
        for (Item item : chestplates) {
            int slot = findItemSlot(item);
            if (slot != -1) return slot;
        }
        return -1;
    }

    private void swapSlots(int from, int armorSlot) {
        int slot = from < 9 ? from + 36 : from;
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, armorSlot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, mc.player);
    }

    @Override
    protected void onEnable() {
        ClientManager.message("Please set version 1.17 in ViaFabricPlus so swaps work correctly");
        super.onEnable();
    }
}
