package dev.wetsuki.modules.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.events.impl.player.EventAttack;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.combat.AttackAura;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.modules.setting.TextSetting;
import dev.wetsuki.util.player.TimerUtil;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@FunctionAnnotation(name = "AutoMessage", type = Type.Player, desc = "Automatic message in chat or DM")
public class AutoMessage extends Function {

    private final ModeSetting mode = new ModeSetting(
            "Send",
            "After a kill",
            "After a kill",
            "While targeting",
            "By delay"
    );
    private final SliderSetting timer = new SliderSetting("Delay", 5000f, 0f, 35000f, 1000f,
            () -> mode.is("By delay") || mode.is("While targeting") || mode.is("msg(Test)"));
    private final TextSetting text = new TextSetting("Message", "Hi %target%!");

    private final TimerUtil delayTimer = new TimerUtil();
    private LivingEntity lastTarget;
    private boolean waitingForDeath = false;


    private final Pattern pattern = Pattern.compile(".*");

    public AutoMessage() {
        addSettings(mode, timer, text);
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null || mc.world == null) return;
        AttackAura aura = Manager.FUNCTION_MANAGER.attackAura;

        if (event instanceof EventAttack attackEvent && attackEvent.getTarget() instanceof LivingEntity entity) {
            if (mode.is("After a kill")) {
                lastTarget = entity;
                waitingForDeath = true;
            }
        }

        if (event instanceof EventUpdate) {
            if (mode.is("By delay")) {
                if (delayTimer.hasTimeElapsed(timer.get().longValue())) {
                    sendMessage(replaceTarget(text.getValue(), null));
                    delayTimer.reset();
                }
                return;
            }

            if (mode.is("While targeting")) {
                if (aura.target != null && delayTimer.hasTimeElapsed(timer.get().longValue())) {
                    sendMessage(replaceTarget(text.getValue(), aura.target));
                    delayTimer.reset();
                }
                return;
            }

            if (mode.is("After a kill") && waitingForDeath && lastTarget != null) {
                boolean dead = lastTarget.isDead() || lastTarget.getHealth() <= 0.0F;
                boolean unloaded = mc.world.getEntityById(lastTarget.getId()) == null;

                if (dead || unloaded) {
                    sendMessage(replaceTarget(text.getValue(), lastTarget));
                    waitingForDeath = false;
                    lastTarget = null;
                    delayTimer.reset();
                }
            }
        }
    }

    private void sendMessage(String msg) {
        if (msg == null || msg.trim().isEmpty()) return;
        mc.player.networkHandler.sendChatMessage(msg);
    }

    private String replaceTarget(String msg, LivingEntity target) {
        return msg.replace("%target%", target != null ? target.getName().getString() : "pig");
    }

    private List<String> getOnlinePlayers() {
        return mc.player.networkHandler.getPlayerList().stream()
                .map(PlayerListEntry::getProfile)
                .map(GameProfile::getName)
                .filter(profileName -> pattern.matcher(profileName).matches())
                .collect(Collectors.toList());
    }
}
