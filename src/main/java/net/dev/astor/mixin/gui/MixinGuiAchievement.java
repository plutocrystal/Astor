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

/**
 * Keeps achievement popups off the screen.
 *
 * <p>Ported from LiquidBounce's AntiBlind, which cancels the same two methods. Vanilla splits the popup
 * across both: {@code displayAchievement} is the handler the server's achievement packet reaches and
 * only records what to show, while {@code updateAchievementWindow} is what draws it each frame. The
 * handler alone would already stop a regular popup, since it leaves {@code theAchievement} null and the
 * draw method does nothing without it; cancelling the draw as well covers a popup that was already
 * pending when the module was switched on.</p>
 *
 * <p>LiquidBounce labels the first hook "cancel achievement display packet", but what it actually
 * cancels is this client side method - the packet has already been sent by then. The effect is the
 * popup not appearing, which is what is reproduced here.</p>
 *
 * <p>{@code displayUnformattedAchievement}, the permanent variant, is left alone because upstream does
 * not hook it either.</p>
 */
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