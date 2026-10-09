package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.PercentProperty;

public class Scoreboard extends Module {

    private static final int MAX_ALPHA = 255;

    public final PercentProperty background = new PercentProperty("Background", 0);

    @Override
    public String getDescription() {
        return "Makes the sidebar scoreboard background adjustable. At 0% the background is gone and "
                + "only the text is left.";
    }

    public Scoreboard() {
        super("Scoreboard", Category.RENDER, false);
    }

    public static Scoreboard get() {
        if (Astor.moduleManager == null) {
            return null;
        }
        Scoreboard scoreboard = (Scoreboard) Astor.moduleManager.modules.get(Scoreboard.class);
        return scoreboard != null && scoreboard.isEnabled() ? scoreboard : null;
    }

    /**
     * The slider as an alpha byte, ready to drop into the top of a colour.
     *
     * <p>0% comes out as 0, which the blend function discards, so nothing is drawn at all. 100% comes
     * out as 255, a solid black that is darker than either colour vanilla uses here - vanilla draws its
     * rows at 31% and its header at 38%.</p>
     */
    public int getBackgroundAlpha() {
        return Math.round(this.background.getValue() / 100.0F * MAX_ALPHA);
    }
}