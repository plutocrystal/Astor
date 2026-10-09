package net.dev.astor.module.impl.combat;

import net.dev.astor.module.Category;
import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.types.Priority;
import net.dev.astor.event.events.impl.input.LeftClickMouseEvent;
import net.dev.astor.event.events.impl.render.Render3DEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.mixin.render.IAccessorRenderManager;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.misc.Target;
import net.dev.astor.util.RenderUtil;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.ColorProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class HitBox extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private MovingObjectPosition targetEntity = null;
    public final FloatProperty multiplier = new FloatProperty("Multiplier", 1.2F, 1.0F, 5.0F);
    public final BooleanProperty showHitbox = new BooleanProperty("ShowHitbox", false);

    public final ColorProperty color = new ColorProperty("Color", 0x96FFFFFF, this.showHitbox::getValue);

    @Override
    public String getDescription() {
        return "Widens the hitbox used for picking and raytracing, so you can select and reach entities more easily.";
    }

    public HitBox() {
        super("HitBox", Category.COMBAT, false);
    }

    public static float getExpansion(Entity entity) {
        HitBox hitBox = (HitBox) Astor.moduleManager.modules.get(HitBox.class);
        if (hitBox != null && hitBox.isEnabled() && entity instanceof EntityLivingBase) {
            return hitBox.multiplier.getValue();
        }
        return 1.0F;
    }

    private void calculateMouseOver(float partialTicks) {
        if (mc.getRenderViewEntity() != null && mc.theWorld != null) {
            mc.pointedEntity = null;
            Entity pointedEntity = null;
            double reach = 3.0;
            this.targetEntity = mc.getRenderViewEntity().rayTrace(reach, partialTicks);
            double distance = reach;
            Vec3 eyePos = mc.getRenderViewEntity().getPositionEyes(partialTicks);
            if (this.targetEntity != null) {
                distance = this.targetEntity.hitVec.distanceTo(eyePos);
            }
            Vec3 lookVec = mc.getRenderViewEntity().getLook(partialTicks);
            Vec3 reachVec = eyePos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);
            Vec3 hitVec = null;
            float expansion = 1.0F;
            List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
                    mc.getRenderViewEntity(),
                    mc.getRenderViewEntity()
                            .getEntityBoundingBox()
                            .addCoord(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach)
                            .expand(expansion, expansion, expansion)
            );
            double closestDistance = distance;
            for (Entity entity : entities) {
                // Never point at a friend or a teammate, otherwise the enlarged box makes the
                // client hit someone it is supposed to leave alone.
                if (entity instanceof EntityLivingBase && !Target.get().isValidTarget((EntityLivingBase) entity)) {
                    continue;
                }
                if (entity.canBeCollidedWith()) {
                    float collisionSize = (float) ((double) entity.getCollisionBorderSize() * getExpansion(entity));
                    AxisAlignedBB expandedBox = entity.getEntityBoundingBox().expand(collisionSize, collisionSize, collisionSize);
                    MovingObjectPosition intercept = expandedBox.calculateIntercept(eyePos, reachVec);
                    if (expandedBox.isVecInside(eyePos)) {
                        if (0.0 < closestDistance || closestDistance == 0.0) {
                            pointedEntity = entity;
                            hitVec = intercept == null ? eyePos : intercept.hitVec;
                            closestDistance = 0.0;
                        }
                    } else if (intercept != null) {
                        double interceptDistance = eyePos.distanceTo(intercept.hitVec);
                        if (interceptDistance < closestDistance || closestDistance == 0.0) {
                            if (entity == mc.getRenderViewEntity().ridingEntity && !entity.canRiderInteract()) {
                                if (closestDistance == 0.0) {
                                    pointedEntity = entity;
                                    hitVec = intercept.hitVec;
                                }
                            } else {
                                pointedEntity = entity;
                                hitVec = intercept.hitVec;
                                closestDistance = interceptDistance;
                            }
                        }
                    }
                }
            }
            if (pointedEntity != null && (closestDistance < distance || this.targetEntity == null)) {
                this.targetEntity = new MovingObjectPosition(pointedEntity, hitVec);
                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame) {
                    mc.pointedEntity = pointedEntity;
                }
            }
        }
    }

    private boolean shouldShowEntity(EntityLivingBase entity) {
        if (entity == mc.thePlayer || entity instanceof EntityArmorStand || entity.isInvisible()) {
            return false;
        }
        if (mc.getRenderViewEntity().getDistanceToEntity(entity) > 128.0F) {
            return false;
        }
        if (!entity.ignoreFrustumCheck && !RenderUtil.isInViewFrustum(entity.getEntityBoundingBox(), 0.1F)) {
            return false;
        }
        // Which kinds are worth drawing is the Target module's call, so the boxes on screen always
        // match the entities the combat modules are allowed to act on.
        return Target.get().isTargetable(entity);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.calculateMouseOver(1.0F);
        }
    }

    @EventTarget(Priority.HIGH)
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isEnabled() && !event.isCancelled() && this.targetEntity != null) {
            mc.objectMouseOver = this.targetEntity;
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled() && this.showHitbox.getValue()) {
            List<EntityLivingBase> entities = mc.theWorld.loadedEntityList
                    .stream()
                    .filter(entity -> entity instanceof EntityLivingBase)
                    .map(entity -> (EntityLivingBase) entity)
                    .filter(this::shouldShowEntity)
                    .collect(Collectors.toList());
            if (!entities.isEmpty()) {
                RenderUtil.enableRenderState();
                Color renderColor = new Color(this.color.getValue(), true);
                for (EntityLivingBase entity : entities) {
                    float collisionSize = (float) ((double) entity.getCollisionBorderSize() * this.multiplier.getValue());
                    AxisAlignedBB expandedBox = entity.getEntityBoundingBox().expand(collisionSize, collisionSize, collisionSize);
                    AxisAlignedBB offsetBox = new AxisAlignedBB(
                            expandedBox.minX - entity.posX + (RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX()),
                            expandedBox.minY - entity.posY + (RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()),
                            expandedBox.minZ - entity.posZ + (RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()),
                            expandedBox.maxX - entity.posX + (RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX()),
                            expandedBox.maxY - entity.posY + (RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()),
                            expandedBox.maxZ - entity.posZ + (RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, event.getPartialTicks()) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ())
                    );
                    RenderUtil.drawBoundingBox(offsetBox, renderColor.getRed(), renderColor.getGreen(), renderColor.getBlue(), renderColor.getAlpha(), 1.5F);
                }
                RenderUtil.disableRenderState();
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%.1fx", this.multiplier.getValue())};
    }
}
