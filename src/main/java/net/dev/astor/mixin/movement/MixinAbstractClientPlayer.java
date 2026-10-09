package net.dev.astor.mixin.movement;

import net.dev.astor.Astor;
import net.dev.astor.mixin.attack.MixinEntityPlayer;
import net.dev.astor.module.impl.movement.Sprint;
import net.dev.astor.module.impl.render.CustomSkin;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {AbstractClientPlayer.class}, priority = 9999)
public abstract class MixinAbstractClientPlayer extends MixinEntityPlayer {
    @Redirect(
            method = {"getFovModifier"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/attributes/IAttributeInstance;getAttributeValue()D"
            )
    )
    private double getFovModifier(IAttributeInstance iAttributeInstance) {
        double attributeValue = iAttributeInstance.getAttributeValue();
        if ((((Entity) (Object) this)) instanceof EntityPlayerSP && Astor.moduleManager != null) {
            Sprint sprint = (Sprint) Astor.moduleManager.modules.get(Sprint.class);
            return sprint.isEnabled() && sprint.shouldApplyFovFix(iAttributeInstance) ? attributeValue * 1.300000011920929 : attributeValue;
        } else {
            return attributeValue;
        }
    }

    @Inject(method = {"getLocationSkin"}, at = @At("HEAD"), cancellable = true)
    private void getLocationSkin(CallbackInfoReturnable<ResourceLocation> callbackInfo) {
        CustomSkin customSkin = this.customSkin();
        if (customSkin == null || !customSkin.isEnabled() || !customSkin.shouldApply((AbstractClientPlayer) (Object) this)) {
            return;
        }
        ResourceLocation skin = customSkin.resolveSkin();
        if (skin != null) {
            callbackInfo.setReturnValue(skin);
        }
    }

    @Inject(method = {"getSkinType"}, at = @At("HEAD"), cancellable = true)
    private void getSkinType(CallbackInfoReturnable<String> callbackInfo) {
        CustomSkin customSkin = this.customSkin();
        if (customSkin == null || !customSkin.isEnabled() || !customSkin.shouldApply((AbstractClientPlayer) (Object) this)) {
            return;
        }
        callbackInfo.setReturnValue(customSkin.getSkinType());
    }

    private CustomSkin customSkin() {
        if (Astor.moduleManager == null) {
            return null;
        }
        return (CustomSkin) Astor.moduleManager.modules.get(CustomSkin.class);
    }
}