package net.dev.astor.module.impl.player;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.misc.Target;
import net.dev.astor.util.ItemUtil;
import net.dev.astor.property.properties.BooleanProperty;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class GhostHand extends Module {
    public final BooleanProperty ignoreWeapons = new BooleanProperty("IgnoreWeapons", false);

    @Override
    public String getDescription() {
        return "Lets you click through entities instead of hitting them, so your hand never lands on the wrong thing.";
    }

    public GhostHand() {
        super("GhostHand", Category.PLAYER, false);
    }

    /**
     * Whether the entity should be taken out of the list of things the camera ray can hit, so the
     * player can click straight through it. Target owns the friend and teammate check, so this
     * module can never be talked into handing items to one.
     */
    public boolean shouldSkip(Entity entity) {
        return entity instanceof EntityLivingBase
                && (Target.get().isFriendOrTeammate((EntityLivingBase) entity)
                || !this.ignoreWeapons.getValue() || !ItemUtil.hasRawUnbreakingEnchant());
    }
}
