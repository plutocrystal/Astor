package net.dev.astor.mixin.gui;

import net.dev.astor.module.impl.render.AntiBlind;
import net.minecraft.client.gui.achievement.GuiAchievement;
import net.minecraft.stats.Achievement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {GuiAchievement.class}, priority = 9999)
public abstract class MixinGuiAchievement {

    @Inject(
            method = {"displayAchievement"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void displayAchievement(Achievement achievement, CallbackInfo callbackInfo) {
        AntiBlind antiBlind = AntiBlind.get();
        if (antiBlind != null && antiBlind.achievements.getValue()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"updateAchievementWindow"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void updateAchievementWindow(CallbackInfo callbackInfo) {
        AntiBlind antiBlind = AntiBlind.get();
        if (antiBlind != null && antiBlind.achievements.getValue()) {
            callbackInfo.cancel();
        }
    }
}