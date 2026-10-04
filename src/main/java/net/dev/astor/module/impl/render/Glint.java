package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.ColorProperty;

public class Glint extends Module {
    public final ColorProperty color = new ColorProperty("Color", 0xFF8040CC);

    public Glint() {
        super("Glint", Category.RENDER, false);
    }

    public int getTint() {
        return this.color.getValue() & 0xFFFFFF | 0xFF000000;
    }
}