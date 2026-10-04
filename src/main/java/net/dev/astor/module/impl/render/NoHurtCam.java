package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.PercentProperty;

public class NoHurtCam extends Module {
    public final PercentProperty multiplier = new PercentProperty("Multiplier", 0);

    public NoHurtCam() {
        super("NoHurtCam", Category.RENDER, false, true);
    }
}