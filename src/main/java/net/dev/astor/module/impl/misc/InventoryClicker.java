package net.dev.astor.module.impl.misc;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.mixin.gui.IAccessorGuiScreen;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.input.Mouse;

public class InventoryClicker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty triggerTicks = new IntProperty("Ticks", 2, 0, 20);
    public int ticks;

    public InventoryClicker() {
        super("InventoryClicker", Category.MISC, false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{triggerTicks.getValue().toString() + " ticks"};
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && mc.thePlayer != null && event.getType() == EventType.PRE) {
            if (mc.currentScreen instanceof GuiContainer) {
                GuiContainer screen = ((GuiContainer) mc.currentScreen);
                final int mouseX = Mouse.getEventX() * screen.width / mc.displayWidth;
                final int mouseY = screen.height - Mouse.getEventY() * screen.height / mc.displayHeight - 1;
                if (Mouse.isButtonDown(0)) {
                    ticks++;
                    if(ticks > triggerTicks.getValue())
                    {
                        ((IAccessorGuiScreen)screen).callMouseClicked(mouseX, mouseY, 0);
                    }
                }else {
                    ticks = 0;
                }
            }
        }
    }
}