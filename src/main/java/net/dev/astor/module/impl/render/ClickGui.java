package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/**
 * Owns the click gui screen. Its enabled state mirrors whether the screen is currently open, so it
 * reads as ON in the module list and in the HUD while the gui is up.
 *
 * <p>Closing the gui by any other route (escape, another screen taking over) is picked up by
 * {@link net.dev.astor.ui.ClickGui#onGuiClosed()}, which switches this module back off.</p>
 */
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
