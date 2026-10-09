package net.dev.astor.module.impl.combat.killaura.attack;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.combat.killaura.target.AttackData;
import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.util.ItemUtil;
import net.dev.astor.util.KeyBindUtil;
import net.dev.astor.util.PacketUtil;
import net.dev.astor.util.PlayerUtil;
import net.dev.astor.util.RandomUtil;
import net.dev.astor.util.RotationUtil;
import net.dev.astor.mixin.attack.IAccessorPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * The attack itself, and the fake block that goes with it.
 *
 * <p>Kept apart from the module because it is the only part that sends packets: the rate limit, the
 * swing, the attack packet and the release all have to happen together, and the block state is
 * bookkeeping for the same sequence rather than for the target or the rotation.</p>
 */
public class Attacker {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final KillAura owner;

    private boolean smartPending = false;
    private long smartPendingSince = 0L;

    public Attacker(KillAura owner) {
        this.owner = owner;
    }

    private long getAttackDelay() {
        return 1000L / RandomUtil.nextLong(this.owner.minCPS.getValue(), this.owner.maxCPS.getValue());
    }

    /**
     * SmartAttack: hold the swing back so the hit lands where the server can register a crit.
     *
     * <p>Ported from Astra's Attack, whose gate is {@code smartAttack && player.hurtTime == 0} - the
     * player must be undamaged, so a hurt player still swings on the module's own rate rather than
     * the air timing below.</p>
     *
     * <p>Two separate waits, in Astra's order:</p>
     * <ul>
     *   <li>Target side: the target must be at hurtTime 0, or at the 10 that marks the tick the
     *       server last accepted a crit. Anything in between is a hit already in flight - the packet
     *       is on the wire but the target has not taken it yet, so a second one now would be a
     *       duplicate the server has already counted.</li>
     *   <li>Air side, only while airborne in a crit-eligible state: while rising the player cannot
     *       crit at all, so no point swinging; while falling, the swing is held for one ping so the
     *       client is not still rising when the server applies the crit. The ping is the estimate the
     *       client has of the server's clock, which is the only way to land the timing without reading
     *       the server's position directly.</li>
     * </ul>
     *
     * <p>Returns whether the attack may go out. Cleared on {@link #reset()} so re-enabling the
     * module does not carry a half-finished fall wait across.</p>
     */
    private boolean isSmartAttackBlocked(AttackData target) {
        if (!this.owner.smartAttack.getValue() || mc.thePlayer.hurtTime != 0) {
            this.smartPending = false;
            return false;
        }
        if (target.getEntity().hurtTime != 0 && target.getEntity().hurtTime != 10) {
            return true;
        }
        boolean airborneCritState = !mc.thePlayer.onGround
                && !mc.thePlayer.isOnLadder()
                && !mc.thePlayer.isInWater()
                && !mc.thePlayer.isPotionActive(Potion.blindness)
                && mc.thePlayer.ridingEntity == null;
        if (!airborneCritState) {
            this.smartPending = false;
            return false;
        }
        if (mc.thePlayer.motionY > 0.0D) {
            // Still rising: no swing can crit from here.
            this.smartPending = false;
            return true;
        }
        if (mc.thePlayer.fallDistance > 0.0F) {
            if (!this.smartPending) {
                this.smartPending = true;
                this.smartPendingSince = System.currentTimeMillis();
            }
            return System.currentTimeMillis() - this.smartPendingSince < this.getPing();
        }
        this.smartPending = false;
        return false;
    }

    /** The client's estimate of the server's clock, which is what the fall wait above is measured in. */
    private long getPing() {
        if (mc.getNetHandler() == null || mc.thePlayer == null) {
            return 0L;
        }
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (info == null) {
            return 0L;
        }
        return Math.max(0L, info.getResponseTime());
    }

