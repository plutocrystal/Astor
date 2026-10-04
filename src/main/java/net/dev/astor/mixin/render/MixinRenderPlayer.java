package net.dev.astor.mixin.render;

import net.dev.astor.module.impl.render.NoRender;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {RenderPlayer.class}, priority = 9999)
public abstract class MixinRenderPlayer {
    @Inject(
            method = {"setModelVisibilities"},
            at = {@At("RETURN")}
    )
    private void setModelVisibilities(AbstractClientPlayer clientPlayer, CallbackInfo callbackInfo) {
        NoRender noRender = NoRender.get();
        if (noRender == null) {
            return;
        }
        ModelPlayer model = ((RenderPlayer) (Object) this).getMainModel();
        if (noRender.helmet.getValue()) {
            model.bipedHeadwear.showModel = false;
        }
        if (noRender.chestplate.getValue()) {
            model.bipedBodyWear.showModel = false;
            model.bipedLeftArmwear.showModel = false;
            model.bipedRightArmwear.showModel = false;
        }
        if (noRender.leggings.getValue()) {
            model.bipedLeftLegwear.showModel = false;
            model.bipedRightLegwear.showModel = false;
        }
    }
}