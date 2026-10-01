package net.dev.astor.module.impl.player;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;

public class AntiDebuff extends Module {
    public final BooleanProperty blindness = new BooleanProperty("Blindness", true);
    public final BooleanProperty nausea = new BooleanProperty("Nausea", true);

    public AntiDebuff() {
        super("AntiDebuff", Category.PLAYER, false);
    }
}
