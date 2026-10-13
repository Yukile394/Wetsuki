package dev.wetsuki.modules.player;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.input.EventKey;
import dev.wetsuki.events.impl.input.EventMouse;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.BindSetting;
import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.util.player.InventoryUtil;

@FunctionAnnotation(name = "MiddleClickPearl",keywords = {"MCP"}, desc = "Throw a pearl with the mouse wheel", type = Type.Player)
public class MiddleClickPearl extends Function {
    private final ModeSetting mode = new ModeSetting("Type","Normal","Normal","On keybind");
    private final BindSetting bind = new BindSetting("Throw key",0,() -> mode.is("On keybind"));
    private final BooleanSetting inventoryUse = new BooleanSetting("Use from inventory",true,"Don't use on HollyWorld (you get banned)");

    public MiddleClickPearl() {
        addSettings(mode,bind,inventoryUse);
    }

    @Override
    public void onEvent(Event event) {
        if (mode.is("Normal")) {
            if (event instanceof EventMouse mouseTick) {
                if (mouseTick.getButton() == 2) {
                    handleMouseTickEvent();
                }
            }
        }
        if (mode.is("On keybind")) {
            if (event instanceof EventKey e) {
                if (e.key == bind.getKey()) {
                    handleMouseTickEvent();
                }
            }
        }
    }

    private void handleMouseTickEvent() {
        if (!mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) {
            if (Manager.FUNCTION_MANAGER.attackAura.target != null) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false));
            }
            InventoryUtil.inventorySwapClick2(Items.ENDER_PEARL, inventoryUse.get(), true);
        }
    }
}
