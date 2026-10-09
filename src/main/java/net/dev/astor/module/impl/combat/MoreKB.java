package net.dev.astor.module.impl.combat;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.attack.AttackEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.event.types.EventType;
import net.dev.astor.module.Module;
import net.dev.astor.mixin.input.IAccessorKeyBinding;
import net.dev.astor.module.impl.misc.Target;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;

public class MoreKB extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int ADVANCED = 5;
    private static final int LEGIT_FAST = 1;

    private static final int ADVANCED_MAX_HURT_TIME = 1;

    private static final int ADVANCED_RESTORE_AGE = 2;

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Legit", "LegitFast", "LessPacket", "Packet", "DoublePacket", "Advanced"});
    public final BooleanProperty intelligent = new BooleanProperty("Intelligent", false);
    public final BooleanProperty onlyGround = new BooleanProperty("OnlyGround", true);
    private boolean shouldSprintReset;
    private EntityLivingBase target;

    private boolean prevMoveForward = false;
    private boolean pendingRestore = false;
    private int pendingAge = 0;

    @Override
    public String getDescription() {
        return "Knockback variants, from a small extra push through to silent packet-level boosts.";
    }

    public MoreKB() {
        super("MoreKB", Category.COMBAT, false);
        this.shouldSprintReset = false;
        this.target = null;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Entity targetEntity = event.getTarget();
        if (!(targetEntity instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase living = (EntityLivingBase) targetEntity;
        
        this.target = Target.get().isFriendOrTeammate(living) ? null : living;
        this.onAdvancedAttack();
    }

    private void onAdvancedAttack() {
        if (this.mode.getValue() != ADVANCED
                || mc.thePlayer == null
                || !mc.thePlayer.isSprinting()
                || !mc.thePlayer.onGround
                || this.target == null
                || this.target.hurtTime > ADVANCED_MAX_HURT_TIME) {
            return;
        }
        this.prevMoveForward = mc.gameSettings.keyBindForward.isKeyDown();
        ((IAccessorKeyBinding) mc.gameSettings.keyBindForward).setPressed(false);
        this.pendingRestore = true;
        this.pendingAge = 0;
    }

    private void restoreForwardKey() {
        if (mc.thePlayer == null) {
            return;
        }
        if (this.prevMoveForward && Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            ((IAccessorKeyBinding) mc.gameSettings.keyBindForward).setPressed(true);
        }
        this.pendingRestore = false;
        this.pendingAge = 0;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getType() == EventType.POST) {
            if (this.pendingRestore) {
                this.restoreForwardKey();
            }
            return;
        }
        if (this.mode.getValue() == ADVANCED) {
            
            if (this.pendingRestore && ++this.pendingAge >= ADVANCED_RESTORE_AGE) {
                this.restoreForwardKey();
            }
            return;
        }
        if (this.mode.getValue() == LEGIT_FAST) {
            if (this.target != null && this.isMoving()) {
                if ((this.onlyGround.getValue() && mc.thePlayer.onGround) || !this.onlyGround.getValue()) {
                    mc.thePlayer.sprintingTicksLeft = 0;
                }
                this.target = null;
            }
            return;
        }
        EntityLivingBase entity = null;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            entity = (EntityLivingBase) mc.objectMouseOver.entityHit;
        }
        if (entity == null || Target.get().isFriendOrTeammate(entity)) {
            return;
        }
        double x = mc.thePlayer.posX - entity.posX;
        double z = mc.thePlayer.posZ - entity.posZ;
        float calcYaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI - 90.0);
        float diffY = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - entity.rotationYawHead));
        if (this.intelligent.getValue() && diffY > 120.0F) {
            return;
        }
        if (entity.hurtTime == 10) {
            switch (this.mode.getValue()) {
                case 0:
                    this.shouldSprintReset = true;
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(false);
                        mc.thePlayer.setSprinting(true);
                    }
                    this.shouldSprintReset = false;
                    break;
                case 2:
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(false);
                    }
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
                case 3:
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
                case 4:
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
            }
        }
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
    }

    @Override
    public void onEnabled() {
        this.prevMoveForward = false;
        this.pendingRestore = false;
        this.pendingAge = 0;
    }

    @Override
    public void onDisabled() {
        
        this.pendingRestore = false;
        this.pendingAge = 0;
        this.prevMoveForward = false;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getValue().toString()};
    }
}

