package net.dev.astor.mixin.attack;

import net.dev.astor.Astor;
import net.dev.astor.mixin.movement.MixinEntityLivingBase;
import net.dev.astor.module.impl.movement.KeepSprint;
import net.dev.astor.module.impl.render.Particles;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {EntityPlayer.class}, priority = 9999)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase {
    @ModifyConstant(
            method = {"attackTargetEntityWithCurrentItem"},
            constant = {@Constant(
                    doubleValue = 0.6
            )}
    )
    private double attackTargetEntityWithCurrentItem(double speed) {
        if (Astor.moduleManager == null) {
            return speed;
        } else {
            KeepSprint keepSprint = (KeepSprint) Astor.moduleManager.modules.get(KeepSprint.class);
            return keepSprint.isEnabled() && keepSprint.shouldKeepSprint()
                    ? speed + (1.0 - speed) * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0)
                    : speed;
        }
    }

    @Redirect(
            method = {"attackTargetEntityWithCurrentItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"
            )
    )
    private void setSprinnt(EntityPlayer entityPlayer, boolean boolean2) {
        if (Astor.moduleManager != null) {
            KeepSprint keepSprint = (KeepSprint) Astor.moduleManager.modules.get(KeepSprint.class);
            if (!keepSprint.isEnabled() || !keepSprint.shouldKeepSprint()) {
                entityPlayer.setSprinting(boolean2);
            }
        }
    }

    @Redirect(
            method = {"attackTargetEntityWithCurrentItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;onCriticalHit(Lnet/minecraft/entity/Entity;)V"
            )
    )
    private void onCriticalHit(EntityPlayer entityPlayer, Entity entity) {
        for (int i = 0; i < Particles.getCriticalsMultiplier(true); i++) {
            entityPlayer.onCriticalHit(entity);
        }
    }

    @Redirect(
            method = {"attackTargetEntityWithCurrentItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;onEnchantmentCritical(Lnet/minecraft/entity/Entity;)V"
            )
    )
    private void onEnchantmentCritical(EntityPlayer entityPlayer, Entity entity) {
        for (int i = 0; i < Particles.getSharpnessMultiplier(true); i++) {
            entityPlayer.onEnchantmentCritical(entity);
        }
    }

    @Inject(
            method = {"attackTargetEntityWithCurrentItem"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;setLastAttacker(Lnet/minecraft/entity/Entity;)V"
            )
    )
    private void onSuccessfulHit(Entity entity, CallbackInfo callbackInfo) {
        Particles particles = Particles.get();
        if (!particles.isEnabled()) {
            return;
        }
        EntityPlayer player = (EntityPlayer) (Object) this;
        if (!isVanillaCritical(player, entity)) {
            for (int i = 0; i < Particles.getCriticalsMultiplier(false); i++) {
                player.onCriticalHit(entity);
            }
        }
        if (!isVanillaEnchanted(player, entity)) {
            for (int i = 0; i < Particles.getSharpnessMultiplier(false); i++) {
                player.onEnchantmentCritical(entity);
            }
        }
    }

    private boolean isVanillaCritical(EntityPlayer player, Entity entity) {
        return player.fallDistance > 0.0F && !player.onGround && !player.isOnLadder() && !player.isInWater()
                && !player.isPotionActive(Potion.blindness) && player.ridingEntity == null
                && entity instanceof EntityLivingBase;
    }

    private boolean isVanillaEnchanted(EntityPlayer player, Entity entity) {
        EnumCreatureAttribute attribute = entity instanceof EntityLivingBase
                ? ((EntityLivingBase) entity).getCreatureAttribute()
                : EnumCreatureAttribute.UNDEFINED;
        return EnchantmentHelper.getModifierForCreature(player.getHeldItem(), attribute) > 0.0F;
    }
}