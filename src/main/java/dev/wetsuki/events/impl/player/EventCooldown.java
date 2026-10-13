package dev.wetsuki.events.impl.player;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.item.Item;
import dev.wetsuki.events.Event;

@Data
@EqualsAndHashCode(callSuper = true)
public class EventCooldown extends Event {
    public Item itemStack;
    public float cooldown;

    public EventCooldown(Item item) {
        this.itemStack = item;
    }
}
