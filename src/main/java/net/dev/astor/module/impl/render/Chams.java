package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.render.RenderLivingEvent;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.misc.Target;
import net.dev.astor.util.TeamUtil;
import net.dev.astor.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class Chams extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty players = new BooleanProperty("Players", true);
    public final BooleanProperty enemiess = new BooleanProperty("Enemies", true);
    public final BooleanProperty self = new BooleanProperty("Self", false);

    private boolean shouldRenderChams(EntityLivingBase entityLivingBase) {
        if (mc.getRenderViewEntity().getDistanceToEntity(entityLivingBase) > 512.0F) {
            return false;
        }
        if (entityLivingBase instanceof EntityPlayer) {
            if (entityLivingBase != mc.thePlayer && entityLivingBase != mc.getRenderViewEntity()) {
                return TeamUtil.isTarget((EntityPlayer) entityLivingBase) ? this.enemiess.getValue() : this.players.getValue();
            }
            return this.self.getValue() && mc.gameSettings.thirdPersonView != 0;
        }
        
        return !entityLivingBase.isInvisible() && Target.get().isTargetable(entityLivingBase);
    }

    @Override
    public String getDescription() {
        return "Renders entities through walls and with altered colours.";
    }

    public Chams() {
        super("Chams", Category.RENDER, false);
    }

    @EventTarget
    public void onRenderLiving(RenderLivingEvent event) {
        if (this.isEnabled()) {
            if (this.shouldRenderChams(event.getEntity())) {
                switch (event.getType()) {
                    case PRE:
                        GL11.glEnable(32823);
                        GL11.glPolygonOffset(1.0F, -2500000.0F);
                        break;
                    case POST:
                        GL11.glPolygonOffset(1.0F, 2500000.0F);
                        GL11.glDisable(32823);
                }
            }
        }
    }
}

