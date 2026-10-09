package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.IntProperty;

/**
 * Sets how many of each hit particle plays.
 *
 * <p>Vanilla plays a single burst of each, and only when its own checks pass: a critical needs the
 * player airborne and falling, an enchantment burst needs the weapon to have applied a damage bonus.
 * Each property is the number of bursts for one particle type, so the two are set independently.
 * Above one it also plays the burst on the hits where vanilla stayed silent, which is the only thing
 * a count can add that vanilla's own conditions would not.</p>
 *
 * <p>Zero means vanilla rather than off. The module is here to change the particles, and zero is
 * indistinguishable from never having turned it on - one burst when the hit qualified for one, none
 * when it did not.</p>
 */
public class Particles extends Module {
    /**
     * Disabled stand-in used before the module manager has registered the real instance. It answers
     * exactly what vanilla would, so an unregistered client is unaffected.
     */
    private static final Particles FALLBACK = new Particles();

    /** Enchantment-particle bursts, from the weapon's damage bonus. */
    public final IntProperty sharpness = new IntProperty("Sharpness", 0, 0, 10);

    /** Critical-particle bursts, from the falling-while-airborne check. */
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

    /**
     * @param should whether the swing produced a critical on its own
     * @return how many critical bursts to play
     */
    public static int getCriticalsMultiplier(boolean should) {
        return Particles.get().count(Particles.get().criticals, should);
    }

    /**
     * @param should whether the swing produced an enchantment burst on its own
     * @return how many enchantment bursts to play
     */
    public static int getSharpnessMultiplier(boolean should) {
        return Particles.get().count(Particles.get().sharpness, should);
    }

    /**
     * The burst count for one particle type.
     *
     * <p>A disabled module and a count of zero both fall back to {@code should}, which is what makes
     * zero vanilla: the module's job is to change the particles, so turning it off - or asking for
     * nothing - has to hand the type back rather than delete it.</p>
     *
     * @param count  the type's burst count
     * @param should whether vanilla produced this type on its own
     */
    private int count(IntProperty count, boolean should) {
        if (!this.isEnabled()) {
            return should ? 1 : 0;
        }
        int bursts = count.getValue();
        return bursts > 0 ? bursts : (should ? 1 : 0);
    }
}
