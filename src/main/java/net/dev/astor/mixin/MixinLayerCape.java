package net.dev.astor.mixin;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.render.CustomCape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {LayerCape.class}, priority = 9991)
public abstract class MixinLayerCape {
    @Shadow
    @Final
    private RenderPlayer playerRenderer;

    @Inject(
            method = "doRenderLayer(Lnet/minecraft/client/entity/AbstractClientPlayer;FFFFFFF)V",
            at = {@At("HEAD")},
            cancellable = true
    )
    private void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                               float partialTicks, float ageInTicks, float netHeadYaw,
                               float headPitch, float scale, CallbackInfo callbackInfo) {
        if (Astor.moduleManager == null) {
            return;
        }
        EntityPlayerSP local = Minecraft.getMinecraft().thePlayer;
        if (local == null || player != local) {
            return;
        }
        CustomCape customCape = (CustomCape) Astor.moduleManager.modules.get(CustomCape.class);
        if (customCape == null || !customCape.isEnabled()) {
            return;
        }
        if (!player.hasPlayerInfo() || player.isInvisible()) {
            return;
        }
        if (customCape.renderCape(player, this.playerRenderer, partialTicks)) {
            callbackInfo.cancel();
        }
    }
}