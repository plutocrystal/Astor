package net.dev.astor.ui.components;

import net.dev.astor.Astor;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.render.HUD;
import net.dev.astor.property.Property;
import net.dev.astor.property.properties.*;
import net.dev.astor.ui.Component;
import net.dev.astor.ui.dataset.impl.FloatSlider;
import net.dev.astor.ui.dataset.impl.IntSlider;
import net.dev.astor.ui.dataset.impl.PercentageSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleComponent implements Component {
    public Module mod;
    public CategoryComponent category;
    public int offsetY;
    private final ArrayList<Component> settings;
    public boolean panelExpand;

    public ModuleComponent(Module mod, CategoryComponent category, int offsetY) {
        this.mod = mod;
        this.category = category;
        this.offsetY = offsetY;
        this.settings = new ArrayList<>();
        this.panelExpand = false;
        int y = offsetY + 16;
        if (!Astor.propertyManager.properties.get(mod.getClass()).isEmpty()) {
            for (Property<?> baseProperty : Astor.propertyManager.properties.get(mod.getClass())) {
                if (baseProperty instanceof BooleanProperty) {
                    BooleanProperty property = (BooleanProperty) baseProperty;
                    CheckBoxComponent c = new CheckBoxComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof FloatProperty) {
                    FloatProperty property = (FloatProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new FloatSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof IntProperty) {
                    IntProperty property = (IntProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new IntSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof PercentProperty) {
                    PercentProperty property = (PercentProperty) baseProperty;
                    SliderComponent c = new SliderComponent(new PercentageSlider(property), this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ModeProperty) {
                    ModeProperty property = (ModeProperty) baseProperty;
                    ModeComponent c = new ModeComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ColorProperty) {
                    ColorProperty property = (ColorProperty) baseProperty;
                    ColorPickerComponent c = new ColorPickerComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof TextProperty) {
                    TextProperty property = (TextProperty) baseProperty;
                    TextComponent c = new TextComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                } else if (baseProperty instanceof ButtonProperty) {
                    ButtonProperty property = (ButtonProperty) baseProperty;
                    ButtonComponent c = new ButtonComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                }
            }
        }

        this.settings.add(new BindComponent(this, y));
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        int y = this.offsetY + 16;

        for (Component c : this.settings) {
            c.setComponentStartAt(y);
            if (c.isVisible()) {
                y += c.getHeight();
            }
        }
    }

    public void draw(AtomicInteger offset) {
        int textColor;
        if (this.mod.isEnabled()) {
            textColor = ((HUD) Astor.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis(), offset.get()).getRGB();
        } else {
            textColor = new Color(102, 102, 102).getRGB();
        }
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(this.mod.getName(), (float) (this.category.getX() + this.category.getWidth() / 2 - Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.mod.getName()) / 2), (float) (this.category.getY() + this.offsetY + 4), textColor);
        if (this.panelExpand && !this.settings.isEmpty()) {
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    c.draw(offset);
                    offset.incrementAndGet();
                }
            }
        }

    }

    public int getHeight() {
        if (!this.panelExpand) {
            return 16;
        } else {
            int h = 16;
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    h += c.getHeight();
                }
            }
            return h;
        }
    }

    public void update(int mousePosX, int mousePosY) {
        if(!panelExpand) return;
        if (!this.settings.isEmpty()) {
            for (Component c : this.settings) {
                if (c.isVisible()) {
                    c.update(mousePosX, mousePosY);
                }
            }
        }

    }

    public boolean mouseDown(int x, int y, int button) {
        if (this.isHovered(x, y)) {
            if (button == 0) {
                this.mod.toggle();
                return true;
            }
            if (button == 1) {
                this.panelExpand = !this.panelExpand;
                return true;
            }
            return false;
        }

        if (!this.panelExpand) return false;
        for (int i = this.settings.size() - 1; i >= 0; i--) {
            Component c = this.settings.get(i);
            if (c.isVisible() && c.mouseDown(x, y, button)) {
                return true;
            }
        }
        return false;
    }

    public void mouseReleased(int x, int y, int button) {
        for (Component c : this.settings) {
            if (c.isVisible()) {
                c.mouseReleased(x, y, button);
            }
        }

    }

    public boolean keyTyped(char chatTyped, int keyCode) {
        if (!this.panelExpand) return false;
        for (int i = this.settings.size() - 1; i >= 0; i--) {
            Component c = this.settings.get(i);
            if (c.isVisible() && c.keyTyped(chatTyped, keyCode)) {
                return true;
            }
        }
        return false;
    }

    public boolean isHovered(int x, int y) {
        return x > this.category.getX() && x < this.category.getX() + this.category.getWidth() && y > this.category.getY() + this.offsetY && y < this.category.getY() + 16 + this.offsetY;
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}