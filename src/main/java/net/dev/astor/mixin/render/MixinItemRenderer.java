package net.dev.astor.mixin.render;

import net.dev.astor.module.impl.render.NoRender;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {ItemRenderer.class}, priority = 9999)
public abstract class MixinItemRenderer {

    @Inject(
            method = {"renderBlockInHand"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderBlockInHand(float partialTicks, TextureAtlasSprite atlas, CallbackInfo callbackInfo) {
        NoRender noRender = NoRender.get();
        if (noRender != null && noRender.headBlock.getValue()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"renderWaterOverlayTexture"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderWaterOverlayTexture(float partialTicks, CallbackInfo callbackInfo) {
        NoRender noRender = NoRender.get();
        if (noRender != null && noRender.waterFog.getValue()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"renderFireInFirstPerson"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderFireInFirstPerson(float partialTicks, CallbackInfo callbackInfo) {
        NoRender noRender = NoRender.get();
        if (noRender != null && noRender.fire.getValue()) {
            callbackInfo.cancel();
        }
    }
}