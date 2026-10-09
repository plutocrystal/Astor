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

/**
 * Draws the dot where the aim ray meets the target, so the point KillAura is actually aiming at is
 * visible instead of having to infer it from the silent rotation. The ray is AimRange long, so the
 * dot tracks the ray rather than the head: while the rotation is still turning onto the target the
 * ray has not reached the box yet, and the dot follows the ray instead of waiting to appear.
 */
public class DotRenderer {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final float DOT_OUTLINE_WIDTH = 1.5F;

    /**
     * How far in front of the camera the fallback marker has to sit. Anything nearer is drawn across the
     * crosshair rather than at the target - a cube a few tenths of a block from the eye fills the middle
     * of the screen and hides whatever the player is trying to aim at.
     */
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
        // Silent rotations never touch the camera, so the applied aim is the only honest direction to
        // trace from; Rotations None leaves the rotation to the player, so fall back to where they look.
        // Either way it is interpolated to the render partial ticks, because onUpdate only runs once
        // per tick - using the raw angles would step the dot 20 times a second no matter the fps.
        float yaw = this.owner.isAiming()
                ? RenderUtil.lerpAngle(this.owner.getAimYaw(), this.owner.getPrevAimYaw(), partialTicks)
                : RenderUtil.lerpAngle(mc.thePlayer.rotationYaw, mc.thePlayer.prevRotationYaw, partialTicks);
        float pitch = this.owner.isAiming()
                ? RenderUtil.lerpAngle(this.owner.getAimPitch(), this.owner.getPrevAimPitch(), partialTicks)
                : RenderUtil.lerpAngle(mc.thePlayer.rotationPitch, mc.thePlayer.prevRotationPitch, partialTicks);
        // Move the target box to its interpolated position first, so both ends of the ray agree on
        // which instant they describe. Shifting the hit point afterwards would only be a linear
        // correction and would leave the origin a tick behind the box it is measured against.
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

    /**
     * The point to mark: where the ray meets the box, or the point on the ray nearest it when the ray
     * has not arrived yet.
     */
    private Vec3 findPoint(float yaw, float pitch, float partialTicks, double dx, double dy, double dz) {
        AttackData data = this.owner.getTargetData();
        double reach = this.owner.aimRange.getValue().doubleValue();
        MovingObjectPosition hit = RotationUtil.rayTrace(data.getBox().offset(dx, dy, dz), yaw, pitch, reach, partialTicks);
        if (hit != null && hit.hitVec != null) {
            return hit.hitVec;
        }
        // The fallback is for a rotation that is swinging onto the target and has not arrived yet, so
        // it only applies while one is being produced. Aiming is false both when Rotations is None and
        // for the whole approach before the target is in swing range, and the ray is then just the
        // player's own view: the point on someone else's crosshair nearest the target can be a few
        // tenths of a block from the camera, which covers the middle of the screen. Off-aiming, only
        // a real hit is worth marking.
        if (!this.owner.isAiming()) {
            return null;
        }
        // No point on the ray sits far enough out - the target is behind the camera, or the aim has
        // barely started swinging towards it. Either way there is nothing to draw.
        return RotationUtil.nearestRayPoint(data.getBox().offset(dx, dy, dz), yaw, pitch, DOT_MIN_DISTANCE, reach, partialTicks);
    }
}
