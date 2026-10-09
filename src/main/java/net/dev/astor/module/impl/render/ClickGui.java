package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class ClickGui extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private net.dev.astor.ui.ClickGui screen;

    @Override
    public String getDescription() {
        return "Opens the click gui, which is also how the client is configured.";
    }

    public ClickGui() {
        super("ClickGui", Category.RENDER, false);
        this.setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        if (this.screen == null) {
            this.screen = new net.dev.astor.ui.ClickGui();
        }
        mc.displayGuiScreen(this.screen);
    }

    @Override
    public void onDisabled() {
        if (mc.currentScreen == this.screen) {
            mc.displayGuiScreen(null);
        }
    }
}

