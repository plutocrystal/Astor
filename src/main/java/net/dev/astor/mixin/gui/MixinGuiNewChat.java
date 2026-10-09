package net.dev.astor.mixin.gui;

import net.dev.astor.module.impl.misc.Chat;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(value = {GuiNewChat.class}, priority = 9999)
public abstract class MixinGuiNewChat {
    
    private static final int VANILLA_LINE_LIMIT = 100;

    @Redirect(
            method = {"drawChat"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V",
                    ordinal = 0
            )
    )
    private void astor$drawChatBackground(int left, int top, int right, int bottom, int color) {
        Chat chat = Chat.get();
        if (chat != null && chat.noBackground.getValue()) {
            return;
        }
        Gui.drawRect(left, top, right, bottom, color);
    }

    @Redirect(
            method = {"setChatLine"},
            at = @At(value = "INVOKE", target = "Ljava/util/List;size()I")
    )
    private int astor$setChatLineSize(List<?> list) {
        int size = list.size();
        Chat chat = Chat.get();
        if (chat == null || !chat.unlimitedChat.getValue()) {
            return size;
        }
        IAccessorGuiNewChat self = (IAccessorGuiNewChat) (Object) this;
        if (list == self.getDrawnChatLines() || list == self.getChatLines()) {
            return Math.min(size, VANILLA_LINE_LIMIT);
        }
        return size;
    }
}

