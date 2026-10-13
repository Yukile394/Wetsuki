package dev.wetsuki.modules.misc;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.TextSetting;
import dev.wetsuki.util.player.TimerUtil;

@FunctionAnnotation(name = "TPLoot", desc = "Teleports to where items are lying", type = Type.Misc)
public class TPLoot extends Function {
    private final ModeSetting lootEnd = new ModeSetting("After looting", "Nothing", "Nothing", "/hub", "/spawn", "home", "Custom");
    private final TextSetting text = new TextSetting("Command", "/home home", () -> lootEnd.is("home"));
    private final TextSetting custom = new TextSetting("Command", "/test", () -> lootEnd.is("Custom"));

    private final TimerUtil timerUtil = new TimerUtil();
    private boolean check;

    public TPLoot() {
        addSettings(lootEnd, text, custom);
    }

    private boolean isValuable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof SwordItem ||
                item instanceof PlayerHeadItem ||
                item instanceof ArmorItem ||
                item == Items.TOTEM_OF_UNDYING ||
                item == Items.ENDER_PEARL ||
                item == Items.END_CRYSTAL ||
                item == Items.FIREWORK_ROCKET ||
                item == Items.ELYTRA ||
                item == Items.GOLDEN_APPLE ||
                item == Items.ENCHANTED_GOLDEN_APPLE ||
                item == Items.CLAY_BALL ||
                item == Items.MAGMA_CREAM ||
                item == Items.CRYING_OBSIDIAN;
    }

    private boolean isHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isValuable(stack)) return true;
        }
        return false;
    }

    private boolean isInv() {
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isValuable(stack)) return true;
        }
        return false;
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventUpdate)) return;

        for (Entity entity : Manager.SYNC_MANAGER.getEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;

            ItemStack stack = itemEntity.getStack();
            if (!isValuable(stack)) continue;

            double itemY = itemEntity.getY();
            BlockPos blockBelow = itemEntity.getBlockPos().down();

            if (!mc.world.getBlockState(blockBelow).isAir() && itemY - blockBelow.getY() <= 1.0) {
                double x = itemEntity.getX() + 0.5;
                double y = itemEntity.getY();
                double z = itemEntity.getZ() + 0.5;
                mc.player.networkHandler.sendPacket(
                        new PlayerMoveC2SPacket.Full(x, y, z, mc.player.getYaw(), mc.player.getPitch(), false, true)
                );

                check = true;
            }
        }

        if (check && timerUtil.hasTimeElapsed(100)) {
            if (isHotbar() || isInv()) {
                switch (lootEnd.get()) {
                    case "/hub" -> mc.player.networkHandler.sendChatMessage("/hub");
                    case "/spawn" -> mc.player.networkHandler.sendChatMessage("/spawn");
                    case "home" -> mc.player.networkHandler.sendChatMessage(text.getValue());
                    case "Custom" -> mc.player.networkHandler.sendChatMessage(custom.getValue());
                }
                check = false;
                timerUtil.reset();
            }
        }
    }
}
