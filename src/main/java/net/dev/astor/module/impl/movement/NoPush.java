package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;

public class NoPush extends Module {
    public final BooleanProperty entities = new BooleanProperty("Entities", true);
    public final BooleanProperty blocks = new BooleanProperty("Blocks", true);

    @Override
    public String getDescription() {
        return "Stops entities and blocks from pushing you.";
    }

    public NoPush() {
        super("NoPush", Category.MOVEMENT, false);
    }

    public boolean cancelEntityPush() {
        return this.isEnabled() && this.entities.getValue();
    }

    public boolean cancelBlockPush() {
        return this.isEnabled() && this.blocks.getValue();
    }
}
