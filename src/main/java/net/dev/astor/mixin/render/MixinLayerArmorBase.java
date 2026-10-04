package net.dev.astor.mixin.render;

import net.dev.astor.module.impl.render.NoRender;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {LayerArmorBase.class}, priority = 9999)
public abstract class MixinLayerArmorBase {
    @Inject(
            method = {"renderLayer"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale, int armorSlot,
                              CallbackInfo callbackInfo) {
        if (NoRender.hidesArmorSlot(armorSlot)) {
            callbackInfo.cancel();
        }
    }
}