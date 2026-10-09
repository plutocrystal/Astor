package net.dev.astor.mixin.gui;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.player.AutoBlockIn;
import net.dev.astor.module.impl.player.AutoTool;
import net.dev.astor.module.impl.player.Scaffold;
import net.dev.astor.module.impl.render.AntiBlind;
import net.dev.astor.module.impl.render.NoRender;
import net.dev.astor.module.impl.render.Scoreboard;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {GuiIngame.class}, priority = 9999)
public abstract class MixinGuiIngame {
    @Redirect(
            method = {"updateTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"
            )
    )
    private ItemStack updateTick(InventoryPlayer inventoryPlayer) {
        Scaffold scaffold = (Scaffold) Astor.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
            int slot = scaffold.getSlot();
            if (slot >= 0) {
                return inventoryPlayer.getStackInSlot(slot);
            }
        }
        AutoBlockIn autoBlockIn = (AutoBlockIn) Astor.moduleManager.modules.get(AutoBlockIn.class);
        if(autoBlockIn.itemSpoof.getValue() && autoBlockIn.isEnabled()){
            int slot = autoBlockIn.getSlot();
            if (slot >= 0) {
                return inventoryPlayer.getStackInSlot(slot);
            }
        }
        AutoTool autoTool = (AutoTool) Astor.moduleManager.modules.get(AutoTool.class);
        if (autoTool.itemSpoof.getValue() && autoTool.isEnabled()) {
            int slot = autoTool.getSlot();
            if (slot >= 0) {
                return inventoryPlayer.getStackInSlot(slot);
            }
        }
        return inventoryPlayer.getCurrentItem();
    }

    @Inject(
            method = {"renderPumpkinOverlay"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderPumpkinOverlay(ScaledResolution scaledRes, CallbackInfo callbackInfo) {
        NoRender noRender = NoRender.get();
        if (noRender != null && noRender.pumpkin.getValue()) {
            callbackInfo.cancel();
        }
    }

    /**
     * Drops the boss health bar vanilla draws across the top of the screen.
     *
     * <p>Ported from LiquidBounce's AntiBlind, which cancels the same method. Vanilla only calls this
     * while a boss bar is active, so there is nothing to check beyond the module's own switch.</p>
     */
    @Inject(
            method = {"renderBossHealth"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderBossHealth(CallbackInfo callbackInfo) {
        AntiBlind antiBlind = AntiBlind.get();
        if (antiBlind != null && antiBlind.bossHealth.getValue()) {
            callbackInfo.cancel();
        }
    }

    /**
     * Applies the Scoreboard module's slider to the sidebar background.
     *
     * <p>{@code renderScoreboard} paints its background with three {@code Gui.drawRect} calls whose
     * colours are hard coded into the method, and that method has no other rectangle in it - the names
     * and the scores both go through {@code drawString}. Every rect that reaches this handler is
     * therefore background, so one redirect covers all three without needing to tell them apart.</p>
     *
     * <p>The target names {@code GuiIngame} as the owner even though {@code drawRect} is declared on
     * {@code Gui}. That is what the bytecode says: the three calls inside {@code renderScoreboard} are
     * {@code invokestatic GuiIngame.func_73734_a}, and Mixin matches the owner literally, so naming
     * {@code Gui} here finds nothing to redirect. MixinGuiNewChat's equivalent hook is written the same
     * way for the same reason.</p>
     *
     * <p>Only the alpha byte is replaced; the red, green and blue are kept as they came in. With the
     * module off the original colour is passed straight through, so the result is bit for bit what
     * vanilla draws. With it on, 0% gives an alpha of zero and the blend function draws nothing at all,
     * and 100% gives a solid black - darker than either colour vanilla uses, since vanilla runs its rows
     * at 31% and its header at 38%, which is also why the seam between header and rows disappears
     * somewhere before the slider reaches the top.</p>
     *
     * <p>Named something other than {@code drawRect} on purpose. {@code GuiIngame} inherits that static
     * method from {@code Gui}, so an instance method of the same name and signature here would land in
     * the target class as an attempt to override a static method, which does not compile.</p>
     */
    @Redirect(
            method = {"renderScoreboard"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiIngame;drawRect(IIIII)V"
            )
    )
    private void drawScoreboardBackground(int left, int top, int right, int bottom, int color) {
        Scoreboard scoreboard = Scoreboard.get();
        if (scoreboard == null) {
            Gui.drawRect(left, top, right, bottom, color);
            return;
        }
        int alpha = scoreboard.getBackgroundAlpha();
        Gui.drawRect(left, top, right, bottom, (color & 0x00FFFFFF) | (alpha << 24));
    }
}