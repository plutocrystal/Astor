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

public class Target extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Target FALLBACK = new Target();

    public final BooleanProperty players = new BooleanProperty("Players", true);
    public final BooleanProperty mobs = new BooleanProperty("Mobs", false);
    public final BooleanProperty animals = new BooleanProperty("Animals", false);
    public final BooleanProperty golems = new BooleanProperty("Golems", false);
    
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

    public boolean isTargetable(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            return this.players.getValue();
        }
        
        if (entity instanceof EntityGolem) {
            return this.golems.getValue();
        }
        
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
        
        if (!this.dead.getValue() && this.isDying(entity)) {
            return false;
        }
        return this.isTargetable(entity);
    }

    private boolean isDying(EntityLivingBase entity) {
        return entity.isDead || entity.getHealth() <= 0.0F || entity.deathTime > 0;
    }

    public boolean isValidTarget(Entity entity) {
        return entity instanceof EntityLivingBase && this.isValidTarget((EntityLivingBase) entity);
    }
}

