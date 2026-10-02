package net.dev.astor.module.impl.misc;

import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.event.types.EventType;
import net.dev.astor.mixin.IAccessorMinecraft;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.movement.NoFall;
import net.dev.astor.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Drives the game speed through Timer.timerSpeed, which runGameLoop multiplies into the amount of
 * ticks it runs per frame. Only the client's tick rate moves, movement input and the outgoing
 * packet cadence are untouched.
 */
public class Timer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    public final FloatProperty multiplier = new FloatProperty("Multiplier", 1.0F, 0.1F, 10.0F);

    public Timer() {
        super("Timer", Category.MISC, false);
    }

/**
 * NoFall reaches for the same field to halve the fall, so the multiplier is halved for as long
 * as that lasts instead of the two of them overwriting each other. A speed of zero would leave
 * elapsedPartialTicks below one forever and stop the game outright, hence the floor.
 */
    private float getSpeed() {
        NoFall noFall = (NoFall) Astor.moduleManager.modules.get(NoFall.class);
        float speed = this.multiplier.getValue() * (noFall != null && noFall.isSlowFalling() ? 0.5F : 1.0F);
        return Math.max(0.1F, speed);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = this.getSpeed();
        }
    }

    @Override
    public void onEnabled() {
        ((IAccessorMinecraft) mc).getTimer().timerSpeed = this.getSpeed();
    }

    @Override
    public void onDisabled() {
        ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
    }

    @Override
    public void verifyValue(String name) {
        if (this.isEnabled()) {
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = this.getSpeed();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.multiplier.getValue()) + "x"};
    }
}