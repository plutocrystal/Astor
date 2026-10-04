package net.dev.astor.mixin.render;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.render.BlockHighlight;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {RenderGlobal.class}, priority = 9999)
public abstract class MixinRenderGlobal {
    @Inject(
            method = {"drawSelectionBox"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void drawSelectionBox(EntityPlayer player, MovingObjectPosition target, int particleManager, float partialTicks, CallbackInfo callbackInfo) {
        if (Astor.moduleManager == null) {
            return;
        }
        BlockHighlight blockHighlight = (BlockHighlight) Astor.moduleManager.modules.get(BlockHighlight.class);
        if (blockHighlight.isEnabled()) {
            callbackInfo.cancel();
        }
    }
}