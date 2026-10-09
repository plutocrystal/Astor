package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.types.Priority;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.mixin.player.IAccessorEntityLivingBase;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.IntProperty;
import net.minecraft.client.Minecraft;

public class NoJumpDelay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    /**
     * Milliseconds of jump cooldown to enforce.
     *
     * <p>{@code jumpTicks} counts ticks, so the value is divided by 50 where it is written. Anything
     * under 50ms floors to zero and the +1 tick vanilla always needs still applies, so the floor is
     * one tick rather than zero.</p>
     */
    public final IntProperty delay = new IntProperty("Delay", 0, 0, 400);

    @Override
    public String getDescription() {
        return "Shortens the cooldown between jumps.";
    }

    public NoJumpDelay() {
        super("NoJumpDelay", Category.MOVEMENT, false);
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            ((IAccessorEntityLivingBase) mc.thePlayer)
                    .setJumpTicks(Math.min(((IAccessorEntityLivingBase) mc.thePlayer).getJumpTicks(), this.delay.getValue() / 50 + 1));
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.delay.getValue().toString()};
    }
}
