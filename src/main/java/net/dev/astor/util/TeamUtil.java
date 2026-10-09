package net.dev.astor.util;

import net.dev.astor.Astor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class TeamUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isEntityLoaded(Entity entity) {
        if (entity == null) return false;
        return TeamUtil.mc.theWorld.loadedEntityList.contains(entity);
    }

    public static List<Entity> getLoadedEntitiesSorted() {
        return TeamUtil.mc.theWorld.loadedEntityList.stream().sorted((entity1, entity2) -> {
            double dist1 = mc.getRenderManager().getDistanceToCamera(entity1.posX, entity1.posY, entity1.posZ);
            double dist2 = mc.getRenderManager().getDistanceToCamera(entity2.posX, entity2.posY, entity2.posZ);
            if (dist1 < dist2) {
                return 1;
            }
            if (dist1 > dist2) {
                return -1;
            }
            return entity1.getUniqueID().toString().compareTo(entity2.getUniqueID().toString());
        }).collect(Collectors.toList());
    }

    /**
     * Health weighted by armour, used to sort the squishiest target first. An entity with no armour
     * scores 20 points per point of health, so the armour division has to fall back to one rather
     * than divide by zero.
     */
    public static float getHealthScore(EntityLivingBase entityLivingBase) {
        int armor = entityLivingBase.getTotalArmorValue();
        return entityLivingBase.getHealth() * (armor > 0 ? 20.0f / (float) armor : 20.0f);
    }

    public static String stripName(Entity entity) {
        return entity.getDisplayName().getFormattedText().replaceAll("§\\S$", "").replaceAll("(?i)§r", "§f").trim();
    }

    public static Color getTeamColor(EntityPlayer player, float alpha) {
        int colorCode = 0xFFFFFF;
        ScorePlayerTeam playerTeam = (ScorePlayerTeam) player.getTeam();
        if (playerTeam != null) {
            String colorPrefix = FontRenderer.getFormatFromString(playerTeam.getColorPrefix());
            if (colorPrefix.length() >= 2) {
                colorCode = TeamUtil.mc.fontRendererObj.getColorCode(colorPrefix.charAt(1));
            }
        }
        return new Color(colorCode & 0xFFFFFF | (int)(alpha * 255) << 24, true);
    }

    /**
     * @deprecated team detection now lives in the Teams module, which also covers the name colour,
     * leather dye and GommeSW prefix hints. Use
     * {@link net.dev.astor.module.impl.misc.Teams#get().isInYourTeam(EntityLivingBase)} instead.
     */
    @Deprecated
    public static boolean isSameTeam(EntityPlayer player) {
        if (player == TeamUtil.mc.thePlayer) {
            return true;
        }
        NetworkPlayerInfo selfInfo = mc.getNetHandler().getPlayerInfo(TeamUtil.mc.thePlayer.getUniqueID());
        if (selfInfo == null) {
            return false;
        }
        ScorePlayerTeam selfTeam = selfInfo.getPlayerTeam();
        if (selfTeam == null) {
            return false;
        }
        NetworkPlayerInfo targetInfo = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (targetInfo == null) {
            return false;
        }
        ScorePlayerTeam targetTeam = targetInfo.getPlayerTeam();
        if (targetTeam == null) {
            return false;
        }
        return selfTeam.getColorPrefix().equals(targetTeam.getColorPrefix());
    }

    public static boolean isShop(EntityLivingBase entity) {
        if (entity == TeamUtil.mc.thePlayer) {
            return false;
        }
        EntityLivingBase armorStand = TeamUtil.mc.theWorld.findNearestEntityWithinAABB(EntityArmorStand.class, entity.getEntityBoundingBox(), entity);
        if (armorStand == null) return false;
        String displayName = armorStand.getName();
        if (displayName.contains("RIGHT CLICK")) return true;
        if (displayName.contains("ITEM SHOP")) return true;
        if (displayName.contains("UPGRADES")) return true;
        if (displayName.contains("BANKER")) return true;
        return displayName.contains("STREAK POWERS");
    }

    public static boolean isFriend(EntityPlayer player) {
        return Astor.friendManager.isFriend(player.getName());
    }

    public static boolean isTarget(EntityPlayer player) {
        return Astor.targetManager.isFriend(player.getName());
    }

    /**
     * Colour the friend and enemy lists paint a player with, or null when the entity is on neither
     * list. Shared so every renderer paints the same player the same way instead of each one
     * re-deriving the friend-then-target precedence.
     */
    public static Color getListColor(Entity entity) {
        if (!(entity instanceof EntityPlayer)) {
            return null;
        }
        if (isFriend((EntityPlayer) entity)) {
            return Astor.friendManager.getColor();
        }
        if (isTarget((EntityPlayer) entity)) {
            return Astor.targetManager.getColor();
        }
        return null;
    }
}