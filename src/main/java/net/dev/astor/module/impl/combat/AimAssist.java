package net.dev.astor.module.impl.combat;

import net.dev.astor.module.Category;
import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.input.KeyEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.module.Module;
import net.dev.astor.util.*;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.PercentProperty;
import net.dev.astor.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    public final FloatProperty hSpeed = new FloatProperty("HorizontalSpeed", 3.0F, 0.0F, 10.0F);
    public final FloatProperty vSpeed = new FloatProperty("VerticalSpeed", 0.0F, 0.0F, 10.0F);
    public final PercentProperty smoothing = new PercentProperty("Smoothing", 50);
    public final FloatProperty range = new FloatProperty("Range", 4.5F, 3.0F, 8.0F);
    public final IntProperty fov = new IntProperty("Fov", 90, 30, 360);
    public final BooleanProperty weaponOnly = new BooleanProperty("WeaponsOnly", true);
    public final BooleanProperty allowTools = new BooleanProperty("AllowTools", false, this.weaponOnly::getValue);
    public final BooleanProperty botChecks = new BooleanProperty("BotCheck", true);
    public final BooleanProperty team = new BooleanProperty("Teams", true);

    private boolean isValidTarget(EntityPlayer entityPlayer) {
        if (entityPlayer != mc.thePlayer && entityPlayer != mc.thePlayer.ridingEntity) {
            if (entityPlayer == mc.getRenderViewEntity() || entityPlayer == mc.getRenderViewEntity().ridingEntity) {
                return false;
            } else if (entityPlayer.deathTime > 0) {
                return false;
            } else if (RotationUtil.distanceToEntity(entityPlayer) > (double) this.range.getValue()) {
                return false;
            } else if (RotationUtil.angleToEntity(entityPlayer) > (float) this.fov.getValue()) {
                return false;
            } else if (RotationUtil.rayTrace(entityPlayer) != null) {
                return false;
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return false;
            } else {
                return (!this.team.getValue() || !TeamUtil.isSameTeam(entityPlayer)) && (!this.botChecks.getValue() || !TeamUtil.isBot(entityPlayer));
            }
        } else {
            return false;
        }
    }

    private boolean isInReach(EntityPlayer entityPlayer) {
        Reach reach = (Reach) Astor.moduleManager.modules.get(Reach.class);
        double distance = reach.isEnabled() ? (double) reach.range.getValue() : 3.0;
        return RotationUtil.distanceToEntity(entityPlayer) <= distance;
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    public AimAssist() {
        super("AimAssist", Category.COMBAT, false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST && mc.currentScreen == null) {
            if (!(Boolean) this.weaponOnly.getValue()
                    || ItemUtil.hasRawUnbreakingEnchant()
                    || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
                boolean attacking = PlayerUtil.isAttacking();
                if (!attacking || !this.isLookingAtBlock()) {
                    if (attacking || !this.timer.hasTimeElapsed(350L)) {
                        List<EntityPlayer> inRange = mc.theWorld
                                .loadedEntityList
                                .stream()
                                .filter(entity -> entity instanceof EntityPlayer)
                                .map(entity -> (EntityPlayer) entity)
                                .filter(this::isValidTarget)
                                .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                                .collect(Collectors.toList());
                        if (!inRange.isEmpty()) {
                            if (inRange.stream().anyMatch(this::isInReach)) {
                                inRange.removeIf(entityPlayer -> !this.isInReach(entityPlayer));
                            }
                            EntityPlayer player = inRange.get(0);
                            if (!(RotationUtil.distanceToEntity(player) <= 0.0)) {
                                AxisAlignedBB axisAlignedBB = player.getEntityBoundingBox();
                                double collisionBorderSize = player.getCollisionBorderSize();
                                float[] rotation = RotationUtil.getRotationsToBox(
                                        axisAlignedBB.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize),
                                        mc.thePlayer.rotationYaw,
                                        mc.thePlayer.rotationPitch,
                                        180.0F,
                                        (float) this.smoothing.getValue() / 100.0F
                                );
                                float yaw = Math.min(Math.abs(this.hSpeed.getValue()), 10.0F);
                                float pitch = Math.min(Math.abs(this.vSpeed.getValue()), 10.0F);
                                Astor.rotationManager
                                        .setRotation(
                                                mc.thePlayer.rotationYaw + (rotation[0] - mc.thePlayer.rotationYaw) * 0.1F * yaw,
                                                mc.thePlayer.rotationPitch + (rotation[1] - mc.thePlayer.rotationPitch) * 0.1F * pitch,
                                                0,
                                                false
                                        );
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPress(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindAttack.getKeyCode() && !Astor.moduleManager.modules.get(AutoClicker.class).isEnabled()) {
            this.timer.reset();
        }
    }
}