    /**
     * Sends the attack if the rate limit has expired and the aim actually lands on the box. Returns
     * whether the swing was spent, so a pending fake block knows to use interact-attack instead of a
     * plain use-item.
     */
    public boolean performAttack(AttackData target, float yaw, float pitch) {
        if (Astor.playerStateManager.digging || Astor.playerStateManager.placing) {
            return false;
        } else if (this.owner.isPlayerBlocking() && this.owner.autoBlock.getValue() != 1) {
            return false;
        } else if (this.owner.getAttackDelayMS() > 0L) {
            return false;
        } else {
            this.owner.addAttackDelay(this.getAttackDelay());
            if (this.isSmartAttackBlocked(target)) {
                // The swing is not sent and the accumulator is not charged, so the next tick is free
                // to try again rather than having to wait out the rate limit for a swing that never
                // happened.
                return false;
            }
            mc.thePlayer.swingItem();
            if ((this.owner.rotations.getValue() != 0 || !this.owner.getTargetSelector().isBoxInAttackRange(target.getBox()))
                    && RotationUtil.rayTrace(target.getBox(), yaw, pitch, this.owner.attackRange.getValue()) == null) {
                return false;
            } else {
                this.owner.callAttackEvent(target.getEntity());
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                PacketUtil.sendPacket(new C02PacketUseEntity(target.getEntity(), Action.ATTACK));
                if (!this.owner.getTargetSelector().isInGameType()) {
                    PlayerUtil.attackEntity(target.getEntity());
                }
                this.owner.setHitRegistered(true);
                return true;
            }
        }
    }

    public void sendUseItem() {
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        this.startBlock(mc.thePlayer.getHeldItem());
    }

    public void startBlock(ItemStack itemStack) {
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(itemStack));
        mc.thePlayer.setItemInUse(itemStack, itemStack.getMaxItemUseDuration());
        this.owner.setBlockingState(true);
    }

    public void stopBlock() {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        this.owner.setBlockingState(false);
    }

    /**
     * Attack and block in one packet sequence. Used when the attack already happened this tick, so a
     * plain use-item would read as blocking without ever hitting.
     */
    public void interactAttack(AttackData target, float yaw, float pitch) {
        MovingObjectPosition mop = RotationUtil.rayTrace(target.getBox(), yaw, pitch, 8.0);
        if (mop == null) {
            return;
        }
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        PacketUtil.sendPacket(
                new C02PacketUseEntity(
                        target.getEntity(),
                        new Vec3(mop.hitVec.xCoord - target.getX(), mop.hitVec.yCoord - target.getY(), mop.hitVec.zCoord - target.getZ())
                )
        );
        PacketUtil.sendPacket(new C02PacketUseEntity(target.getEntity(), Action.INTERACT));
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
        mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
        this.owner.setBlockingState(true);
    }

    /**
     * Whether a fake block is possible at all. Only a sword can, because EnumAction.BLOCK is the only
     * action a sword has and the fake block is what puts the item into use.
     */
    public boolean canAutoBlock() {
        return ItemUtil.isHoldingSword();
    }

    /** Ticks the rate limit down. The module's own tick, but the counter lives with the attacker. */
    public void decayAttackDelay() {
        if (this.owner.getAttackDelayMS() > 0L) {
            this.owner.addAttackDelay(-50L);
        }
    }

    public boolean isAttackAllowed() {
        if (Astor.moduleManager.modules.get(net.dev.astor.module.impl.player.Scaffold.class).isEnabled()) {
            return false;
        } else if (!this.owner.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.owner.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            return !this.owner.requirePress.getValue() || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
        } else {
            return false;
        }
    }

    /** Keeps the fake block held across a tick boundary, since the server only sees one release. */
    public void holdBlock() {
        if (this.owner.isPlayerBlocking() && !mc.thePlayer.isBlocking()) {
            mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
        }
    }

    public void onHeldItemChanged() {
        this.owner.setBlockingState(false);
        if (this.owner.isBlocking()) {
            mc.thePlayer.stopUsingItem();
        }
    }

    public void onUseItemReleased() {
        this.owner.setBlockingState(false);
    }

    public void reset() {
        this.smartPending = false;
        this.smartPendingSince = 0L;
    }
}
