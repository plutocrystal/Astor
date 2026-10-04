package net.dev.astor.ui.components;

import net.dev.astor.Astor;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.render.GuiModule;
import net.dev.astor.module.impl.render.HUD;
import net.dev.astor.ui.Component;
import net.dev.astor.ui.dataset.BindStage;
import net.dev.astor.util.ChatUtil;
import net.dev.astor.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicInteger;

public class BindComponent implements Component {
    private static BindComponent activeBinding;
    private boolean isBinding;
    private final ModuleComponent parentModule;
    private int offsetY;
    private int x;
    private int y;

    public BindComponent(ModuleComponent b, int offsetY) {
        this.parentModule = b;
        this.x = b.category.getX() + b.category.getWidth();
        this.y = b.category.getY() + b.offsetY;
        this.offsetY = offsetY;
    }

    public void draw(AtomicInteger offset) {
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        String displayText = this.isBinding ? BindStage.binding : BindStage.bind + ": " + KeyBindUtil.getKeyName(this.parentModule.mod.getKey());
        this.renderText(displayText, ((HUD) Astor.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis(), offset.get()).getRGB());
        GL11.glPopMatrix();
    }

    @Override
    public void update(int mousePosX, int mousePosY) {
        this.y = this.parentModule.category.getY() + this.offsetY;
        this.x = this.parentModule.category.getX();
    }

    public boolean mouseDown(int x, int y, int button) {
        if (this.isBinding) {
            if (button == 0) {
                this.stopBinding();
                return true;
            }
            this.setBind(button - 100);
            return true;
        }

        if (button == 0 && this.parentModule.panelExpand && this.isHovered(x, y)) {
            if (activeBinding != null && activeBinding != this) {
                activeBinding.stopBinding();
            }
            this.isBinding = true;
            activeBinding = this;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int x, int y, int button) {

    }

    @Override
    public boolean keyTyped(char chatTyped, int keyCode) {
        if (!this.isBinding) {
            return false;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {

            this.setBind(0);
            return true;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            this.setBind(this.parentModule.mod instanceof GuiModule ? 54 : 0);
        } else {
            this.setBind(keyCode);
        }
        return true;
    }

    private void setBind(int key) {
        if (key != 0) {
            for (Module other : Astor.moduleManager.modules.values()) {
                if (other != this.parentModule.mod && other.getKey() == key) {
                    ChatUtil.sendFormatted(String.format("%sUnbound &o%s&r, &o%s&r took the key", Astor.clientName, other.getName(), this.parentModule.mod.getName()));
                    break;
                }
            }
        } else if (this.parentModule.mod.getKey() != 0) {
            ChatUtil.sendFormatted(String.format("%sUnbound &o%s&r", Astor.clientName, this.parentModule.mod.getName()));
        }
        this.parentModule.mod.setKey(key);
        this.stopBinding();
    }

    private void stopBinding() {
        this.isBinding = false;
        if (activeBinding == this) {
            activeBinding = null;
        }
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
    }

    @Override
    public boolean isBinding() {
        return this.isBinding;
    }

    public boolean isHovered(int x, int y) {
        return x > this.x && x < this.x + this.parentModule.category.getWidth() && y > this.y - 1 && y < this.y + 12;
    }

    public int getHeight() {
        return 12;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    private void renderText(String s, int color) {
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(s, (float) ((this.parentModule.category.getX() + 4) * 2), (float) ((this.parentModule.category.getY() + this.offsetY + 3) * 2), color);
    }
}