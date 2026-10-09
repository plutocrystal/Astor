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

/**
 * Two render-time hooks for the local player.
 *
 * <p>Points the player renderer at the selected CustomModel shape's texture instead of the player's
 * skin - ported from NightX's MixinRenderPlayer, reduced to the one hook CustomModel needs, since
 * upstream also resolves the texture through its API downloader on every call including when the module
 * is off.</p>
 *
 * <p>And stops KillAura's fake block from raising an arm in third person.</p>
 */
@SideOnly(Side.CLIENT)
@Mixin(value = {RenderPlayer.class}, priority = 9998)
public abstract class MixinRenderPlayer {
    private static final Minecraft mc = Minecraft.getMinecraft();
    /** Index of the Fake mode in KillAura's AutoBlock list. */
    private static final int FAKE_BLOCK = 1;
    /** The value RenderPlayer puts on the model for a raised blocking arm. */
    private static final int BLOCKING_ARM = 3;
    private static final int ARM_AT_REST = 1;

    @Inject(method = {"getEntityTexture"}, at = {@At("HEAD")}, cancellable = true)
    private void getEntityTexture(AbstractClientPlayer entity, CallbackInfoReturnable<ResourceLocation> callbackInfo) {
        CustomModel customModel = CustomModel.get();
        if (customModel != null && customModel.isEnabled()) {
            callbackInfo.setReturnValue(customModel.getTexture());
        }
    }

    /**
     * Undoes the raised arm that a fake block would otherwise draw in third person.
     *
     * <p>{@code RenderPlayer.setModelVisibilities} lifts the arm by setting {@code heldItemRight = 3}
     * whenever the held item is in use with {@code EnumAction.BLOCK}, and a sword is the only thing
     * that has that action. Pressing the value back down here leaves the use-item state itself alone, so
     * the packets that make the server believe the block is real still go out - only the pose is
     * dropped. Faking the block by clearing the item in use instead would have taken the network path
     * down with it.</p>
     *
     * <p>Scoped to the local player in third person under the fake mode. The first person view is left
     * alone, a remote player who is genuinely blocking is left alone, and KillAura's own blocking mode
     * keeps its pose - it is only the block the server cannot see that gets hidden.</p>
     */
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