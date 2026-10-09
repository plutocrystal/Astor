package net.dev.astor.event.events.impl.render;

import net.dev.astor.event.events.Event;

public class GameLoopEvent implements Event {
    private float timerSpeed;

    public GameLoopEvent(float currentTimerSpeed) {
        this.timerSpeed = currentTimerSpeed;
    }

    public float getTimerSpeed() {
        return this.timerSpeed;
    }

    public void setTimerSpeed(float timerSpeed) {
        this.timerSpeed = timerSpeed;
    }
}