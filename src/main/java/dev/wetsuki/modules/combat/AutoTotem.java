package dev.wetsuki.modules.combat;

import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import dev.wetsuki.events.impl.move.EventEntitySpawn;
import dev.wetsuki.events.impl.move.EventMotion;
import dev.wetsuki.mixin.player.MixinEntity;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.MultiSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.util.player.InventoryUtil;

import java.util.Arrays;

@SuppressWarnings("all")
@FunctionAnnotation(name = "AutoTotem", desc = "Takes a totem in hand at a certain health", type = Type.Combat)
public class AutoTotem extends Function {

    private final MultiSetting mode = new MultiSetting(
            "Take if",
            Arrays.asList("Crystal", "Player with a mace"),
            new String[]{"Crystal", "Player with a mace", "Creeper nearby", "Obsidian", "Anchor", "Fall", "Minecart"}
    );

    private final SliderSetting HPElytra = new SliderSetting("Take earlier on elytra", 5, 2, 6, 1);
    private final BooleanSetting back = new BooleanSetting("Return item", true);
    private final BooleanSetting noBallSwitch = new BooleanSetting("Don't take if holding ball", false);
    private final BooleanSetting saveEnchantedtotem = new BooleanSetting("Keep enchanted totems", true);
    private final BooleanSetting absorptionCheck = new BooleanSetting("+ Golden hearts", false);
    public final SliderSetting hp = new SliderSetting("Health", 4.5f, 2.0f, 20.0f, 0.1f);

    private final SliderSetting crystalDistance = new SliderSetting("Until crystal", 4, 2, 6, 1, () -> mode.get("Crystal"));
    private final SliderSetting anchorDistance = new SliderSetting("Until anchor", 4, 2, 6, 1, () -> mode.get("Anchor"));
    private final SliderSetting minecartDistance = new SliderSetting("Until minecart", 4, 2, 8, 1, () -> mode.get("Minecart"));
    private final SliderSetting obsidianDistance = new SliderSetting("Until obsidian", 4, 2, 8, 1, () -> mode.get("Obsidian"));

    private int item = -1;

