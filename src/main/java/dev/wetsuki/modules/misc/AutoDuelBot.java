package dev.wetsuki.modules.misc;

import dev.wetsuki.events.Event;
import dev.wetsuki.events.impl.EventPacket;
import dev.wetsuki.events.impl.EventUpdate;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;
import dev.wetsuki.modules.setting.ModeSetting;
import dev.wetsuki.modules.setting.SliderSetting;
import dev.wetsuki.util.player.TimerUtil;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;

@FunctionAnnotation(name = "AutoDuelBot", type = Type.Misc,desc = "Test")
public class AutoDuelBot extends Function {
    private final ModeSetting chat = new ModeSetting("Chat", "Local", "Local", "Global");
    private final SliderSetting fromMoney = new SliderSetting("Minimum coins", 1000, 1000, 1000000, 1000);
    private final SliderSetting beforeMoney = new SliderSetting("Maximum coins", 1000000, 1000, 1000000, 1000);
    private final SliderSetting messageDelay = new SliderSetting("Message delay", 5000L, 3000L, 30000L, 1000L);

    private final TimerUtil timerUtil = new TimerUtil();

    private String lastNick = null;
    private int lastBet = -1;


    public AutoDuelBot() {
        addSettings(chat, fromMoney, beforeMoney, messageDelay);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (timerUtil.hasTimeElapsed(messageDelay.get().longValue())) {
                String msg = "all Send a duel request from " + fromMoney.get().intValue() + " to " + beforeMoney.get().intValue() + " coins";
                if (chat.is("Global")) {
                    msg = "! " + msg;
                }
                mc.player.networkHandler.sendChatMessage(msg);
                timerUtil.reset();
            }


            if (lastNick != null && lastBet >= 0) {
                if (lastBet >= fromMoney.get().intValue() && lastBet <= beforeMoney.get().intValue()) {
                    mc.player.networkHandler.sendChatMessage("/duel accept " + lastNick);
                }
                lastNick = null;
                lastBet = -1;
            }

        } else if (event instanceof EventPacket eventPacket) {
            if (eventPacket.getPacket() instanceof GameMessageS2CPacket) {
                Text message = ((GameMessageS2CPacket) eventPacket.getPacket()).content();
                String text = message.getString();


                if (text.startsWith("➝ Ник: ")) {
                    lastNick = text.replace("➝ Ник: ", "").trim();
                } else if (text.startsWith("➝ Ставка: ")) {
                    try {
                        lastBet = Integer.parseInt(text.replace("➝ Ставка: ", "").trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }
}
