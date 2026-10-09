package net.dev.astor.module.impl.movement;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.FloatProperty;

public class Sneak extends Module {
    public final FloatProperty speed = new FloatProperty("Speed", 0.3F, 0.0F, 2.0F, 2);

    public Sneak() {
        super("Sneak", Category.MOVEMENT, false);
    }

    @Override
    public String getDescription() {
        return "Sets how fast you move while crouching. Vanilla scales the movement input down to 0.3.";
    }

    public static Sneak get() {
        if (Astor.moduleManager != null) {
            Sneak sneak = (Sneak) Astor.moduleManager.modules.get(Sneak.class);
            if (sneak != null) {
                return sneak;
            }
        }
        return null;
    }
}