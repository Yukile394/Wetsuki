package dev.wetsuki.events.impl.input;


import dev.wetsuki.events.Event;

public class EventKey extends Event {
    public int key;

    public EventKey(int key) {
        this.key = key;
    }
}
