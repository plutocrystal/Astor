package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.movement.StrafeEvent;
import net.dev.astor.event.events.impl.player.UpdateEvent;
import net.dev.astor.module.Module;
import net.dev.astor.util.KeyBindUtil;
import net.dev.astor.util.MoveUtil;
import net.dev.astor.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

public class Fly extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private double verticalMotion = 0.0;
    public final FloatProperty hSpeed = new FloatProperty("HorizontalSpeed", 1.0F, 0.0F, 100.0F);
    public final FloatProperty vSpeed = new FloatProperty("VerticalSpeed", 1.0F, 0.0F, 100.0F);

    public Fly() {
        super("Fly", Category.MOVEMENT, false);
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            if (mc.thePlayer.posY % 1.0 != 0.0) {
                mc.thePlayer.motionY = this.verticalMotion;
            }
            MoveUtil.setSpeed(0.0);
            event.setFriction((float) MoveUtil.getBaseMoveSpeed() * this.hSpeed.getValue());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.verticalMotion = 0.0;
            if (mc.currentScreen == null) {
                if (KeyBindUtil.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                    this.verticalMotion = this.verticalMotion + this.vSpeed.getValue().doubleValue() * 0.42F;
                }
                if (KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                    this.verticalMotion = this.verticalMotion - this.vSpeed.getValue().doubleValue() * 0.42F;
                }
                KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }
        }
    }

    @Override
    public void onDisabled() {
        mc.thePlayer.motionY = 0.0;
        MoveUtil.setSpeed(0.0);
        KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSneak.getKeyCode());
    }
}
