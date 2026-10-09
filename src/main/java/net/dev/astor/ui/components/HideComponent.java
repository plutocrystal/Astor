package net.dev.astor.ui.components;

import net.dev.astor.enums.ChatColors;
import net.dev.astor.ui.Component;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Renders {@code Module#hide} on the left of the bind row instead of as a normal settings row, so
 * that the toggle and the key bind sit side by side. Right half of the row belongs to
 * {@link BindComponent}.
 */
public class HideComponent implements Component {
    private final ModuleComponent parentModule;
    private int offsetY;
    private int x;
    private int y;

    public HideComponent(ModuleComponent parentModule, int offsetY) {
        this.parentModule = parentModule;
        this.x = parentModule.category.getX();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
    }

    public void draw(AtomicInteger offset) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        Minecraft mc = Minecraft.getMinecraft();
        String text = "Hide: " + ChatColors.formatColor(this.parentModule.mod.hide.formatValue());
        // Right aligned with the same 4px padding BindComponent uses on the left.
        float right = (float) (this.parentModule.category.getX() + this.parentModule.category.getWidth() - 4) * 2.0F;
        mc.fontRendererObj.drawString(text, right - mc.fontRendererObj.getStringWidth(text),
                (float) (this.parentModule.category.getY() + this.offsetY + 5) * 2.0F,
                -1, false);
        GL11.glPopMatrix();
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.x = this.parentModule.category.getX();
        this.y = this.parentModule.category.getY() + this.offsetY;
    }

    @Override
    public boolean mouseDown(int x, int y, int button) {
        if (button == 0 && this.parentModule.panelExpand && this.isHovered(x, y)) {
            this.parentModule.mod.hide.setValue(!this.parentModule.mod.hide.getValue());
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

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {
        return 12;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    /**
     * Right half of the row only. {@link BindComponent} owns the left half, and the two must not
     * overlap because {@link ModuleComponent#mouseDown} walks the settings back to front and stops
     * at the first hit.
     */
    public boolean isHovered(int x, int y) {
        int mid = this.x + this.parentModule.category.getWidth() / 2;
        return x >= mid && x < this.x + this.parentModule.category.getWidth() && y > this.y && y < this.y + 12;
    }
}