package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.movement.SafeWalkEvent;
import net.dev.astor.event.events.impl.player.UpdateEvent;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.player.Scaffold;
import net.dev.astor.util.ItemUtil;
import net.dev.astor.util.MoveUtil;
import net.dev.astor.util.PlayerUtil;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

public class SafeWalk extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty motion = new FloatProperty("Motion", 1.0F, 0.5F, 1.0F);
    public final FloatProperty speedMotion = new FloatProperty("SpeedMotion", 1.0F, 0.5F, 1.5F);
    public final BooleanProperty air = new BooleanProperty("Air", false);
    public final BooleanProperty directionCheck = new BooleanProperty("DirectionCheck", true);
    public final BooleanProperty pitCheck = new BooleanProperty("PitchCheck", true);
    public final BooleanProperty requirePress = new BooleanProperty("RequirePress", false);
    public final BooleanProperty blocksOnly = new BooleanProperty("BlocksOnly", true);

    private boolean canSafeWalk() {
        Scaffold scaffold = (Scaffold) Astor.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        } else if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        } else if (this.pitCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        } else if (this.blocksOnly.getValue() && !ItemUtil.isHoldingBlock()) {
            return false;
        } else {
            return (!this.requirePress.getValue() || mc.gameSettings.keyBindUseItem.isKeyDown()) && (mc.thePlayer.onGround && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -1.0)
                    || this.air.getValue() && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -2.0));
        }
    }

    @Override
    public String getDescription() {
        return "Stops you walking off the edge, by cancelling the motion that would carry you off.";
    }

    public SafeWalk() {
        super("SafeWalk", Category.MOVEMENT, false);
    }

    @EventTarget
    public void onMove(SafeWalkEvent event) {
        if (this.isEnabled()) {
            if (this.canSafeWalk()) {
                event.setSafeWalk(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && this.canSafeWalk()) {
                if (MoveUtil.getSpeedLevel() <= 0) {
                    if (this.motion.getValue() != 1.0F) {
                        MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.motion.getValue());
                    }
                } else if (this.speedMotion.getValue() != 1.0F) {
                    MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.speedMotion.getValue());
                }
            }
        }
    }
}
