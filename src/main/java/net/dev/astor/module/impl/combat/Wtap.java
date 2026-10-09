package net.dev.astor.module.impl.combat;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.types.Priority;
import net.dev.astor.event.events.impl.movement.MoveInputEvent;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.module.Module;
import net.dev.astor.util.TimerUtil;
import net.dev.astor.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.potion.Potion;

public class Wtap extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    private boolean active = false;
    private boolean stopForward = false;
    private long delayTicks = 0L;
    private long durationTicks = 0L;
    /**
     * Milliseconds to hold the sprint before the forward tap starts.
     *
     * <p>{@link #delayTicks} is drained 50 per tick, so it counts milliseconds despite the name; the
     * value is added to it directly and the decrement does the tick conversion.</p>
     */
    public final FloatProperty delay = new FloatProperty("Delay", 275.0F, 0.0F, 500.0F, 0);

    /** Milliseconds the tap is held, added to {@link #durationTicks} the same way. */
    public final FloatProperty duration = new FloatProperty("Duration", 75.0F, 50.0F, 250.0F, 0);

    private boolean canTrigger() {
        return !(mc.thePlayer.movementInput.moveForward < 0.8F)
                && !mc.thePlayer.isCollidedHorizontally
                && (!((float) mc.thePlayer.getFoodStats().getFoodLevel() <= 6.0F) || mc.thePlayer.capabilities.allowFlying) && (mc.thePlayer.isSprinting()
                || !mc.thePlayer.isUsingItem() && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.gameSettings.keyBindSprint.isKeyDown());
    }

    @Override
    public String getDescription() {
        return "Releases and re-presses forward around your hits so they register as sprint hits.";
    }

    public Wtap() {
        super("WTap", Category.COMBAT, false);
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (this.active) {
            if (!this.stopForward && !this.canTrigger()) {
                this.active = false;
                while (this.delayTicks > 0L) {
                    this.delayTicks -= 50L;
                }
                while (this.durationTicks > 0L) {
                    this.durationTicks -= 50L;
                }
            } else if (this.delayTicks > 0L) {
                this.delayTicks -= 50L;
            } else {
                if (this.durationTicks > 0L) {
                    this.durationTicks -= 50L;
                    this.stopForward = true;
                    mc.thePlayer.movementInput.moveForward = 0.0F;
                }
                if (this.durationTicks <= 0L) {
                    this.active = false;
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && !event.isCancelled() && event.getType() == EventType.SEND) {
            if (event.getPacket() instanceof C02PacketUseEntity
                    && ((C02PacketUseEntity) event.getPacket()).getAction() == Action.ATTACK
                    && !this.active
                    && this.timer.hasTimeElapsed(500L)
                    && mc.thePlayer.isSprinting()) {
                this.timer.reset();
                this.active = true;
                this.stopForward = false;
                this.delayTicks = this.delayTicks + this.delay.getValue().longValue();
                this.durationTicks = this.durationTicks + this.duration.getValue().longValue();
            }
        }
    }
}
