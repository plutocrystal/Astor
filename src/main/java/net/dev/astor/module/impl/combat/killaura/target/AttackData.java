package net.dev.astor.module.impl.combat.killaura.target;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;

/**
 * A snapshot of one target: the entity plus the box and position it had when it was picked.
 *
 * <p>The position is copied rather than read live because the dot has to keep aiming at where the
 * target was when the rotation was computed, not at where it has since moved to - the rotation is
 * only recomputed once a tick while the entity moves every tick.</p>
 */
public class AttackData {
    private final EntityLivingBase entity;
    private final AxisAlignedBB box;
    private final double x;
    private final double y;
    private final double z;

    public AttackData(EntityLivingBase entityLivingBase) {
        this.entity = entityLivingBase;
        double collisionBorderSize = entityLivingBase.getCollisionBorderSize();
        this.box = entityLivingBase.getEntityBoundingBox().expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
        this.x = entityLivingBase.posX;
        this.y = entityLivingBase.posY;
        this.z = entityLivingBase.posZ;
    }

    public EntityLivingBase getEntity() {
        return this.entity;
    }

    public AxisAlignedBB getBox() {
        return this.box;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }
}
