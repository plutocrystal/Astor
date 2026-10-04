package net.dev.astor.event.events;

public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean state);
}