package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.FloatProperty;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class AspectRatio extends Module {
    private static final DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
    public final FloatProperty ratio = new FloatProperty("Ratio", 1.78F, 0.0F, 5.0F);

    @Override
    public String getDescription() {
        return "Stretches the screen to a different aspect ratio than your display.";
    }

    public AspectRatio() {
        super("AspectRatio", Category.RENDER, false);
    }

    public float getAspect(float vanilla) {
        float value = this.ratio.getValue();
        return value > 0.0F ? value : vanilla;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.ratio.getValue()) + "x"};
    }
}
