package net.dev.astor.mixin.movement;

import net.dev.astor.Astor;
import net.dev.astor.event.EventManager;
import net.dev.astor.event.events.impl.combat.KnockbackEvent;
import net.dev.astor.event.events.impl.movement.SafeWalkEvent;
import net.dev.astor.module.impl.exploit.NoPitchLimit;
import net.dev.astor.module.impl.movement.NoPush;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {Entity.class}, priority = 9999)
public abstract class MixinEntity {
    @Shadow
    public World worldObj;
    @Shadow
    public double posX;
    @Shadow
    public double posY;
    @Shadow
    public double posZ;
    @Shadow
    public double motionX;
    @Shadow
    public double motionY;
    @Shadow
    public double motionZ;
    @Shadow
    public float rotationYaw;
    @Shadow
    public float rotationPitch;
    @Shadow
    public float prevRotationYaw;
    @Shadow
    public float prevRotationPitch;
    @Shadow
    public boolean onGround;

    @Shadow
    public boolean isRiding() {
        return false;
    }

    @Inject(
            method = {"setVelocity"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void setVelocity(double double1, double double2, double double3, CallbackInfo callbackInfo) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP) {
            KnockbackEvent event = new KnockbackEvent(double1, double2, double3);
            EventManager.call(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
                this.motionX = event.getX();
                this.motionY = event.getY();
                this.motionZ = event.getZ();
            }
        }
    }

    @Inject(
            method = {"setAngles"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void setAngles(CallbackInfo callbackInfo) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP && Astor.rotationManager != null && Astor.rotationManager.isRotated()) {
            callbackInfo.cancel();
        }
    }

    @Redirect(
            method = {"setAngles"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MathHelper;clamp_float(FFF)F")
    )
    private float noPitchLimit(float value, float min, float max) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP && Astor.moduleManager != null) {
            NoPitchLimit noPitchLimit = (NoPitchLimit) Astor.moduleManager.modules.get(NoPitchLimit.class);
            if (noPitchLimit != null && noPitchLimit.isEnabled()) {
                return value;
            }
        }
        return MathHelper.clamp_float(value, min, max);
    }

    @Redirect(
            method = {"applyEntityCollision"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"
            )
    )
    private void noPushEntityCollision(Entity entity, double x, double y, double z) {
        Entity self = (Entity) ((Object) this);
        if (entity == self && self instanceof EntityPlayerSP && Astor.moduleManager != null) {
            NoPush noPush = (NoPush) Astor.moduleManager.modules.get(NoPush.class);
            if (noPush != null && noPush.cancelEntityPush()) {
                return;
            }
        }
        entity.addVelocity(x, y, z);
    }

    @ModifyVariable(
            method = {"moveEntity"},
            ordinal = 0,
            at = @At("STORE"),
            name = {"flag"}
    )
    private boolean moveEntity(boolean boolean1) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP) {
            SafeWalkEvent event = new SafeWalkEvent(boolean1);
            EventManager.call(event);
            return event.isSafeWalk();
        } else {
            return boolean1;
        }
    }
}