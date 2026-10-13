package dev.wetsuki.modules.misc;

import dev.wetsuki.modules.setting.BooleanSetting;
import dev.wetsuki.modules.setting.TextSetting;
import dev.wetsuki.events.Event;
import dev.wetsuki.manager.Manager;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "NameProtect", desc = "", type = Type.Misc)
public class NameProtect extends Function {
    public final TextSetting text = new TextSetting("Nickname","levin1337");
    public final BooleanSetting friend = new BooleanSetting("Hide friends",true);

    public NameProtect() {
        addSettings(text,friend);
    }
    public String getCustomName() {
        return Manager.FUNCTION_MANAGER.nameProtect.state ? text.getValue().replaceAll("&", "\u00a7") : mc.getGameProfile().getName();
    }
    public String getProtectedName(String originalName) {
        if (!Manager.FUNCTION_MANAGER.nameProtect.state) return originalName;

        if (isSelf(originalName)) {
            return applyFormatting(text.getValue());
        }

        if (friend.get() && Manager.FRIEND_MANAGER.isFriend(originalName)) {
            return applyFormatting(text.getValue());
        }

        return originalName;
    }
    private String applyFormatting(String name) {
        return name.replace('&', '§');
    }

    private boolean isSelf(String name) {
        return name.equals(mc.getSession().getUsername());
    }
    @Override
    public void onEvent(Event event) {

    }
}
