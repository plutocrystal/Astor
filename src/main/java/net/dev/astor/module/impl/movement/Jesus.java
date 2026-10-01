package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.FloatProperty;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Jesus extends Module {
    private static final DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
    public final FloatProperty speed = new FloatProperty("Speed", 2.5F, 0.0F, 3.0F);
    public final BooleanProperty noPush = new BooleanProperty("NoPush", true);
    public final BooleanProperty groundOnly = new BooleanProperty("GroundOnly", true);

    public Jesus() {
        super("Jesus", Category.MOVEMENT, false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.speed.getValue())};
    }
}
