package net.dev.astor.ui.components;

import net.dev.astor.property.Property;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.ColorProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.ListProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.property.properties.PercentProperty;
import net.dev.astor.property.properties.TextProperty;
import net.dev.astor.ui.Component;
import net.dev.astor.ui.dataset.impl.FloatSlider;
import net.dev.astor.ui.dataset.impl.IntSlider;
import net.dev.astor.ui.dataset.impl.PercentageSlider;
import net.dev.astor.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ListComponent implements Component {
    private static final int ROW_HEIGHT = 12;
    private static final int TRIANGLE_SIZE = 4;

    private static final int CHILD_INDENT = 4;

    private static final int TREE_X = 4;
    private static final int TREE_TOP = 10;
    private static final int TREE_BRANCH = 3;

    private static final int LINE = 1;

    private static final Color GOLD = ColorUtil.GOLD;

    private final ListProperty property;
    private final ModuleComponent module;
    private final List<Component> children = new ArrayList<>();
    private boolean expanded;
    private int offsetY;
    private int x;
    private int y;

    public ListComponent(ListProperty property, ModuleComponent parentModule, int offsetY) {
        this.property = property;
        this.module = parentModule;
        this.x = parentModule.category.getX();
        this.y = parentModule.category.getY() + parentModule.offsetY;
        this.offsetY = offsetY;
        for (Property<?> child : property.getChildren()) {
            Component c = this.createChild(child);
            if (c != null) {
                this.children.add(c);
            }
        }
        this.layoutChildren();
    }

    private Component createChild(Property<?> child) {
        
        if (child instanceof BooleanProperty) {
            return new CheckBoxComponent((BooleanProperty) child, this.module, 0);
        } else if (child instanceof FloatProperty) {
            return new SliderComponent(new FloatSlider((FloatProperty) child), this.module, 0);
        } else if (child instanceof IntProperty) {
            return new SliderComponent(new IntSlider((IntProperty) child), this.module, 0);
        } else if (child instanceof PercentProperty) {
            return new SliderComponent(new PercentageSlider((PercentProperty) child), this.module, 0);
        } else if (child instanceof ModeProperty) {
            return new ModeComponent((ModeProperty) child, this.module, 0);
        } else if (child instanceof ColorProperty) {
            return new ColorPickerComponent((ColorProperty) child, this.module, 0);
        } else if (child instanceof TextProperty) {
            return new TextComponent((TextProperty) child, this.module, 0);
        }
        return null;
    }

    private void layoutChildren() {
        int childY = this.offsetY + ROW_HEIGHT;
        for (Component c : this.children) {
            c.setComponentStartAt(childY);
            if (c.isVisible()) {
                childY += c.getHeight();
            }
        }
    }

    public void draw(AtomicInteger offset) {
        int baseX = this.module.category.getX() + 4;
        int rowY = this.module.category.getY() + this.offsetY;

        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        Minecraft.getMinecraft()
                .fontRendererObj
                .drawString(this.property.getName().replace("-", " "), (float) (baseX * 2), (float) ((rowY + 5) * 2), -1, false);
        GL11.glPopMatrix();

        this.drawTriangle(rowY);
        this.drawTree(rowY);

        if (this.expanded) {
            
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(CHILD_INDENT, 0.0F, 0.0F);
                for (Component c : this.children) {
                    if (c.isVisible()) {
                        c.draw(offset);
                        offset.incrementAndGet();
                    }
                }
            } finally {
                GL11.glPopMatrix();
            }
        }
    }

    private void drawTree(int headerRowY) {
        if (!this.expanded) {
            return;
        }
        List<Integer> rowCenters = new ArrayList<>();
        int childY = this.offsetY + ROW_HEIGHT;
        for (Component c : this.children) {
            if (c.isVisible()) {
                rowCenters.add(this.module.category.getY() + childY + c.getHeight() / 2);
                childY += c.getHeight();
            }
        }
        if (rowCenters.isEmpty()) {
            return;
        }
        int trunkX = this.module.category.getX() + TREE_X;
        int gold = GOLD.getRGB();

        int last = rowCenters.get(rowCenters.size() - 1);
        Gui.drawRect(trunkX, headerRowY + TREE_TOP, trunkX + LINE, last + LINE, gold);
        for (int center : rowCenters) {
            Gui.drawRect(trunkX, center, trunkX + TREE_BRANCH, center + LINE, gold);
        }
    }

    private void drawTriangle(int rowY) {
        int right = this.module.category.getX() + this.module.category.getWidth() - 8;
        int top = rowY + 3;
        int left = right - TRIANGLE_SIZE;
        int middle = left + TRIANGLE_SIZE / 2;
        int bottom = top + TRIANGLE_SIZE;

        GL11.glPushMatrix();
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(GOLD.getRed() / 255.0F, GOLD.getGreen() / 255.0F, GOLD.getBlue() / 255.0F, 1.0F);

        GL11.glBegin(GL11.GL_TRIANGLES);
        if (this.expanded) {
            
            GL11.glVertex2i(left, top);
            GL11.glVertex2i(left + TRIANGLE_SIZE, top);
            GL11.glVertex2i(middle, bottom);
        } else {
            
            GL11.glVertex2i(left, bottom);
            GL11.glVertex2i(left + TRIANGLE_SIZE, bottom);
            GL11.glVertex2i(middle, top);
        }
        GL11.glEnd();

        GlStateManager.resetColor();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    @Override
    public int getHeight() {
        int h = ROW_HEIGHT;
        if (this.expanded) {
            for (Component c : this.children) {
                if (c.isVisible()) {
                    h += c.getHeight();
                }
            }
        }
        return h;
    }

    public void update(int mousePosX, int mousePosY) {
        this.x = this.module.category.getX();
        this.y = this.module.category.getY() + this.offsetY;
        for (Component c : this.children) {
            c.update(mousePosX - CHILD_INDENT, mousePosY);
        }
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        this.layoutChildren();
    }

    public boolean mouseDown(int x, int y, int button) {
        if (!this.module.panelExpand) {
            return false;
        }
        if (this.expanded) {
            for (int i = this.children.size() - 1; i >= 0; i--) {
                Component c = this.children.get(i);
                if (c.isVisible() && c.mouseDown(x - CHILD_INDENT, y, button)) {
                    return true;
                }
            }
        }
        if (button == 0 && this.isHovered(x, y)) {
            this.expanded = !this.expanded;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        
        for (Component c : this.children) {
            if (c.isVisible()) {
                c.mouseReleased(x - CHILD_INDENT, y, button);
            }
        }
    }

    @Override
    public boolean keyTyped(char chatTyped, int keyCode) {
        if (!this.expanded) {
            return false;
        }
        for (int i = this.children.size() - 1; i >= 0; i--) {
            Component c = this.children.get(i);
            if (c.isVisible() && c.keyTyped(chatTyped, keyCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHovered(int x, int y) {
        return x > this.x && x < this.x + this.module.category.getWidth() && y > this.y && y < this.y + ROW_HEIGHT - 1;
    }

    @Override
    public boolean isVisible() {
        return this.property.isVisible();
    }
}

