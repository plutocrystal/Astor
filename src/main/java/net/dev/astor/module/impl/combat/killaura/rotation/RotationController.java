package net.dev.astor.module.impl.combat.killaura.rotation;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.combat.killaura.target.AttackData;
import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.util.RandomUtil;
import net.dev.astor.util.RotationUtil;
import net.dev.astor.management.RotationState;
import net.dev.astor.util.MoveUtil;
import net.dev.astor.event.events.impl.movement.MoveInputEvent;
import net.dev.astor.event.events.impl.player.UpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class RotationController {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final KillAura owner;

    private float aimYaw = 0.0F;
    private float aimPitch = 0.0F;
    private float prevAimYaw = 0.0F;
    private float prevAimPitch = 0.0F;
    private boolean aiming = false;

    private float yawVel = 0.0F;
    private float pitchVel = 0.0F;
    private float residYaw = 0.0F;
    private float residPitch = 0.0F;

    private final float[] yawLag = new float[3];
    private final float[] pitchLag = new float[3];
    private int lagIdx = 0;
    private int lagCount = 0;
    private float lagYaw = 0.0F;
    private float lagPitch = 0.0F;

    private float tremorYaw = 0.0F;
    private float tremorPitch = 0.0F;

    private static final float STIFFNESS = 0.55F;
    private static final float DAMPING = 0.72F;
    private static final float TREMOR_DECAY = 0.55F;
    private static final float TREMOR_INJECT = 0.40F;
    private static final float PHYSICAL_MAX_VEL = 90.0F;

    public RotationController(KillAura owner) {
        this.owner = owner;
    }

    public boolean isAiming() {
        return this.aiming;
    }

    public float getAimYaw() {
        return this.aimYaw;
    }

    public float getAimPitch() {
        return this.aimPitch;
    }

    public float getPrevAimYaw() {
        return this.prevAimYaw;
    }

    public float getPrevAimPitch() {
        return this.prevAimPitch;
    }

    public boolean applyAim(UpdateEvent event, AttackData target) {
        this.aiming = false;
        if (this.owner.rotations.getValue() == 0) {
            this.clearMotion();
            return false;
        }

        float[] rotations;
        if (this.owner.rotationMode.getValue() == 1) {
            rotations = this.computePhysicalRotations(target, event.getYaw(), event.getPitch());
        } else {
            rotations = this.computeBasicRotations(target, event.getYaw(), event.getPitch());
        }

        event.setRotation(rotations[0], rotations[1], 1);
        this.prevAimYaw = this.aimYaw;
        this.prevAimPitch = this.aimPitch;
        this.aimYaw = rotations[0];
        this.aimPitch = rotations[1];
        this.aiming = true;

        if (this.owner.rotations.getValue() == 2) {
            Astor.rotationManager.setRotation(rotations[0], rotations[1], 1, true);
        }
        if (this.owner.moveFix.getValue() != 0 || this.owner.rotations.getValue() == 2) {
            event.setPervRotation(rotations[0], 1);
        }
        return true;
    }

    private float[] computeAimTarget(AttackData target) {
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0F);
        AxisAlignedBB box = target.getBox();

        double tx = (box.minX + box.maxX) * 0.5;
        double tz = (box.minZ + box.maxZ) * 0.5;

        double dx = tx - eye.xCoord;
        double dz = tz - eye.zCoord;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        double centerY = (box.minY + box.maxY) * 0.5;
        double eyeAnchor = MathHelper.clamp_double(eye.yCoord, box.minY, box.maxY);

        double weight = MathHelper.clamp_double(1.0 - (horizDist - 1.5) / 3.0, 0.0, 1.0);

        double ty = centerY * (1.0 - weight) + eyeAnchor * weight;

        double dy = ty - eye.yCoord;

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizDist));
        return new float[]{targetYaw, targetPitch};
    }

    private float[] computeBasicRotations(AttackData target, float yaw, float pitch) {
        float[] desired = this.computeAimTarget(target);

        float dYaw = MathHelper.wrapAngleTo180_float(desired[0] - yaw);
        float dPitch = MathHelper.wrapAngleTo180_float(desired[1] - pitch);

        float gcd = RotationUtil.getFixedAngleDelta();

        if (gcd >= 1.0E-4F) {
            float dead = gcd * 0.5F;
            if (Math.abs(dYaw) < dead) dYaw = 0.0F;
            if (Math.abs(dPitch) < dead) dPitch = 0.0F;
        }

        float smooth = this.owner.smoothing.getValue() / 100.0F;
        float gain = 0.55F - 0.40F * smooth;

        float stepYaw = dYaw * gain;
        float stepPitch = dPitch * gain;

        float jitter = 0.04F * (1.0F - smooth * 0.5F);
        stepYaw += RandomUtil.nextFloat(-jitter, jitter) * Math.abs(stepYaw);
        stepPitch += RandomUtil.nextFloat(-jitter, jitter) * Math.abs(stepPitch);

        if (gcd >= 1.0E-4F) {
            float yawPixels = stepYaw / gcd + this.residYaw;
            float pitchPixels = stepPitch / gcd + this.residPitch;
            int yawWhole = randomRound(yawPixels);
            int pitchWhole = randomRound(pitchPixels);
            this.residYaw = yawPixels - yawWhole;
            this.residPitch = pitchPixels - pitchWhole;
            stepYaw = yawWhole * gcd;
            stepPitch = pitchWhole * gcd;
        }

        return new float[]{
                yaw + stepYaw,
                MathHelper.clamp_float(pitch + stepPitch, -90.0F, 90.0F)
        };
    }

    private float[] computePhysicalRotations(AttackData target, float yaw, float pitch) {
        float[] desired = this.computeAimTarget(target);
        this.updateLag(desired[0], desired[1]);

        float errYaw = MathHelper.wrapAngleTo180_float(this.lagYaw - yaw);
        float errPitch = MathHelper.wrapAngleTo180_float(this.lagPitch - pitch);

        float smooth = this.owner.smoothing.getValue() / 100.0F;
        float stiffness = STIFFNESS * (1.0F - 0.40F * smooth);
        float damping = DAMPING + 0.30F * smooth;

        this.yawVel += errYaw * stiffness - this.yawVel * damping;
        this.pitchVel += errPitch * stiffness - this.pitchVel * damping;

        this.yawVel = MathHelper.clamp_float(this.yawVel, -PHYSICAL_MAX_VEL, PHYSICAL_MAX_VEL);
        this.pitchVel = MathHelper.clamp_float(this.pitchVel, -PHYSICAL_MAX_VEL, PHYSICAL_MAX_VEL);

        this.tremorYaw = this.tremorYaw * TREMOR_DECAY + RandomUtil.nextFloat(-1.0F, 1.0F) * TREMOR_INJECT;
        this.tremorPitch = this.tremorPitch * TREMOR_DECAY + RandomUtil.nextFloat(-1.0F, 1.0F) * TREMOR_INJECT;

        float speed = MathHelper.sqrt_float(this.yawVel * this.yawVel + this.pitchVel * this.pitchVel);
        float tremorScale = 0.05F + 0.14F * Math.min(1.0F, speed / 10.0F);

        float totalYaw = this.yawVel + this.tremorYaw * tremorScale;
        float totalPitch = this.pitchVel + this.tremorPitch * tremorScale;

        float gcd = RotationUtil.getFixedAngleDelta();
        if (gcd < 1.0E-4F) {
            return new float[]{
                    yaw + totalYaw,
                    MathHelper.clamp_float(pitch + totalPitch, -90.0F, 90.0F)
            };
        }

        float yawPixels = totalYaw / gcd + this.residYaw;
        float pitchPixels = totalPitch / gcd + this.residPitch;

        int yawWhole = randomRound(yawPixels);
        int pitchWhole = randomRound(pitchPixels);

        this.residYaw = yawPixels - yawWhole;
        this.residPitch = pitchPixels - pitchWhole;

        return new float[]{
                yaw + yawWhole * gcd,
                MathHelper.clamp_float(pitch + pitchWhole * gcd, -90.0F, 90.0F)
        };
    }

    private void updateLag(float yaw, float pitch) {
        if (this.lagCount >= this.yawLag.length) {
            int readIdx = (this.lagIdx + 1) % this.yawLag.length;
            this.lagYaw = this.yawLag[readIdx];
            this.lagPitch = this.pitchLag[readIdx];
        } else {
            this.lagYaw = yaw;
            this.lagPitch = pitch;
            this.lagCount++;
        }
        this.yawLag[this.lagIdx] = yaw;
        this.pitchLag[this.lagIdx] = pitch;
        this.lagIdx = (this.lagIdx + 1) % this.yawLag.length;
    }

    private static int randomRound(float value) {
        int base = (int) Math.floor(value);
        float frac = value - base;
        return base + (RandomUtil.nextFloat() < frac ? 1 : 0);
    }

    private void clearMotion() {
        this.yawVel = 0.0F;
        this.pitchVel = 0.0F;
        this.residYaw = 0.0F;
        this.residPitch = 0.0F;
        this.tremorYaw = 0.0F;
        this.tremorPitch = 0.0F;
        this.lagCount = 0;
    }

    public void applyMoveFix(MoveInputEvent event) {
        if (this.owner.moveFix.getValue() == 1
                && this.owner.rotations.getValue() != 2
                && RotationState.isActived()
                && RotationState.getPriority() == 1.0F
                && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    public void reset() {
        this.aiming = false;
        this.aimYaw = this.prevAimYaw = 0.0F;
        this.aimPitch = this.prevAimPitch = 0.0F;
        this.clearMotion();
    }
}