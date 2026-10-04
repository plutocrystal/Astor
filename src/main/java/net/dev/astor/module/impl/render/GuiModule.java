package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.ui.ClickGui;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private ClickGui clickGui;

    public GuiModule() {
        super("ClickGui", Category.RENDER, false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        if(clickGui == null){
            clickGui = new ClickGui();
        }
        mc.displayGuiScreen(clickGui);
    }
}