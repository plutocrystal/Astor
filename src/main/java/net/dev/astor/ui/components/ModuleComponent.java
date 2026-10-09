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
import java.util.List;
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
            List<Property<?>> declared = Astor.propertyManager.properties.get(mod.getClass());
            for (Property<?> baseProperty : declared) {
                if (baseProperty == mod.hide) {
                    // Rendered next to the bind instead of as its own row.
                    continue;
                }
                if (isNestedInGroup(declared, baseProperty)) {
                    // Rendered under its ListProperty's own row. Still in propertyManager's list, which
                    // is what keeps it in the config - only the row here is skipped.
                    continue;
                }
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
                } else if (baseProperty instanceof ListProperty) {
                    ListProperty property = (ListProperty) baseProperty;
                    ListComponent c = new ListComponent(property, this, y);
                    this.settings.add(c);
                    y += c.getHeight();
                }
            }
        }

        // Hide on the left, key bind on the right, sharing one row.
        this.settings.add(new HideComponent(this, y));
        this.settings.add(new BindComponent(this, y));
    }

    /**
     * Whether a property is a child of one of this module's groups, and so already rendered nested
     * under it. Compared by identity, which is what the child list holds.
     */
    private static boolean isNestedInGroup(List<Property<?>> declared, Property<?> candidate) {
        for (Property<?> property : declared) {
            if (property instanceof ListProperty && ((ListProperty) property).getChildren().contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    public void setComponentStartAt(int newOffsetY) {
        this.offsetY = newOffsetY;
        int y = this.offsetY + 16;

        for (int i = 0; i < this.settings.size(); i++) {
            Component c = this.settings.get(i);
            c.setComponentStartAt(y);
            // The last two (HideComponent and BindComponent) share one row, so y must not advance
            // for the first of the pair or the two end up staggered on separate rows.
            if (i < this.settings.size() - 2 && c.isVisible()) {
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
            for (int i = 0; i < this.settings.size(); i++) {
                Component c = this.settings.get(i);
                // Count the shared Hide/Bind row once instead of twice.
                if (c.isVisible() && i < this.settings.size() - 1) {
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
        // The whole row, not the name drawn in the middle of it: the box the category background
        // covers for this module. Bounds are inclusive at the low edge and exclusive at the high one
        // so the row is exactly width x 16 pixels - a strict comparison on both sides would drop the
        // first and last row of it, which is felt at the edges of a 92x16 target.
        return x >= this.category.getX() && x < this.category.getX() + this.category.getWidth()
                && y >= this.category.getY() + this.offsetY && y < this.category.getY() + 16 + this.offsetY;
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}