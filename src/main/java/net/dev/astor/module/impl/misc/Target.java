package net.dev.astor.module.impl.misc;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Single authority on which entities this client is allowed to act on. Every module that picks or
 * reacts to a target has to ask here, so nothing ever picks up a friend or a teammate.
 *
 * <p>Two levels of checks:</p>
 * <ul>
 *     <li>{@link #isTargetable(EntityLivingBase)} answers "may this kind of entity be targeted at
 *     all", purely from the four kind switches. Render modules use it, because they only care about
 *     what is worth drawing.</li>
 *     <li>{@link #isValidTarget(EntityLivingBase)} is the one entry point every module that acts on
 *     an entity must pass. It adds the self/rider/loaded guards, the friend list and the Teams
 *     module on top of the kind switches.</li>
 * </ul>
 *
 * <p>Range, fov, line of sight, sort order and switch behaviour stay with the individual modules,
 * because those genuinely differ per module.</p>
 *
 * <p>The kind switches apply whether or not the module itself is switched on, so other modules can
 * read them unconditionally. Teams is the one that gates on its own toggle, so turning Teams off
 * really does disable team detection everywhere.</p>
 */
public class Target extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Stand-in used before the module manager has registered the real instance. It carries the
     * default policy, which is exactly what an unregistered client should behave like.
     */
    private static final Target FALLBACK = new Target();

    public final BooleanProperty players = new BooleanProperty("Players", true);
    public final BooleanProperty mobs = new BooleanProperty("Mobs", false);
    public final BooleanProperty animals = new BooleanProperty("Animals", false);
    public final BooleanProperty golems = new BooleanProperty("Golems", false);
    /**
     * Whether entities that are playing their death animation count as targets. On by default, so
     * nothing changes for a client that was already willing to keep hitting a corpse.
     */
    public final BooleanProperty dead = new BooleanProperty("Dead", true);

    @Override
    public String getDescription() {
        return "The single authority on what this client is allowed to target, so nothing ever picks up a friend or a teammate.";
    }

    public Target() {
        super("Target", Category.MISC, true);
    }

    public static Target get() {
        if (Astor.moduleManager != null) {
            Target target = (Target) Astor.moduleManager.modules.get(Target.class);
            if (target != null) {
                return target;
            }
        }
        return Target.FALLBACK;
    }

    /**
     * Whether the entity is on the friend list or on our team. This is the exclusion half of
     * {@link #isValidTarget(EntityLivingBase)} on its own, for the cases that must skip friends and
     * teammates regardless of the kind switches, such as handing out items.
     */
    public boolean isFriendOrTeammate(EntityLivingBase entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof EntityPlayer
                && Astor.friendManager != null
                && Astor.friendManager.isFriend(entity.getName())) {
            return true;
        }
        return Teams.get().isInYourTeam(entity);
    }

    /**
     * Whether the entity kind is one this client is willing to target, ignoring anything about the
     * concrete entity beyond its class. This is the check render modules want: it never hides
     * yourself, friends or teammates, it only decides what kind of thing is worth drawing.
     */
    public boolean isTargetable(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            return this.players.getValue();
        }
        // Golems extend EntityCreature rather than EntityMob, but checked first anyway so a modded
        // mob hierarchy cannot swallow them.
        if (entity instanceof EntityGolem) {
            return this.golems.getValue();
        }
        // EntityMob covers the wither, silverfish, creepers, endermen, blazes, ghasts and spiders;
        // slimes and the ender dragon sit outside it despite being hostile.
        if (entity instanceof EntityMob || entity instanceof EntitySlime || entity instanceof EntityDragon) {
            return this.mobs.getValue();
        }
        if (entity instanceof EntityAnimal
                || entity instanceof EntityBat
                || entity instanceof EntitySquid
                || entity instanceof EntityVillager) {
            return this.animals.getValue();
        }
        return false;
    }

    /**
     * The check every module that acts on an entity must use. Skips anything we cannot hit, plus
     * friends and teammates, then defers to {@link #isTargetable(EntityLivingBase)}.
     *
     * <p>Entities playing their death animation stay valid while {@link #dead} is on, so a client that
     * wants to keep hitting a corpse can.</p>
     */
    public boolean isValidTarget(EntityLivingBase entity) {
        if (entity == null || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }
        if (!mc.theWorld.loadedEntityList.contains(entity)) {
            return false;
        }
        if (entity == mc.thePlayer || entity == mc.thePlayer.ridingEntity) {
            return false;
        }
        Entity viewEntity = mc.getRenderViewEntity();
        if (entity == viewEntity || entity == viewEntity.ridingEntity) {
            return false;
        }
        if (this.isFriendOrTeammate(entity)) {
            return false;
        }
        // A dying entity keeps ticking through onDeathUpdate for 20 ticks and only then stops being
        // simulated, so isDead alone would miss the whole window where the corpse can still be hit.
        if (!this.dead.getValue() && this.isDying(entity)) {
            return false;
        }
        return this.isTargetable(entity);
    }

    /**
     * Whether the entity is dead or on its way there - the death animation is still running. Mirrors what
     * {@link net.minecraft.entity.EntityLivingBase#isEntityAlive()} reports inverted, plus deathTime so
     * an entity that already reached zero health is caught even before the flag flips.
     */
    private boolean isDying(EntityLivingBase entity) {
        return entity.isDead || entity.getHealth() <= 0.0F || entity.deathTime > 0;
    }

    /**
     * Convenience wrapper for call sites that only have an untyped entity, such as the ray trace
     * results and attack packets.
     */
    public boolean isValidTarget(Entity entity) {
        return entity instanceof EntityLivingBase && this.isValidTarget((EntityLivingBase) entity);
    }
}
