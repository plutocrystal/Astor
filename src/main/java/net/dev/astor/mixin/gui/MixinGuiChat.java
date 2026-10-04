package net.dev.astor.mixin.gui;

import net.dev.astor.Astor;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {GuiChat.class}, priority = 9999)
public abstract class MixinGuiChat {
    @Inject(
            method = {"keyTyped"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void astor$tabComplete(char typedChar, int keyCode, CallbackInfo callbackInfo) {
        if (keyCode != Keyboard.KEY_TAB) {
            return;
        }
        callbackInfo.cancel();
        if (Astor.commandManager == null) {
            return;
        }
        GuiTextField input = ((IAccessorGuiChat) (Object) this).getInputField();
        if (input == null) {
            return;
        }
        String text = input.getText();
        if (!text.startsWith(".")) {
            return;
        }
        String completed = Astor.commandManager.getTabComplete(text);
        if (completed != null) {
            input.setText(completed);
            input.setCursorPositionZero();
        }
    }
}
