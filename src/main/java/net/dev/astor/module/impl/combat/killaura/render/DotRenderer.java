package net.dev.astor.module.impl.combat.killaura.render;

import net.dev.astor.mixin.render.IAccessorRenderManager;
import net.dev.astor.module.impl.combat.killaura.target.AttackData;
import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.util.RenderUtil;
import net.dev.astor.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.awt.Color;

public class DotRenderer {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final float DOT_OUTLINE_WIDTH = 1.5F;

    private static final double DOT_MIN_DISTANCE = 0.5D;

    private final KillAura owner;

    public DotRenderer(KillAura owner) {
        this.owner = owner;
    }

    public void draw(float partialTicks) {
        if (!this.owner.isEnabled() || !this.owner.dot.getValue() || this.owner.getTargetData() == null || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        EntityLivingBase target = this.owner.getTargetData().getEntity();
        if (target == null || target.isDead || !this.owner.getTargetSelector().isInAimRange(target)) {
            return;
        }
        
        float yaw = this.owner.isAiming()
                ? RenderUtil.lerpAngle(this.owner.getAimYaw(), this.owner.getPrevAimYaw(), partialTicks)
                : RenderUtil.lerpAngle(mc.thePlayer.rotationYaw, mc.thePlayer.prevRotationYaw, partialTicks);
        float pitch = this.owner.isAiming()
                ? RenderUtil.lerpAngle(this.owner.getAimPitch(), this.owner.getPrevAimPitch(), partialTicks)
                : RenderUtil.lerpAngle(mc.thePlayer.rotationPitch, mc.thePlayer.prevRotationPitch, partialTicks);
        
        double dx = RenderUtil.lerpDouble(target.posX, target.lastTickPosX, partialTicks) - target.posX;
        double dy = RenderUtil.lerpDouble(target.posY, target.lastTickPosY, partialTicks) - target.posY;
        double dz = RenderUtil.lerpDouble(target.posZ, target.lastTickPosZ, partialTicks) - target.posZ;
        Vec3 point = this.findPoint(yaw, pitch, partialTicks, dx, dy, dz);
        if (point == null) {
            return;
        }
        double half = (double) this.owner.dotSize.getValue() / 2.0;
        AxisAlignedBB dot = new AxisAlignedBB(
                point.xCoord - half, point.yCoord - half, point.zCoord - half,
                point.xCoord + half, point.yCoord + half, point.zCoord + half
        );
        if (!RenderUtil.isInViewFrustum(dot, 0.0D)) {
            return;
        }
        dot = dot.offset(
                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
        );
        Color fill = new Color(this.owner.fillColor.getValue(), true);
        Color outline = new Color(this.owner.outlineColor.getValue(), true);
        RenderUtil.enableRenderState();
        RenderUtil.drawFilledBox(dot, fill.getRed(), fill.getGreen(), fill.getBlue(), fill.getAlpha());
        RenderUtil.drawBoundingBox(dot, outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha(), DOT_OUTLINE_WIDTH);
        RenderUtil.disableRenderState();
    }

    private Vec3 findPoint(float yaw, float pitch, float partialTicks, double dx, double dy, double dz) {
        AttackData data = this.owner.getTargetData();
        double reach = this.owner.aimRange.getValue().doubleValue();
        MovingObjectPosition hit = RotationUtil.rayTrace(data.getBox().offset(dx, dy, dz), yaw, pitch, reach, partialTicks);
        if (hit != null && hit.hitVec != null) {
            return hit.hitVec;
        }
        
        if (!this.owner.isAiming()) {
            return null;
        }
        
        return RotationUtil.nearestRayPoint(data.getBox().offset(dx, dy, dz), yaw, pitch, DOT_MIN_DISTANCE, reach, partialTicks);
    }
}

