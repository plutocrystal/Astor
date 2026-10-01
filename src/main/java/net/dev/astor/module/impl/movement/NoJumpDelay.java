package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.types.Priority;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.mixin.IAccessorEntityLivingBase;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.IntProperty;
import net.minecraft.client.Minecraft;

public class NoJumpDelay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty delay = new IntProperty("Delay", 3, 0, 8);

    public NoJumpDelay() {
        super("NoJumpDelay", Category.MOVEMENT, false);
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            ((IAccessorEntityLivingBase) mc.thePlayer)
                    .setJumpTicks(Math.min(((IAccessorEntityLivingBase) mc.thePlayer).getJumpTicks(), this.delay.getValue() + 1));
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.delay.getValue().toString()};
    }
}
