package net.dev.astor.mixin.render;

import net.dev.astor.module.impl.render.CustomModel;
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

    /**
     * Drops armour entirely while a custom model is drawn in place of the biped.
     *
     * <p>None of the three shapes has an armour layer, so the vanilla chest/legs/feet/head plates would
     * hang off geometry that is not there. Cancelling at the layer's entry point also covers the glint
     * pass, which reads the same model boxes. Upstream injects at the same point for the same reason.</p>
     */
    @Inject(
            method = {"doRenderLayer"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                                float ageInTicks, float netHeadYaw, float headPitch, float scale,
                                CallbackInfo callbackInfo) {
        CustomModel customModel = CustomModel.get();
        if (customModel != null && customModel.isEnabled()) {
            callbackInfo.cancel();
        }
    }
}