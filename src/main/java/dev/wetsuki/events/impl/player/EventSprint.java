package dev.wetsuki.events.impl.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import dev.wetsuki.events.Event;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class EventSprint extends Event {
    private boolean sprinting;
}