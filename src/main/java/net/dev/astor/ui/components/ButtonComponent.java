package net.dev.astor.ui.components;

import net.dev.astor.enums.ChatColors;
import net.dev.astor.property.properties.ButtonProperty;
import net.dev.astor.ui.Component;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicInteger;

public class ButtonComponent implements Component {
    private final ButtonProperty property;
    private final ModuleComponent module;
    private int offsetY;
    private int x;
    private int y;
    private boolean hovered;

    public ButtonComponent(ButtonProperty property, ModuleComponent parentModule, int offsetY) {
        this.property = property;
        this.module = parentModule;
        this.x = parentModule.category.getX() + parentModule.category.getWidth();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
    }

    @Override
    public void draw(AtomicInteger offset) {
        String name = this.property.getName().replace("-", " ");
        String color = this.hovered ? "&a" : "&7";
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        Minecraft.getMinecraft().fontRendererObj.drawString(
                ChatColors.formatColor(color + name + ":"),
                (float) ((this.module.category.getX() + 4) * 2),
                (float) ((this.module.category.getY() + this.offsetY + 5) * 2),
                -1, false
        );
        GL11.glPopMatrix();
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.y = this.module.category.getY() + this.offsetY;
        this.x = this.module.category.getX();
        this.hovered = mousePosX > this.x && mousePosX < this.x + this.module.category.getWidth()
                && mousePosY > this.y && mousePosY < this.y + 11;
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 12;
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button == 0 && this.module.panelExpand && this.isHovered(x, y)) {
            this.property.click();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
    }

    @Override
    public boolean keyTyped(char chatTyped, int keyCode) {
        return false;
    }

    private boolean isHovered(int mousePosX, int mousePosY) {
        return mousePosX > this.x && mousePosX < this.x + this.module.category.getWidth()
                && mousePosY > this.y && mousePosY < this.y + 11;
    }

    @Override
    public boolean isVisible() {
        return this.property.isVisible();
    }
}