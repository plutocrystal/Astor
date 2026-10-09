package net.dev.astor.module.impl.player;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.mixin.attack.IAccessorPlayerControllerMP;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class SpeedMine extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty speed = new PercentProperty("Speed", 15);
    /**
     * Milliseconds of mining cooldown to enforce.
     *
     * <p>{@code blockHitDelay} counts ticks, so the value is divided by 50 where it is written.
     * Anything under 50ms floors to zero and the +1 tick vanilla always needs still applies, so the
     * floor is one tick rather than zero.</p>
     */
    public final IntProperty delay = new IntProperty("Delay", 0, 0, 200);

    @Override
    public String getDescription() {
        return "Speeds up block breaking by raising how fast the damage accumulates.";
    }

    public SpeedMine() {
        super("SpeedMine", Category.PLAYER, false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (!mc.playerController.isInCreativeMode()) {
                if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
                    ((IAccessorPlayerControllerMP) mc.playerController)
                            .setBlockHitDelay(Math.min(((IAccessorPlayerControllerMP) mc.playerController).getBlockHitDelay(), this.delay.getValue() / 50 + 1));
                    if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) {
                        float curBlockDamageMP = ((IAccessorPlayerControllerMP) mc.playerController).getCurBlockDamageMP();
                        float damage = 0.3F * (this.speed.getValue().floatValue() / 100.0F);
                        if (curBlockDamageMP < damage) {
                            ((IAccessorPlayerControllerMP) mc.playerController).setCurBlockDamageMP(damage);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%d%%", this.speed.getValue())};
    }
}
