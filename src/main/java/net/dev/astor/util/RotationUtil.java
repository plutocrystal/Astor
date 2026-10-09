package net.dev.astor.util;

import net.dev.astor.mixin.movement.IAccessorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class RotationUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float wrapAngleDiff(float angle, float target) {
        return target + MathHelper.wrapAngleTo180_float(angle - target);
    }

    public static float clampAngle(float angle, float maxAngle) {
        maxAngle = Math.max(0.0f, Math.min(180.0f, maxAngle));
        if (angle > maxAngle) {
            angle = maxAngle;
        } else if (angle < -maxAngle) {
            angle = -maxAngle;
        }
        return angle;
    }

    public static float smoothAngle(float angle, float smoothFactor) {
        float f = Math.max(0.0f, Math.min(1.0f, smoothFactor));
        return angle * (1.0f - 0.5f * f);
    }

    public static float quantizeAngle(float angle) {
        float gcd = getFixedAngleDelta();
        if (gcd < 1.0E-4F) {
            return angle;
        }
        return angle - angle % gcd;
    }

    public static float getFixedAngleDelta(float sensitivity) {
        float f = sensitivity * 0.6F + 0.2F;
        return f * f * f * 1.2F;
    }

    public static float getFixedAngleDelta() {
        if (mc.gameSettings == null) {
            return 0.15F;
        }
        return getFixedAngleDelta(mc.gameSettings.mouseSensitivity);
    }

    public static float getFixedSensitivityAngle(float targetAngle, float startAngle, float gcd) {
        if (Float.isNaN(targetAngle) || Float.isNaN(startAngle) || Float.isNaN(gcd) || gcd < 1.0E-4F) {
            return targetAngle;
        }
        float diff = MathHelper.wrapAngleTo180_float(targetAngle - startAngle);
        double steps = Math.round((double) diff / (double) gcd);
        return (float) ((double) startAngle + steps * (double) gcd);
    }

    public static float applyGCD(float target, float current) {
        return getFixedSensitivityAngle(target, current, getFixedAngleDelta());
    }

    public static float[] getRotationsToBox(AxisAlignedBB boundingBox, float yaw, float pitch, float maxAngle, float smoothFactor) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        double minTargetY = boundingBox.minY + 0.05 * (boundingBox.maxY - boundingBox.minY);
        double maxTargetY = boundingBox.minY + 0.75 * (boundingBox.maxY - boundingBox.minY);
        double deltaX = (boundingBox.minX + boundingBox.maxX) / 2.0 - eyePos.xCoord;
        double deltaY = eyePos.yCoord >= maxTargetY ? maxTargetY - eyePos.yCoord : (eyePos.yCoord <= minTargetY ? minTargetY - eyePos.yCoord : 0.0);
        double deltaZ = (boundingBox.minZ + boundingBox.maxZ) / 2.0 - eyePos.zCoord;
        return getRotations(deltaX, deltaY, deltaZ, yaw, pitch, maxAngle, smoothFactor);
    }

    public static float[] getRotationsTo(double targetX, double targetY, double targetZ, float currentYaw, float currentPitch) {
        return getRotations(targetX, targetY, targetZ, currentYaw, currentPitch, 180.0f, 0.0f);
    }

    public static float[] getRotations(double targetX, double targetY, double targetZ, float currentYaw, float currentPitch, float maxAngle, float smoothFactor) {
        double horizontalDistance = Math.sqrt(targetX * targetX + targetZ * targetZ);
        float targetYaw = (float) (Math.atan2(targetZ, targetX) * 180.0 / Math.PI) - 90.0f;
        float targetPitch = (float) (-Math.atan2(targetY, horizontalDistance) * 180.0 / Math.PI);
        float yawDelta = MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float(targetPitch - currentPitch);
        yawDelta = clampAngle(yawDelta, maxAngle);
        pitchDelta = clampAngle(pitchDelta, maxAngle);
        yawDelta = smoothAngle(yawDelta, smoothFactor);
        pitchDelta = smoothAngle(pitchDelta, smoothFactor);
        float newYaw = applyGCD(currentYaw + yawDelta, currentYaw);
        float newPitch = applyGCD(currentPitch + pitchDelta, currentPitch);
        newPitch = MathHelper.clamp_float(newPitch, -90.0f, 90.0f);
        return new float[]{newYaw, newPitch};
    }

    public static Vec3 clampVecToBox(Vec3 vector, AxisAlignedBB boundingBox) {
        double[] coords = new double[]{vector.xCoord, vector.yCoord, vector.zCoord};
        double[] minCoords = new double[]{boundingBox.minX, boundingBox.minY, boundingBox.minZ};
        double[] maxCoords = new double[]{boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ};
        for (int i = 0; i < 3; ++i) {
            if (coords[i] > maxCoords[i]) {
                coords[i] = maxCoords[i];
            } else if (coords[i] < minCoords[i]) {
                coords[i] = minCoords[i];
            }
        }
        return new Vec3(coords[0], coords[1], coords[2]);
    }

    public static double distanceToEntity(Entity entity) {
        float borderSize = entity.getCollisionBorderSize();
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        return distanceToBox(boundingBox);
    }

    public static double distanceToBox(Entity entity, Vec3 point) {
        float borderSize = entity.getCollisionBorderSize();
        return distanceToBox(entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize), point);
    }

    public static double distanceToBox(AxisAlignedBB boundingBox) {
        return distanceToBox(boundingBox, mc.thePlayer.getPositionEyes(1.0f));
    }

    public static double distanceToBox(AxisAlignedBB boundingBox, Vec3 point) {
        if (boundingBox.isVecInside(point)) {
            return 0.0;
        }
        Vec3 clampedPoint = clampVecToBox(point, boundingBox);
        double deltaX = clampedPoint.xCoord - point.xCoord;
        double deltaY = clampedPoint.yCoord - point.yCoord;
        double deltaZ = clampedPoint.zCoord - point.zCoord;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public static float angleToEntity(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float borderSize = entity.getCollisionBorderSize();
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        if (boundingBox.isVecInside(eyePos)) {
            return 0.0f;
        }
        double deltaX = entity.posX - eyePos.xCoord;
        double deltaZ = entity.posZ - eyePos.zCoord;
        return Math.abs(MathHelper.wrapAngleTo180_float((float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f - mc.thePlayer.rotationYaw)) * 2.0f;
    }

    public static float getYawBetween(double x1, double z1, double x2, double z2) {
        return MathHelper.wrapAngleTo180_float((float) (Math.atan2(z2 - z1, x2 - x1) * 180.0 / Math.PI) - 90.0f - mc.thePlayer.rotationYaw);
    }

    public static MovingObjectPosition rayTrace(float yaw, float pitch, double distance, float partialTicks) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 lookVec = ((IAccessorEntity) mc.thePlayer).callGetVectorForRotation(pitch, yaw);
        Vec3 targetPos = eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);
        return mc.theWorld.rayTraceBlocks(eyePos, targetPos);
    }

    public static MovingObjectPosition rayTrace(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float borderSize = entity.getCollisionBorderSize();
        Vec3 targetPos = clampVecToBox(eyePos, entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize));
        return mc.theWorld.rayTraceBlocks(eyePos, targetPos);
    }

    public static MovingObjectPosition rayTrace(AxisAlignedBB boundingBox, float yaw, float pitch, double distance) {
        return rayTrace(boundingBox, yaw, pitch, distance, 1.0f);
    }

    public static Vec3 rayPoint(float yaw, float pitch, double distance, float partialTicks) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 lookVec = ((IAccessorEntity) mc.thePlayer).callGetVectorForRotation(pitch, yaw);
        return eyePos.addVector(lookVec.xCoord * distance, lookVec.yCoord * distance, lookVec.zCoord * distance);
    }

    public static MovingObjectPosition rayTrace(AxisAlignedBB boundingBox, float yaw, float pitch, double distance, float partialTicks) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(partialTicks);
        return boundingBox.calculateIntercept(eyePos, rayPoint(yaw, pitch, distance, partialTicks));
    }

    public static Vec3 nearestRayPoint(AxisAlignedBB box, float yaw, float pitch, double minDistance, double maxDistance, float partialTicks) {
        Vec3 eye = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 direction = ((IAccessorEntity) mc.thePlayer).callGetVectorForRotation(pitch, yaw);
        double t = ((box.minX + box.maxX) / 2.0 - eye.xCoord) * direction.xCoord
                + ((box.minY + box.maxY) / 2.0 - eye.yCoord) * direction.yCoord
                + ((box.minZ + box.maxZ) / 2.0 - eye.zCoord) * direction.zCoord;
        if (t < minDistance || t > maxDistance) {
            return null;
        }
        return rayPoint(yaw, pitch, t, partialTicks);
    }
}