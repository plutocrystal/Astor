package net.dev.astor.module.impl.player;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.util.ItemUtil;
import net.dev.astor.util.TeamUtil;
import net.dev.astor.property.properties.BooleanProperty;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class GhostHand extends Module {
    public final BooleanProperty teamsOnly = new BooleanProperty("TeamOnly", true);
    public final BooleanProperty ignoreWeapons = new BooleanProperty("IgnoreWeapons", false);

    public GhostHand() {
        super("GhostHand", Category.PLAYER, false);
    }

    public boolean shouldSkip(Entity entity) {
        return entity instanceof EntityPlayer
                && !TeamUtil.isBot((EntityPlayer) entity)
                && (!this.teamsOnly.getValue() || TeamUtil.isSameTeam((EntityPlayer) entity))
                && (!this.ignoreWeapons.getValue() || !ItemUtil.hasRawUnbreakingEnchant());
    }
}
