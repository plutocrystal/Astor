package net.dev.astor.event.events.callables;

import net.dev.astor.event.events.Event;
import net.dev.astor.event.events.Typed;

public abstract class EventTyped implements Event, Typed {
    private final byte type;

    protected EventTyped(byte eventType) {
        type = eventType;
    }

    @Override
    public byte getType() {
        return type;
    }
}