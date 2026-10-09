package net.dev.astor.event.events.impl.render;

import net.dev.astor.event.events.Event;

/**
 * Fired once per frame from the head of Minecraft's game loop, which runs outside the runTick() loop.
 *
 * <p>That placement is the whole point. Timer.updateTimer() multiplies the tick accumulator by
 * timerSpeed, and runGameLoop runs one runTick() per accumulated tick, so a timerSpeed of zero pins
 * elapsedTicks at zero and the tick loop - with every TickEvent - stops dead. Anything that has to keep
 * making decisions while the clock is stopped, such as restoring that same clock, has to run here
 * instead.</p>
 *
 * <p>The speed is seeded with whatever it was when the event was dispatched, so a listener that does not
 * care leaves it untouched.</p>
 */
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