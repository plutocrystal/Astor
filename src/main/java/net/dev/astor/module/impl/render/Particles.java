package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.IntProperty;

public class Particles extends Module {
    
    private static final Particles FALLBACK = new Particles();

    public final IntProperty sharpness = new IntProperty("Sharpness", 0, 0, 10);

    public final IntProperty criticals = new IntProperty("Criticals", 0, 0, 10);

    @Override
    public String getDescription() {
        return "Sets how many critical and enchantment hit particles play. Zero leaves them on vanilla settings.";
    }

    public Particles() {
        super("Particles", Category.RENDER, false);
    }

    public static Particles get() {
        if (Astor.moduleManager != null) {
            Particles particles = (Particles) Astor.moduleManager.modules.get(Particles.class);
            if (particles != null) {
                return particles;
            }
        }
        return FALLBACK;
    }

    public static int getCriticalsMultiplier(boolean should) {
        return Particles.get().count(Particles.get().criticals, should);
    }

    public static int getSharpnessMultiplier(boolean should) {
        return Particles.get().count(Particles.get().sharpness, should);
    }

    private int count(IntProperty count, boolean should) {
        if (!this.isEnabled()) {
            return should ? 1 : 0;
        }
        int bursts = count.getValue();
        return bursts > 0 ? bursts : (should ? 1 : 0);
    }
}

