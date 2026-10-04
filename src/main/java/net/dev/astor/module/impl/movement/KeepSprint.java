package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty slowdown = new PercentProperty("Slowdown", 0);
    public final BooleanProperty groundOnly = new BooleanProperty("GroundOnly", false);
    public final BooleanProperty reachOnly = new BooleanProperty("ReachOnly", false);

    public KeepSprint() {
        super("KeepSprint", Category.MOVEMENT, false);
    }

    public boolean shouldKeepSprint() {
        if (this.groundOnly.getValue() && !mc.thePlayer.onGround) {
            return false;
        } else {
            return !this.reachOnly.getValue() || mc.objectMouseOver.hitVec.distanceTo(mc.getRenderViewEntity().getPositionEyes(1.0F)) > 3.0;
        }
    }
}