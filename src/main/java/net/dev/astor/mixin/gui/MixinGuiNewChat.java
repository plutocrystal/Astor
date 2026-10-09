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
    /** How many lines vanilla keeps before it starts deleting the oldest ones. */
    private static final int VANILLA_LINE_LIMIT = 100;

    /**
     * Vanilla paints a translucent black box behind every chat line. That box is the first of the
     * three {@code drawRect} calls in {@code drawChat}; the two later ones are the scroll bar and
     * have to stay.
     */
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

    /**
     * {@code setChatLine} trims the log down to {@link #VANILLA_LINE_LIMIT} lines by dropping the
     * oldest entry while the list is still over the limit. Reporting the size as capped at the limit
     * makes both trim loops believe the list already fits, so nothing gets dropped.
     * <p>
     * {@code refreshChat} rebuilds the drawn lines through this same method, so a resource reload
     * keeps the whole history too.
     */
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
