package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.ModeProperty;

import java.util.Locale;

public class ClientSetting extends Module {

    private static final String[] SOUND_PACKS = {"Vanilla", "FDP", "Sigma", "QuickMacro", "Rise"};
    private static final int VANILLA = 0;
    private static final String VANILLA_SOUND = "random.click";
    private static final String SOUND_NAMESPACE = "astor";

    public final BooleanProperty toggleSounds = new BooleanProperty("ToggleSounds", true);

    public final ModeProperty toggleSound = new ModeProperty(
            "ToggleSound", 0, SOUND_PACKS, () -> this.toggleSounds.getValue()
    );
    public final BooleanProperty toggleAlerts = new BooleanProperty("ToggleAlerts", false);
    public final BooleanProperty boldShadow = new BooleanProperty("BoldShadow", true);

    @Override
    public String getDescription() {
        return "Client-wide options: toggle sounds, alerts and text styling.";
    }

    public ClientSetting() {
        super("ClientSetting", Category.RENDER, false);
    }

    public String getToggleSound(boolean enabled) {
        int pack = this.toggleSound.getValue();
        if (pack <= VANILLA || pack >= SOUND_PACKS.length) {
            return VANILLA_SOUND;
        }
        return String.format(
                "%s:%s/%s",
                SOUND_NAMESPACE,
                SOUND_PACKS[pack].toLowerCase(Locale.ROOT),
                enabled ? "enable" : "disable"
        );
    }
}
