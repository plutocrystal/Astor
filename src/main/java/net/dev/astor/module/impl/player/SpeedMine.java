package net.dev.astor.module.impl.player;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.mixin.IAccessorPlayerControllerMP;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class SpeedMine extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty speed = new PercentProperty("Speed", 15);
    public final IntProperty delay = new IntProperty("Delay", 0, 0, 4);

    public SpeedMine() {
        super("SpeedMine", Category.PLAYER, false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (!mc.playerController.isInCreativeMode()) {
                if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
                    ((IAccessorPlayerControllerMP) mc.playerController)
                            .setBlockHitDelay(Math.min(((IAccessorPlayerControllerMP) mc.playerController).getBlockHitDelay(), this.delay.getValue() + 1));
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
