package dev.wetsuki.modules.combat;


import dev.wetsuki.events.Event;
import dev.wetsuki.modules.Function;
import dev.wetsuki.modules.FunctionAnnotation;
import dev.wetsuki.modules.Type;

@FunctionAnnotation(name = "NoFriendDamage", keywords = {"NFD","FriendDamage"}, type = Type.Combat, desc = "Disables damage to friends")
public class NoFriendDamage extends Function {
    public NoFriendDamage() {
        addSettings();
    }
    @Override
    public void onEvent(Event event) {
    }
}
