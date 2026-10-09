package net.dev.astor.mixin.render;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.module.impl.render.CustomModel;
import net.dev.astor.util.ItemUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(value = {RenderPlayer.class}, priority = 9998)
public abstract class MixinRenderPlayer {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private static final int FAKE_BLOCK = 1;
    
    private static final int BLOCKING_ARM = 3;
    private static final int ARM_AT_REST = 1;

    @Inject(method = {"getEntityTexture"}, at = {@At("HEAD")}, cancellable = true)
    private void getEntityTexture(AbstractClientPlayer entity, CallbackInfoReturnable<ResourceLocation> callbackInfo) {
        CustomModel customModel = CustomModel.get();
        if (customModel != null && customModel.isEnabled()) {
            callbackInfo.setReturnValue(customModel.getTexture());
        }
    }

    @Inject(method = {"setModelVisibilities"}, at = {@At("RETURN")})
    private void setModelVisibilities(AbstractClientPlayer clientPlayer, CallbackInfo callbackInfo) {
        if (clientPlayer != mc.thePlayer || mc.gameSettings.thirdPersonView == 0) {
            return;
        }
        KillAura killAura = (KillAura) Astor.moduleManager.modules.get(KillAura.class);
        if (killAura == null || !killAura.isEnabled() || killAura.autoBlock.getValue() != FAKE_BLOCK) {
            return;
        }
        ModelPlayer model = ((RenderPlayer) (Object) this).getMainModel();
        if (model.heldItemRight == BLOCKING_ARM && ItemUtil.isHoldingSword()) {
            model.heldItemRight = ARM_AT_REST;
        }
    }
}