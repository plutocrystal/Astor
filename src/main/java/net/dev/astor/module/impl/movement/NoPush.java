package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;

/**
 * Cancels the pushes vanilla applies to the local player. Entities covers the separation that
 * {@code Entity.applyEntityCollision} applies when another entity walks into the player, blocks
 * covers {@code EntityPlayerSP.pushOutOfBlocks}, the ejection that fires when the player ends up
 * inside a block. Both are cancelled at their source in the mixins, so this module itself only
 * carries the settings.
 */
public class NoPush extends Module {
    public final BooleanProperty entities = new BooleanProperty("Entities", true);
    public final BooleanProperty blocks = new BooleanProperty("Blocks", true);

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