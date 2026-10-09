package net.dev.astor.ui.components;

import net.dev.astor.module.Module;
import net.dev.astor.ui.Component;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CategoryComponent {
    public ArrayList<Component> modulesInCategory = new ArrayList<>();
    public String categoryName;
    private boolean categoryOpened;
    private int width;
    private int y;
    private int x;
    private final int bh;
    public boolean dragging;
    public int xx;
    public int yy;
    private double marginY, marginX;
    private int height = 0;
    
    private int scrollOffset = 0;

    public CategoryComponent(String category, List<Module> modules) {
        this.categoryName = category;
        this.width = 92;
        this.x = 5;
        this.y = 5;
        this.bh = 13;
        this.xx = 0;
        this.categoryOpened = false;
        this.dragging = false;
        int tY = this.bh + 3;
        this.marginX = 80;
        this.marginY = 4.5;
        for (Module mod : modules) {
            ModuleComponent b = new ModuleComponent(mod, this, tY);
            this.modulesInCategory.add(b);
            tY += 16;
        }
        updateHeight();
        layout();
    }

    public ArrayList<Component> getModules() {
        return this.modulesInCategory;
    }

    public void setX(int n) {
        this.x = n;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setScrollOffset(int offset) {
        this.scrollOffset = offset;
    }

    public void setDragging(boolean d) {
        this.dragging = d;
    }

    public boolean isOpened() {
        return this.categoryOpened;
    }

    public void setOpened(boolean on) {
        this.categoryOpened = on;
        if (on) {
            this.dragging = false;
        }
    }

    public void render(FontRenderer renderer) {
        this.width = 92;
        updateHeight();
        layout();
        final int sy = getY();
        if (this.categoryOpened && !this.modulesInCategory.isEmpty()) {
            Gui.drawRect(this.x - 1, sy, this.x + this.width + 1, sy + this.bh + this.height + 4, new Color(0, 0, 0, 100).getRGB());
        }
        Gui.drawRect(this.x - 2, sy, this.x + this.width + 2, sy + this.bh + 3, new Color(0, 0, 0, 200).getRGB());
        renderer.drawString(this.categoryName, (float) (this.x + 2), (float) (sy + 4), -1, false);
        renderer.drawString(this.categoryOpened ? "-" : "+", (float) (this.x + marginX), (float) (sy + marginY), Color.white.getRGB(), false);
        if (this.categoryOpened && !this.modulesInCategory.isEmpty()) {
            for (Component c2 : this.modulesInCategory) {
                c2.draw(new AtomicInteger(0));
            }
        }
    }

    private void layout() {
        int renderHeight = this.bh + 3;
        for (Component component : this.modulesInCategory) {
            component.setComponentStartAt(renderHeight);
            renderHeight += component.getHeight();
        }
    }

    private void updateHeight() {
        int h = 0;
        for (Component component : this.modulesInCategory) {
            h += component.getHeight();
        }
        this.height = h;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y - this.scrollOffset;
    }

    public int getUnscolledY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public void handleDrag(int x, int y) {
        if (this.dragging) {
            this.setX(x - this.xx);
            
            this.setY(y - this.yy + this.scrollOffset);
        }
    }

    public boolean handleHeaderClick(int x, int y, int button) {
        if (button != 0 || !this.insideArea(x, y)) {
            return false;
        }
        if (this.isToggleHovered(x, y)) {
            this.setOpened(!this.isOpened());
        } else {
            this.dragging = true;
            this.xx = x - this.getX();
            this.yy = y - this.getY();
        }
        return true;
    }

    public boolean isInsideBody(int x, int y) {
        if (!this.categoryOpened) {
            return false;
        }
        final int sy = getY();
        int top = sy + this.bh + 3;
        return this.height > 0 && x >= this.x && x <= this.x + this.width && y >= top && y < top + this.height;
    }

    private boolean isToggleHovered(int x, int y) {
        final int sy = getY();
        return x >= this.x + 77 && x <= this.x + this.width - 6 && (float) y >= (float) sy + 2.0F && y <= sy + this.bh + 1;
    }

    private boolean insideArea(int x, int y) {
        final int sy = getY();
        return x >= this.x && x <= this.x + this.width && y >= sy && y <= sy + this.bh + 3;
    }

    public String getName() {
        return categoryName;
    }

    public void setLocation(int parseInt, int parseInt1) {
        this.x = parseInt;
        this.y = parseInt1;
    }
}