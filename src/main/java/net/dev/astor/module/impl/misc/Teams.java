package net.dev.astor.module.impl.misc;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.IChatComponent;

public class Teams extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final Teams FALLBACK = new Teams();

    public final BooleanProperty scoreboardTeam = new BooleanProperty("ScoreboardTeam", true);
    public final BooleanProperty nameColor = new BooleanProperty("NameColor", true);
    public final BooleanProperty armorColor = new BooleanProperty("ArmorColor", true);
    public final BooleanProperty gommeSW = new BooleanProperty("GommeSW", false);

    @Override
    public String getDescription() {
        return "Works out who is on your team from cosmetics and behaviour, for servers that hide the scoreboard teams.";
    }

    public Teams() {
        super("Teams", Category.MISC, true);
    }

    public static Teams get() {
        if (Astor.moduleManager != null) {
            Teams teams = (Teams) Astor.moduleManager.modules.get(Teams.class);
            if (teams != null) {
                return teams;
            }
        }
        return Teams.FALLBACK;
    }

    public boolean isInYourTeam(EntityLivingBase entity) {
        if (entity == null || mc.thePlayer == null) {
            return false;
        }
        Teams teams = Teams.get();
        if (!teams.isEnabled()) {
            return false;
        }
        if (teams.scoreboardTeam.getValue() && this.isSameScoreboardTeam(mc.thePlayer, entity)) {
            return true;
        }
        String clientName = stripReset(mc.thePlayer.getDisplayName());
        String targetName = stripReset(entity.getDisplayName());
        if (teams.gommeSW.getValue() && this.isSameGommePrefix(clientName, targetName)) {
            return true;
        }
        if (teams.nameColor.getValue() && clientName.startsWith("§") && clientName.length() > 1) {
            
            return targetName.startsWith("§" + clientName.charAt(1));
        }
        return teams.armorColor.getValue() && this.hasMatchingLeather(mc.thePlayer, entity);
    }

    private boolean isSameScoreboardTeam(EntityLivingBase player, EntityLivingBase entity) {
        Team selfTeam = player.getTeam();
        Team entityTeam = entity.getTeam();
        return selfTeam != null && entityTeam != null && selfTeam.isSameTeam(entityTeam);
    }

    private boolean isSameGommePrefix(String clientName, String targetName) {
        return clientName.startsWith("T") && targetName.startsWith("T")
                && clientName.length() > 1 && targetName.length() > 1
                && Character.isDigit(clientName.charAt(1)) && Character.isDigit(targetName.charAt(1))
                && clientName.charAt(1) == targetName.charAt(1);
    }

    private boolean hasMatchingLeather(EntityLivingBase player, EntityLivingBase entity) {
        for (int slot = 0; slot < 4; slot++) {
            ItemArmor playerArmor = getArmor(player, slot);
            ItemArmor entityArmor = getArmor(entity, slot);
            if (entityArmor == null || entityArmor.getArmorMaterial() != ItemArmor.ArmorMaterial.LEATHER) {
                continue;
            }
            if (playerArmor != null
                    && playerArmor.getColor(player.getCurrentArmor(slot))
                    == entityArmor.getColor(entity.getCurrentArmor(slot))) {
                return true;
            }
        }
        return false;
    }

    private ItemArmor getArmor(EntityLivingBase entity, int slot) {
        ItemStack stack = entity.getCurrentArmor(slot);
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) {
            return null;
        }
        return (ItemArmor) stack.getItem();
    }

    private static String stripReset(IChatComponent component) {
        return component == null ? "" : component.getFormattedText().replace("§r", "");
    }
}

