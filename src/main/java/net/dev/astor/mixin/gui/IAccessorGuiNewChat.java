package net.dev.astor.mixin.gui;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin({GuiNewChat.class})
public interface IAccessorGuiNewChat {
    
    @Accessor("drawnChatLines")
    List<ChatLine> getDrawnChatLines();

    @Accessor("chatLines")
    List<ChatLine> getChatLines();
}

