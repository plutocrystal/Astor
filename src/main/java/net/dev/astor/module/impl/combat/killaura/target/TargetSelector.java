package net.dev.astor.module.impl.combat.killaura.target;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.module.impl.misc.BedNuker;
import net.dev.astor.module.impl.misc.Target;
import net.dev.astor.module.impl.player.AutoBlockIn;
import net.dev.astor.module.impl.player.AutoHeal;
import net.dev.astor.module.impl.player.Scaffold;
import net.dev.astor.mixin.attack.IAccessorPlayerControllerMP;
import net.dev.astor.util.ItemUtil;
import net.dev.astor.util.PlayerUtil;
import net.dev.astor.util.RotationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.dev.astor.util.TeamUtil;
import net.minecraft.world.WorldSettings.GameType;

import java.util.ArrayList;

/**
 * Finding and keeping a target: which entities count, how far away they may be, and which one of
 * them is picked. The four range tests stay separate because they measure different things - how far
 * you can hit, how far you swing early, how far you can block, and how far the aim reaches.
 */
public class TargetSelector {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final KillAura owner;

    public TargetSelector(KillAura owner) {
        this.owner = owner;
    }

    /** Whether an attack is allowed at all right now, ignoring whether a target exists. */
    public boolean canAttack() {
        if (this.owner.inventoryCheck.getValue() && mc.currentScreen instanceof GuiContainer) {
            return false;
        } else if (!(Boolean) this.owner.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.owner.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) {
                return false;
            } else if ((ItemUtil.isEating() || ItemUtil.isUsingBow()) && PlayerUtil.isUsingItem()) {
                return false;
            } else {
                AutoHeal autoHeal = (AutoHeal) Astor.moduleManager.modules.get(AutoHeal.class);
                if (autoHeal.isEnabled() && autoHeal.isSwitching()) {
                    return false;
                } else {
                    BedNuker bedNuker = (BedNuker) Astor.moduleManager.modules.get(BedNuker.class);
                    AutoBlockIn autoBlockIn = (AutoBlockIn) Astor.moduleManager.modules.get(AutoBlockIn.class);
                    if (bedNuker.isEnabled() && bedNuker.isReady()) {
                        return false;
                    } else if (Astor.moduleManager.modules.get(Scaffold.class).isEnabled()) {
                        return false;
                    } else if (autoBlockIn.isEnabled()) {
                        return false;
                    } else if (this.owner.requirePress.getValue()) {
                        return PlayerUtil.isAttacking();
                    } else {
                        return !this.owner.allowMining.getValue() || !mc.objectMouseOver.typeOfHit.equals(MovingObjectType.BLOCK) || !PlayerUtil.isAttacking();
                    }
                }
            }
        } else {
            return false;
        }
    }

    public boolean hasValidTarget() {
        return mc.theWorld
                .loadedEntityList
                .stream()
                .anyMatch(
                        entity -> entity instanceof EntityLivingBase
                                && this.isValidTarget((EntityLivingBase) entity)
                                && this.isInBlockRange((EntityLivingBase) entity)
                );
    }

    public boolean isValidTarget(EntityLivingBase entityLivingBase) {
        if (!Target.get().isValidTarget(entityLivingBase)) {
            return false;
        }
        // Range, fov and line of sight stay with this module, Target does not decide them.
        return RotationUtil.angleToEntity(entityLivingBase) <= this.owner.fov.getValue().floatValue()
                && (this.owner.throughWalls.getValue() || RotationUtil.rayTrace(entityLivingBase) == null);
    }

    public boolean isInRange(EntityLivingBase entityLivingBase) {
        return this.isInBlockRange(entityLivingBase) || this.isInSwingRange(entityLivingBase) || this.isInAttackRange(entityLivingBase);
    }

    /**
     * How far away KillAura is willing to aim at a target. This is an independent setting: it is not
     * linked to AttackRange / SwingRange / AutoBlockRange, because each of those measures something
     * different - how far you can hit, how far you swing early, and how far you can block.
     */
    public boolean isInAimRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.owner.aimRange.getValue();
    }

    public boolean isInBlockRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.owner.autoBlockRange.getValue();
    }

    public boolean isInSwingRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.owner.swingRange.getValue();
    }

    public boolean isBoxInSwingRange(AxisAlignedBB axisAlignedBB) {
        return RotationUtil.distanceToBox(axisAlignedBB) <= (double) this.owner.swingRange.getValue();
    }

    public boolean isInAttackRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.owner.attackRange.getValue();
    }

    public boolean isBoxInAttackRange(AxisAlignedBB axisAlignedBB) {
        return RotationUtil.distanceToBox(axisAlignedBB) <= (double) this.owner.attackRange.getValue();
    }

    public boolean isPlayerTarget(EntityLivingBase entityLivingBase) {
        return entityLivingBase instanceof EntityPlayer && TeamUtil.isTarget((EntityPlayer) entityLivingBase);
    }

    private void sortTargets(ArrayList<EntityLivingBase> targets) {
        targets.sort(
                (entityLivingBase1, entityLivingBase2) -> {
                    int sortBase = 0;
                    switch (this.owner.sort.getValue()) {
                        case 1:
                            sortBase = Float.compare(TeamUtil.getHealthScore(entityLivingBase1), TeamUtil.getHealthScore(entityLivingBase2));
                            break;
                        case 2:
                            sortBase = Integer.compare(entityLivingBase1.hurtResistantTime, entityLivingBase2.hurtResistantTime);
                            break;
                        case 3:
                            sortBase = Float.compare(
                                    RotationUtil.angleToEntity(entityLivingBase1),
                                    RotationUtil.angleToEntity(entityLivingBase2)
                            );
                    }
                    return sortBase != 0
                            ? sortBase
                            : Double.compare(RotationUtil.distanceToEntity(entityLivingBase1), RotationUtil.distanceToEntity(entityLivingBase2));
                }
        );
    }

    /**
     * Which index into the sorted list to take. Switch mode advances on every registered hit and
     * wraps at the end of the list, so it only ever cycles through targets that all passed the
     * filters above.
     */
    public int pickTarget(ArrayList<EntityLivingBase> targets, boolean hitRegistered) {
        if (this.owner.mode.getValue() == 1 && hitRegistered) {
            this.owner.consumeHit();
            this.owner.advanceSwitchTick();
        }
        if (this.owner.mode.getValue() == 0 || this.owner.getSwitchTick() >= targets.size()) {
            this.owner.resetSwitchTick();
        }
        return this.owner.getSwitchTick();
    }

    /**
     * Every entity that could be attacked right now, narrowed to the tightest range that still has
     * a candidate, then sorted. The narrowing is what makes a distant entity not win just by being
     * the only one left.
     */
    public ArrayList<EntityLivingBase> collectTargets() {
        ArrayList<EntityLivingBase> targets = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase
                    && this.isValidTarget((EntityLivingBase) entity)
                    && this.isInAimRange((EntityLivingBase) entity)
                    && this.isInRange((EntityLivingBase) entity)) {
                targets.add((EntityLivingBase) entity);
            }
        }
        if (targets.isEmpty()) {
            return targets;
        }
        if (targets.stream().anyMatch(this::isInSwingRange)) {
            targets.removeIf(entityLivingBase -> !this.isInSwingRange(entityLivingBase));
        }
        if (targets.stream().anyMatch(this::isInAttackRange)) {
            targets.removeIf(entityLivingBase -> !this.isInAttackRange(entityLivingBase));
        }
        if (this.owner.preferEnemies.getValue() && targets.stream().anyMatch(this::isPlayerTarget)) {
            targets.removeIf(entityLivingBase -> !this.isPlayerTarget(entityLivingBase));
        }
        this.sortTargets(targets);
        return targets;
    }

    /** Whether the current target has to be dropped and looked for again. */
    public boolean needsRetarget(AttackData current) {
        return current == null
                || !this.isValidTarget(current.getEntity())
                || !this.isInAimRange(current.getEntity())
                || !this.isBoxInAttackRange(current.getBox())
                || !this.isBoxInSwingRange(current.getBox());
    }

    public boolean isInGameType() {
        return mc.playerController.getCurrentGameType() == GameType.SPECTATOR;
    }
}
