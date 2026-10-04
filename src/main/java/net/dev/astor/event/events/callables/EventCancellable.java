package net.dev.astor.event.events.callables;

import net.dev.astor.event.events.Cancellable;
import net.dev.astor.event.events.Event;

public abstract class EventCancellable implements Event, Cancellable {
    private boolean cancelled;

    protected EventCancellable() {
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean state) {
        cancelled = state;
    }
}