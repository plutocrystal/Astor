package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.FloatProperty;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Stretches the 3D projection to a chosen width over height instead of whatever the window
 * happens to be. The projection is the only thing that moves: raycasts still run off the real
 * coordinates, so aiming and the target boxes stay honest while the picture is pulled around them.
 */
public class AspectRatio extends Module {
    private static final DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
    public final FloatProperty ratio = new FloatProperty("Ratio", 1.78F, 0.0F, 5.0F);

    public AspectRatio() {
        super("AspectRatio", Category.RENDER, false);
    }

    /**
     * A projection built with a width of zero collapses and the screen comes back black. The
     * property only rejects negative values, so zero has to be answered with the window's own
     * aspect rather than passed on.
     */
    public float getAspect(float vanilla) {
        float value = this.ratio.getValue();
        return value > 0.0F ? value : vanilla;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.ratio.getValue()) + "x"};
    }
}