    public AutoTotem() {
        addSettings(mode, hp, HPElytra, back, noBallSwitch, saveEnchantedtotem, absorptionCheck,
                crystalDistance, anchorDistance, minecartDistance, obsidianDistance);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventEntitySpawn spawnEvent) {
            Entity e = spawnEvent.getEntity();
            if (mode.get("Crystal") && e instanceof EndCrystalEntity) {
                if (mc.player != null && e.distanceTo(mc.player) <= crystalDistance.get().floatValue()) {
                    forceTotem();
                }
            }

            if (mode.get("Minecart") && e instanceof TntMinecartEntity) {
                if (mc.player != null && e.distanceTo(mc.player) <= minecartDistance.get().floatValue()) {
                    forceTotem();
                }
            }
        }
        if (event instanceof EventUpdate) {
            int slot = getTotemSlot();
            ItemStack offhand = mc.player.getOffHandStack();
            boolean hasTotemInHand = offhand.getItem() == Items.TOTEM_OF_UNDYING;

            if (condition()) {
                if (slot == -1) return;
                if (saveEnchantedtotem.get() && offhand.getItem() == Items.TOTEM_OF_UNDYING && offhand.hasEnchantments()) {
                    ItemStack candidate = mc.player.getInventory().getStack(slot);
                    if (candidate.getItem() == Items.TOTEM_OF_UNDYING && !candidate.hasEnchantments()) {
                        InventoryUtil.swapSlotsUniversal(slot, 40, false, true);
                        item = slot;
                        return;
                    }
                }

                if (!hasTotemInHand) {
                    InventoryUtil.swapSlotsUniversal(slot, 40, false, true);
                    if (item == -1) {
                        item = slot;
                    }
                }
            } else {
                if (item != -1 && back.get()) {
                    InventoryUtil.swapSlotsUniversal(item, 40, false, true);
                    item = -1;
                }
            }
        }
    }
    private void forceTotem() {
        int slot = getTotemSlot();
        if (slot == -1) return;

        ItemStack offhand = mc.player.getOffHandStack();
        if (offhand.getItem() != Items.TOTEM_OF_UNDYING) {
            InventoryUtil.swapSlotsUniversal(slot, 40, false, true);
            item = slot;
        }
    }


    private int getTotemSlot() {
        ItemStack offhand = mc.player.getOffHandStack();

        if (saveEnchantedtotem.get()) {
            if (offhand.getItem() == Items.TOTEM_OF_UNDYING && offhand.hasEnchantments()) {
                int normalTotem = findTotem(false);
                if (normalTotem != -1) return normalTotem;
                return -1;
            }

            int normalTotem = findTotem(false);
            if (normalTotem != -1) return normalTotem;

            int enchantedTotem = findTotem(true);
            if (enchantedTotem != -1) return enchantedTotem;
            return -1;
        }

        return InventoryUtil.getItemSlot(Items.TOTEM_OF_UNDYING);
    }

    private int findTotem(boolean enchanted) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                boolean hasEnchant = stack.hasEnchantments();
                if (enchanted == hasEnchant) return i;
            }
        }
        return -1;
    }

    private boolean condition() {
        final float absorption = absorptionCheck.get() && mc.player.hasStatusEffect(StatusEffects.ABSORPTION)
                ? mc.player.getAbsorptionAmount() : 0.0f;

        if (mc.player.getHealth() + absorption <= hp.get().floatValue()) return true;
        if (!isBall()) {
            if (crystal()) return true;
            if (anchor()) return true;
            if (macePlayer()) return true;
            if (creeper()) return true;
            if (obsidian()) return true;
        }

        return checkHPElytra() || checkFall();
    }

    private boolean checkFall() {
        if (!mode.get("Fall")) return false;
        if (mc.player.isGliding()) return false;
        return mc.player.fallDistance > 10.0f;
    }

    private boolean checkHPElytra() {
        return ((ItemStack) mc.player.getInventory().armor.get(2)).getItem() == Items.ELYTRA &&
                mc.player.getHealth() <= hp.get().floatValue() + HPElytra.get().floatValue();
    }

    private boolean isBall() {
        if (mode.get("Anchor") && mc.player.fallDistance > 5.0f) return false;
        return noBallSwitch.get() && mc.player.getOffHandStack().getItem() instanceof PlayerHeadItem;
    }

    private boolean anchor() {
        if (!mode.get("Anchor")) return false;
        return InventoryUtil.TotemUtil.getBlock((float) anchorDistance.get().floatValue(), Blocks.RESPAWN_ANCHOR) != null;
    }

    private boolean obsidian() {
        if (!mode.get("Obsidian")) return false;
        return InventoryUtil.TotemUtil.getBlock((float) obsidianDistance.get().floatValue(), Blocks.OBSIDIAN) != null;
    }

    private boolean creeper() {
        if (!mode.get("Creeper nearby")) return false;

        for (Entity entity : Manager.SYNC_MANAGER.getEntities()) {
            if (entity instanceof CreeperEntity creeper && mc.player.distanceTo(creeper) < 5.0f) {
                if (creeper.getClientFuseTime(0f) > 0f) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean macePlayer() {
        if (!mode.get("Player with a mace")) return false;

        for (PlayerEntity player : Manager.SYNC_MANAGER.getPlayers()) {
            if (player == mc.player) continue;

            boolean hasMace = player.getMainHandStack().getItem() == Items.MACE;
            double dy = player.getY() - mc.player.getY();
            double yVel = player.getVelocity().y;
            double distance = player.distanceTo(mc.player);
            boolean isAbove = dy > 1.5;
            boolean isInAir = !player.isOnGround() && !player.isTouchingWater() && !player.isClimbing();
            boolean fallingOrInAir = (yVel < -0.1 || yVel > 0.1) && isInAir;

            if (hasMace && isAbove && fallingOrInAir && distance < 24) {
                return true;
            }
        }
        return false;
    }

    private boolean crystal() {
        boolean checkCrystal = mode.get("Crystal");
        boolean checkMinecart = mode.get("Minecart");

        if (!checkCrystal && !checkMinecart) return false;
        for (Entity entity : Manager.SYNC_MANAGER.getEntities()) {
            if (checkCrystal && entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) < crystalDistance.get().floatValue()) {
                return true;
            }
            if (checkMinecart && entity instanceof TntMinecartEntity && mc.player.distanceTo(entity) < minecartDistance.get().floatValue()) {
                return true;
            }
        }
        return false;
    }

    private void reload() {
        item = -1;
    }

    @Override
    protected void onEnable() {
        reload();
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        reload();
        super.onDisable();
    }
}
