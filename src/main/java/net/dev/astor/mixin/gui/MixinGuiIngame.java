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