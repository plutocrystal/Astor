package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.enums.ChatColors;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.render.Render3DEvent;
import net.dev.astor.mixin.render.IAccessorRenderManager;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.misc.Target;
import net.dev.astor.util.ColorUtil;
import net.dev.astor.util.RenderUtil;
import net.dev.astor.util.TeamUtil;
import net.dev.astor.property.properties.*;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NameTags extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormatter = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    public final FloatProperty scale = new FloatProperty("Scale", 1.0F, 0.5F, 2.0F);
    public final BooleanProperty autoScale = new BooleanProperty("AutoScale", true);
    public final PercentProperty backgroundOpacity = new PercentProperty("Background", 25);
    public final BooleanProperty shadow = new BooleanProperty("Shadow", true);
    public final ModeProperty distanceMode = new ModeProperty("Distance", 0, new String[]{"None", "Default", "Vape"});
    public final ModeProperty healthMode = new ModeProperty("Health", 2, new String[]{"None", "Hp", "Hearts", "Tab"});
    public final BooleanProperty armor = new BooleanProperty("Armor", true);
    public final BooleanProperty effects = new BooleanProperty("Effects", true);
    public final BooleanProperty players = new BooleanProperty("Players", true);
    public final BooleanProperty enemies = new BooleanProperty("Enemies", true);
    public final BooleanProperty self = new BooleanProperty("Self", false);

    @Override
    public String getDescription() {
        return "Draws player names with health, armour and distance in several formats.";
    }

    public NameTags() {
        super("NameTags", Category.RENDER, false);
    }

    public boolean shouldRenderTags(EntityLivingBase entityLivingBase) {
        if (mc.getRenderViewEntity().getDistanceToEntity(entityLivingBase) > 512.0F) {
            return false;
        }
        if (entityLivingBase instanceof EntityPlayer) {
            if (entityLivingBase != mc.thePlayer && entityLivingBase != mc.getRenderViewEntity()) {
                return TeamUtil.isTarget((EntityPlayer) entityLivingBase) ? this.enemies.getValue() : this.players.getValue();
            }
            return this.self.getValue() && mc.gameSettings.thirdPersonView != 0;
        }
        
        return !entityLivingBase.isInvisible() && Target.get().isTargetable(entityLivingBase);
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled()) {
            for (Entity entity : TeamUtil.getLoadedEntitiesSorted()) {
                if (entity instanceof EntityLivingBase
                        && this.shouldRenderTags((EntityLivingBase) entity)
                        && (entity.ignoreFrustumCheck || RenderUtil.isInViewFrustum(entity.getEntityBoundingBox(), 10.0))) {
                    String teamName = TeamUtil.stripName(entity);
                    if (!StringUtils.isBlank(EnumChatFormatting.getTextWithoutFormattingCodes(teamName))) {
                        double x = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, event.getPartialTicks())
                                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
                        double y = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, event.getPartialTicks())
                                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()
                                + (double) entity.getEyeHeight();
                        double z = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, event.getPartialTicks())
                                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
                        double distance = mc.getRenderViewEntity().getDistanceToEntity(entity);
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(x, y + (entity.isSneaking() ? 0.225 : 0.4), z);
                        GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
                        float view = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
                        GlStateManager.rotate(mc.getRenderManager().playerViewX, view, 0.0F, 0.0F);
                        double scale = Math.pow(Math.min(Math.max(this.autoScale.getValue() ? distance : 0.0, 6.0), 128.0), 0.75) * 0.0075;
                        GlStateManager.scale(-scale * (double) this.scale.getValue(), -scale * (double) this.scale.getValue(), 1.0);
                        String distanceText = "";
                        switch (this.distanceMode.getValue()) {
                            case 1:
                                distanceText = String.format("&7%dm&r ", (int) distance);
                                break;
                            case 2:
                                distanceText = String.format("&a[&f%d&a]&r ", (int) distance);
                        }
                        float health = ((EntityLivingBase) entity).getHealth();
                        float absorption = ((EntityLivingBase) entity).getAbsorptionAmount();
                        float max = ((EntityLivingBase) entity).getMaxHealth();
                        float percent = Math.min(Math.max((health + absorption) / max, 0.0F), 1.0F);
                        String healText = "";
                        switch (this.healthMode.getValue()) {
                            case 1:
                                healText = String.format(" %d%s", (int) health, absorption > 0.0F ? String.format(" &6%d&r", (int) absorption) : "&r");
                                break;
                            case 2:
                                healText = String.format(
                                        " %s%s",
                                        healthFormatter.format((double) health / 2.0),
                                        absorption > 0.0F ? String.format(" &6%s&r", healthFormatter.format((double) absorption / 2.0)) : "&r"
                                );
                                break;
                            case 3:
                                if (entity instanceof EntityPlayer) {
                                    Scoreboard scoreboard = mc.theWorld.getScoreboard();
                                    if (scoreboard != null) {
                                        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);
                                        if (objective != null) {
                                            Score score = scoreboard.getValueFromObjective(entity.getName(), objective);
                                            if (score != null) {
                                                healText = String.format(" &e%d&r", score.getScorePoints());
                                            }
                                        }
                                    }
                                }
                        }
                        String color = ChatColors.formatColor(String.format("%s&f%s&r%s", distanceText, teamName, healText));
                        int width = mc.fontRendererObj.getStringWidth(color);
                        if (this.backgroundOpacity.getValue() > 0) {
                            Color textColor = !entity.isSneaking() && !entity.isInvisible()
                                    ? new Color(0.0F, 0.0F, 0.0F, (float) this.backgroundOpacity.getValue() / 100.0F)
                                    : new Color(0.33F, 0.0F, 0.33F, (float) this.backgroundOpacity.getValue() / 100.0F);
                            RenderUtil.enableRenderState();
                            RenderUtil.drawRect(
                                    (float) (-width) / 2.0F - 1.0F,
                                    (float) (-mc.fontRendererObj.FONT_HEIGHT) - 1.0F,
                                    (float) width / 2.0F + (this.shadow.getValue() ? 1.0F : 0.0F),
                                    this.shadow.getValue() ? 0.0F : -1.0F,
                                    textColor.getRGB()
                            );
                            RenderUtil.disableRenderState();
                        }
                        GlStateManager.disableDepth();
                        mc.fontRendererObj
                                .drawString(
                                        color,
                                        (float) (-width) / 2.0F,
                                        (float) (-mc.fontRendererObj.FONT_HEIGHT),
                                        ColorUtil.getHealthBlend(percent).getRGB(),
                                        this.shadow.getValue()
                                );
                        GlStateManager.enableDepth();
                        if (entity instanceof EntityPlayer) {
                            int height = mc.fontRendererObj.FONT_HEIGHT + 2;
                            if (this.armor.getValue()) {
                                ArrayList<ItemStack> renderingItems = new ArrayList<>();
                                for (int i = 4; i >= 0; i--) {
                                    ItemStack itemStack;
                                    if (i == 0) {
                                        itemStack = ((EntityPlayer) entity).getHeldItem();
                                    } else {
                                        itemStack = ((EntityPlayer) entity).inventory.armorInventory[i - 1];
                                    }
                                    if (itemStack != null) {
                                        renderingItems.add(itemStack);
                                    }
                                }
                                if (!renderingItems.isEmpty()) {
                                    int offset = renderingItems.size() * -8;
                                    for (int i = 0; i < renderingItems.size(); i++) {
                                        RenderUtil.renderItemInGUI(renderingItems.get(i), offset + i * 16, -height - 16);
                                    }
                                    height += 16;
                                }
                            }
                            if (this.effects.getValue()) {
                                List<PotionEffect> effects = ((EntityPlayer) entity)
                                        .getActivePotionEffects()
                                        .stream()
                                        .filter(potionEffect -> Potion.potionTypes[potionEffect.getPotionID()].hasStatusIcon())
                                        .collect(Collectors.toList());
                                if (!effects.isEmpty()) {
                                    GlStateManager.pushMatrix();
                                    GlStateManager.scale(0.5F, 0.5F, 1.0F);
                                    int offset = effects.size() * -9;
                                    for (int i = 0; i < effects.size(); i++) {
                                        RenderUtil.renderPotionEffect(effects.get(i), offset + i * 18, -(height * 2) - 18);
                                    }
                                    GlStateManager.popMatrix();
                                }
                            }
                            Color listColor = TeamUtil.getListColor(entity);
                            if (listColor != null) {
                                RenderUtil.enableRenderState();
                                float x1 = (float) (-width) / 2.0F - 1.0F;
                                view = (float) (-mc.fontRendererObj.FONT_HEIGHT) - 1.0F;
                                float y1 = (float) width / 2.0F + 1.0F;
                                float offset = this.shadow.getValue() ? 0.0F : -1.0F;
                                RenderUtil.drawOutlineRect(x1, view, y1, offset, 1.5F, 0, listColor.getRGB());
                                RenderUtil.disableRenderState();
                            }
                        }
                        GlStateManager.popMatrix();
                    }
                }
            }
        }
    }
}

