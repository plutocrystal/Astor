package net.dev.astor.module.impl.combat;

import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.Astor;
import net.dev.astor.enums.ChatColors;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.event.events.impl.player.LoadWorldEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.event.events.impl.render.GameLoopEvent;
import net.dev.astor.event.events.impl.render.Render3DEvent;
import net.dev.astor.event.types.EventType;
import net.dev.astor.mixin.client.IAccessorMinecraft;
import net.dev.astor.mixin.movement.IAccessorEntity;
import net.dev.astor.mixin.render.IAccessorRenderManager;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.misc.Target;
import net.dev.astor.module.impl.player.Scaffold;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.util.ChatUtil;
import net.dev.astor.util.PacketUtil;
import net.dev.astor.util.RenderUtil;
import net.dev.astor.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Ported from Astra's TimerRange, which is itself the Java form of the RN_Random_Name "TimerRangeReborn"
 * script. Slows the client clock while holding outgoing movement packets back for exactly as long as
 * the target is predicted to need, so the server sees the hit land sooner than the client view suggests.
 *
 * <p>The state machine runs on {@link GameLoopEvent} rather than a tick, and that is load-bearing.
 * MinTimer defaults to 0, which pins elapsedTicks at zero and stops runTick() entirely - taking every
 * TickEvent with it. A tick-driven machine could never set the clock back and would wedge the client
 * for good; running here means it can always recover.</p>
 */
public class TimerRange extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty workMode = new ModeProperty("WorkMode", 0, new String[]{"PRE", "POST"});
    public final BooleanProperty outGoing = new BooleanProperty("OutGoing", true);
    public final IntProperty maxTick = new IntProperty("MaxTick", 10, 1, 30);
    public final FloatProperty maxFinalDistance = new FloatProperty("MaxFinalDistance", 3.5F, 0.0F, 8.0F);
    public final FloatProperty maxTimer = new FloatProperty("MaxTimer", 7.0F, 1.0F, 10.0F);
    public final FloatProperty minTimer = new FloatProperty("MinTimer", 0.0F, 0.0F, 1.0F);
    public final FloatProperty slowTimerFactor = new FloatProperty("SlowTimerFactor", 1.5F, 0.0F, 3.0F);
    public final FloatProperty fastTimerFactor = new FloatProperty("FastTimerFactor", 0.0F, 0.0F, 3.0F);
    public final IntProperty delay = new IntProperty("Delay", 1600, 400, 5000);
    public final FloatProperty minBps = new FloatProperty("MinBps", 0.08F, 0.01F, 0.16F);
    public final BooleanProperty debug = new BooleanProperty("Debug", true);
    public final BooleanProperty renderPoint = new BooleanProperty("RenderPoint", true);
    public final BooleanProperty renderOnlyWhenNeed = new BooleanProperty("RenderOnlyWhenNeed", true);

    private double timerBalance = 0.0;
    private double smartMaxBalance = 0.0;
    private boolean getHurt = false;
    private final TimerUtil hurtTimer = new TimerUtil();
    private final TimerUtil delayTimer = new TimerUtil();
    private boolean work = false;
    private boolean stopWorking = false;
    private boolean timerReset = false;
    private final TimerUtil attackTimer = new TimerUtil();
    private boolean attack = false;
    private double lastRenderX = 0.0;
    private double lastRenderY = 0.0;
    private double lastRenderZ = 0.0;
    private Vec3 predictedPosition = null;
    private final Deque<Packet<?>> blinkPackets = new ArrayDeque<>();
    private final TimerUtil tickTimer = new TimerUtil();

    @Override
    public String getDescription() {
        return "Slows the client clock while holding movement packets back, so the server sees your hits land sooner than your own view suggests.";
    }

    public TimerRange() {
        super("TimerRange", Category.COMBAT, false);
    }

    @Override
    public void onEnabled() {
        this.delayTimer.reset();
        this.hurtTimer.reset();
        this.tickTimer.reset();
    }

    @Override
    public void onDisabled() {
        this.setTimerSpeed(1.0F);
        this.timerReset = false;
        this.timerBalance = 0.0;
        this.smartMaxBalance = 0.0;
        this.blinkPackets.clear();
        this.work = false;
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.timerBalance = 0.0;
        this.smartMaxBalance = 0.0;
        this.setTimerSpeed(1.0F);
        this.tickTimer.reset();
        this.blinkPackets.clear();
        this.predictedPosition = null;
    }

    // ------------------------------------------------------------------ tick: prediction and decay

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (mc.theWorld == null) {
            return;
        }
        if (mc.thePlayer != null) {
            SimulatedPlayer simulatedPlayer = new SimulatedPlayer(mc.thePlayer);
            this.predictedPosition = this.updatePlayer(simulatedPlayer, this.maxTick.getValue().intValue());
        }
        if (this.getTimerSpeed() == this.maxTimer.getValue().floatValue()) {
            this.timerBalance -= this.maxTimer.getValue() + this.fastTimerFactor.getValue();
        }
    }

    // ------------------------------------------------------------------ game loop: the state machine

    @EventTarget
    public void onGameLoop(GameLoopEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null || this.predictedPosition == null) {
            return;
        }
        float ts = this.getTimerSpeed();
        EntityLivingBase entity = this.getClosestEntity(256.0);
        if (entity == null) {
            event.setTimerSpeed(ts);
            return;
        }
        if (mc.thePlayer.isDead) {
            this.timerBalance = 0.0;
            ts = 1.0F;
            event.setTimerSpeed(ts);
            return;
        }

        double playerBPS = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
        double distance = this.calculateDistance(
                new Vec3(
                        this.predictedPosition.xCoord,
                        this.predictedPosition.yCoord + mc.thePlayer.getEyeHeight(),
                        this.predictedPosition.zCoord
                ),
                new Vec3(entity.serverPosX / 32.0, entity.serverPosY / 32.0, entity.serverPosZ / 32.0)
        ) - entity.getCollisionBorderSize() * 3.5 + this.distanceAdjust(entity);

        if (this.delayTimer.hasTimeElapsed(this.delay.getValue().longValue())
                && !this.stopWorking
                && this.isCrosshairOnEntity(entity) != null) {
            playerBPS = this.setSmartBalance(entity, mc.thePlayer, playerBPS, distance);
            if (this.smartMaxBalance <= this.maxTick.getValue() && this.smartMaxBalance > 0.0 && this.timerBalance == 0.0) {
                if (this.debug.getValue()) {
                    ChatUtil.sendFormatted(
                            String.format("%s[§6Timed§3] Tick:%s", ChatColors.RED, Math.round(this.smartMaxBalance * 1000.0) / 1000.0)
                    );
                }
                this.work = true;
                this.tickTimer.reset();
                this.delayTimer.reset();
            }
        }

        if (this.stopWorking && !this.work && this.timerBalance < 0.0 && this.workMode.getValue() == 0) {
            ts = this.resetTimer(ts);
        }

        if (this.workMode.getValue() == 0) {
            if (this.work) {
                if (this.timerBalance > this.smartMaxBalance && ts == this.minTimer.getValue().floatValue()) {
                    ts = this.maxTimer.getValue().floatValue();
                    this.work = false;
                } else {
                    this.timerReset = true;
                    ts = this.minTimer.getValue().floatValue();
                }
            } else if (this.timerBalance < 0.0) {
                ts = this.resetTimer(ts);
            }
        } else {
            if (this.work) {
                ts = this.maxTimer.getValue().floatValue();
                if (Math.abs(this.timerBalance) > this.smartMaxBalance) {
                    this.timerReset = true;
                    this.work = false;
                }
            } else {
                if (this.timerBalance < 0.0) {
                    ts = this.minTimer.getValue().floatValue();
                } else {
                    ts = this.resetTimer(ts);
                }
            }
        }

        if (ts == this.minTimer.getValue().floatValue() && this.tickTimer.hasTimeElapsed(40L)) {
            if ((this.workMode.getValue() == 0 && this.work) || (this.workMode.getValue() == 1 && !this.work)) {
                this.timerBalance += this.slowTimerFactor.getValue() - this.minTimer.getValue();
            }
            this.tickTimer.reset();
        }

        this.stopWorking = mc.thePlayer.isOnLadder()
                || !this.moveCheck()
                || !this.veloCheck()
                || !this.fluidCheck(mc.thePlayer)
                || !this.rayTraceCheck(mc.thePlayer)
                || !this.inRange(distance)
                || !this.otherModuleCheck()
                || !this.attackCheck()
                || playerBPS < this.minBps.getValue();

        event.setTimerSpeed(ts);
    }

    private float resetTimer(float ts) {
        this.timerBalance = 0.0;
        if (!this.timerReset) {
            return ts;
        }
        ts = 1.0F;
        this.timerReset = false;
        return ts;
    }

    // ------------------------------------------------------------------ checks

    private double setSmartBalance(Entity entity, net.minecraft.entity.player.EntityPlayer player, double playerBPS, double distance) {
        double entityMotionX = Math.abs(entity.lastTickPosX - entity.posX);
        double entityMotionZ = Math.abs(entity.lastTickPosZ - entity.posZ);
        double entityBPS = Math.sqrt(entityMotionX * entityMotionX + entityMotionZ * entityMotionZ);
        entityBPS = Math.max(0.12, entityBPS);
        playerBPS = Math.max(0.12, playerBPS);
        double dis2 = player.getDistanceToEntity(entity) - entity.getCollisionBorderSize() * 3.5;
        double finalDistance = dis2 - distance + 0.45;
        double a = player.onGround ? 1.0 : 0.6;
        this.smartMaxBalance = finalDistance / (playerBPS * a + entityBPS / 3.0);
        return playerBPS;
    }

    private double distanceAdjust(Entity entity) {
        double toLast = mc.thePlayer.getDistance(entity.lastTickPosX, entity.lastTickPosY, entity.lastTickPosZ);
        double toNow = mc.thePlayer.getDistance(entity.posX, entity.posY, entity.posZ);
        if (toLast < toNow - 0.05) {
            return -0.5;
        }
        if (toLast > toNow + 0.1) {
            return 0.3;
        }
        return 0.0;
    }

    private boolean inRange(double distance) {
        return distance <= this.maxFinalDistance.getValue();
    }

    private boolean veloCheck() {
        if (this.getHurt) {
            if (this.hurtTimer.hasTimeElapsed(399L)) {
                this.getHurt = false;
                this.hurtTimer.reset();
            }
            return false;
        }
        return true;
    }

    private boolean fluidCheck(net.minecraft.entity.player.EntityPlayer player) {
        return !player.isInWater() && !player.isInLava() && !((IAccessorEntity) player).getIsInWeb();
    }

    private boolean attackCheck() {
        return !this.attack;
    }

    private boolean moveCheck() {
        return mc.gameSettings.keyBindForward.isKeyDown() && !mc.gameSettings.keyBindBack.isKeyDown();
    }

    private boolean otherModuleCheck() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }
        KillAura killAura = (KillAura) Astor.moduleManager.modules.get(KillAura.class);
        boolean auraOn = killAura != null && killAura.isEnabled();
        Scaffold scaffold = (Scaffold) Astor.moduleManager.modules.get(Scaffold.class);
        boolean scaffoldOff = scaffold == null || !scaffold.isEnabled();
        return auraOn && scaffoldOff && mc.thePlayer.ticksExisted >= 10;
    }

    private boolean rayTraceCheck(net.minecraft.entity.player.EntityPlayer player) {
        Vec3 playerPos = player.getPositionEyes(1.0F);
        float yaw = player.rotationYaw;
        float pitch = 0.0F;
        Vec3 direction = new Vec3(
                -Math.sin(yaw * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0),
                -Math.sin(pitch * Math.PI / 180.0),
                Math.cos(yaw * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0)
        );
        float len = this.maxFinalDistance.getValue().floatValue();
        Vec3 end = playerPos.addVector(
                direction.xCoord * len,
                direction.yCoord * len,
                direction.zCoord * len
        );
        return mc.theWorld.rayTraceBlocks(playerPos, end) == null;
    }

    /**
     * Astor's RotationManager writes through to the player's own rotation rather than keeping a separate
     * copy, so reading rotationYaw here already covers the case Astra handles with its isActive() branch.
     * The reach cap uses AimRange, standing in for Astra's KillAura range.
     */
    private Vec3 isCrosshairOnEntity(Entity targetEntity) {
        if (mc.thePlayer == null || targetEntity == null) {
            return null;
        }
        float size = targetEntity.getCollisionBorderSize();
        AxisAlignedBB bb = targetEntity.getEntityBoundingBox().expand(size, size, size);
        float serverYaw = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
        float serverPitch = mc.thePlayer.rotationPitch;
        Vec3 playerLookVec = this.getVectorForRotation(serverPitch, serverYaw);
        KillAura killAura = (KillAura) Astor.moduleManager.modules.get(KillAura.class);
        double reachDistance = mc.thePlayer.getDistanceToEntity(targetEntity);
        if (killAura != null) {
            reachDistance = Math.min(killAura.aimRange.getValue(), reachDistance);
        }
        Vec3 playerEyesPos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 rayEnd = playerEyesPos.addVector(
                playerLookVec.xCoord * reachDistance,
                playerLookVec.yCoord * reachDistance,
                playerLookVec.zCoord * reachDistance
        );
        MovingObjectPosition hitResult = bb.calculateIntercept(playerEyesPos, rayEnd);
        return hitResult != null ? hitResult.hitVec : null;
    }

    private Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    private EntityLivingBase getClosestEntity(double searchRange) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            return null;
        }
        double searchRangeSq = searchRange * searchRange;
        EntityLivingBase closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer) {
                continue;
            }
            EntityLivingBase elb = (EntityLivingBase) entity;
            if (!Target.get().isValidTarget(elb)) {
                continue;
            }
            double dx = elb.posX - mc.thePlayer.posX;
            double dy = elb.posY - mc.thePlayer.posY;
            double dz = elb.posZ - mc.thePlayer.posZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < searchRangeSq && distSq < closestDistSq) {
                closest = elb;
                closestDistSq = distSq;
            }
        }
        return closest;
    }

    // ------------------------------------------------------------------ packets

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (event.getType() == EventType.SEND) {
            if (this.outGoing.getValue()) {
                if (this.work && this.workMode.getValue() == 0
                        || this.workMode.getValue() == 1 && (this.work || this.timerBalance < 0.0 && !this.work)) {
                    this.blinkPacket(packet, event);
                } else {
                    this.processPacket(packet, event);
                }
            }
            if (packet instanceof C02PacketUseEntity) {
                this.attackTimer.reset();
                this.attack = true;
            } else if (this.attackTimer.hasTimeElapsed(400L)) {
                this.attack = false;
            }
            return;
        }
        if (packet instanceof S08PacketPlayerPosLook) {
            this.getHurt = true;
            this.delayTimer.reset();
            this.work = false;
            this.timerReset = false;
            this.setTimerSpeed(1.0F);
            this.timerBalance = 0.0;
        } else if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity p = (S12PacketEntityVelocity) packet;
            if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                this.getHurt = true;
            }
        }
    }

    private void blinkPacket(Packet<?> packet, PacketEvent event) {
        if (this.isClientPacket(packet)) {
            this.blinkPackets.add(packet);
            event.setCancelled(true);
        }
    }

    private void processPacket(Packet<?> packet, PacketEvent event) {
        if (this.blinkPackets.isEmpty()) {
            return;
        }
        this.blinkPacket(packet, event);
        while (!this.blinkPackets.isEmpty()) {
            Packet<?> p = this.blinkPackets.poll();
            if (p != null && this.isClientPacket(p) && mc.getNetHandler() != null) {
                PacketUtil.sendPacketNoEvent(p);
            }
        }
    }

    private boolean isClientPacket(Packet<?> packet) {
        return packet.getClass().getName().startsWith("net.minecraft.network.play.client.");
    }

    // ------------------------------------------------------------------ render

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        float partialTicks = event.getPartialTicks();
        if (!this.renderPoint.getValue() || this.predictedPosition == null) {
            return;
        }
        if ((this.renderOnlyWhenNeed.getValue() && this.work) || !this.renderOnlyWhenNeed.getValue()) {
            Vec3 fix = this.fixPos(this.predictedPosition.xCoord, this.predictedPosition.yCoord + 0.18, this.predictedPosition.zCoord);
            double smoothFactor = 0.05;
            this.lastRenderX = this.lastRenderX + (fix.xCoord - this.lastRenderX) * smoothFactor;
            this.lastRenderY = this.lastRenderY + (fix.yCoord - this.lastRenderY) * smoothFactor;
            this.lastRenderZ = this.lastRenderZ + (fix.zCoord - this.lastRenderZ) * smoothFactor;
            AxisAlignedBB renderBB = this.createPointBB(this.lastRenderX, this.lastRenderY, this.lastRenderZ, 0.2);
            Color color = new Color(255, 120, 20);
            RenderUtil.enableRenderState();
            RenderUtil.drawFilledBox(renderBB, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            RenderUtil.drawBoundingBox(renderBB, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 4.0F);
            RenderUtil.disableRenderState();
        }
        if (this.renderOnlyWhenNeed.getValue() && !this.work) {
            Vec3 newPos = this.fixPos(mc.thePlayer.posX, mc.thePlayer.posY + 0.18, mc.thePlayer.posZ);
            this.lastRenderX = newPos.xCoord;
            this.lastRenderY = newPos.yCoord;
            this.lastRenderZ = newPos.zCoord;
        }
    }

    private Vec3 fixPos(double fixX, double fixY, double fixZ) {
        return new Vec3(
                fixX - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                fixY - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                fixZ - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
        );
    }

    private static AxisAlignedBB createPointBB(double x, double y, double z, double size) {
        double i = size / 2.0;
        return new AxisAlignedBB(x - i, y - i, z - i, x + i, y + i, z + i);
    }

    private static double calculateDistance(Vec3 a, Vec3 b) {
        double deltaX = b.xCoord - a.xCoord;
        double deltaY = b.yCoord - a.yCoord;
        double deltaZ = b.zCoord - a.zCoord;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    // ------------------------------------------------------------------ player simulation

    private Vec3 updatePlayer(SimulatedPlayer state, int ticks) {
        this.handleMovement(state, ticks);
        this.handleCollisions(state);
        return this.updatePosition(state);
    }

    private void handleMovement(SimulatedPlayer state, int ticks) {
        net.minecraft.client.entity.EntityPlayerSP player = state.player;
        if (player.movementInput.jump) {
            state.motionY += 0.42;
        }
        if (player.isInWater()) {
            state.motionX *= 0.8;
            state.motionZ *= 0.8;
        }
        float strafe = player.movementInput.moveStrafe;
        float forward = player.movementInput.moveForward;
        if (forward != 0.0F || strafe != 0.0F) {
            double a1 = strafe * 0.98;
            double a2 = forward * 0.98;
            double a = fixedJumpFactor(player);
            a1 *= a;
            a2 *= a;
            double sinY = Math.sin(player.rotationYaw * Math.PI / 180.0);
            double cosY = Math.cos(player.rotationYaw * Math.PI / 180.0);
            state.motionX += state.motionX * ticks + (a1 * cosY - a2 * sinY) * ticks;
            state.motionZ += state.motionZ * ticks + (a2 * cosY + a1 * sinY) * ticks;
            state.motionX *= 0.91;
            state.motionZ *= 0.91;
        }
        if (!state.onGround) {
            state.motionY += state.gravity * ticks;
            state.motionY *= 0.98;
        }
    }

    private static double fixedJumpFactor(net.minecraft.client.entity.EntityPlayerSP player) {
        double factor = player.jumpMovementFactor;
        if (player.isSprinting()) {
            factor *= 1.3;
        }
        return factor;
    }

    private void handleCollisions(SimulatedPlayer state) {
        AxisAlignedBB boundingBox = new AxisAlignedBB(
                state.posX - 0.3, state.posY, state.posZ - 0.3,
                state.posX + 0.3, state.posY + 1.8, state.posZ + 0.3
        );
        List<AxisAlignedBB> collidingBoxes = state.world.getCollidingBoundingBoxes(state.player, boundingBox);
        for (AxisAlignedBB box : collidingBoxes) {
            if (box.calculateYOffset(boundingBox, (float) state.motionY) != 0.0F) {
                state.motionY = -state.motionY;
            }
        }
        state.onGround = state.motionY == 0.0 && !collidingBoxes.isEmpty();
    }

    private Vec3 updatePosition(SimulatedPlayer state) {
        state.posX += state.motionX;
        state.posY += state.motionY;
        state.posZ += state.motionZ;
        if (state.posY < 0.0) {
            state.posY = 0.0;
            state.motionY = 0.0;
        }
        return new Vec3(state.posX, state.posY, state.posZ);
    }

    // ------------------------------------------------------------------ timer

    private float getTimerSpeed() {
        return ((IAccessorMinecraft) mc).getTimer().timerSpeed;
    }

    private void setTimerSpeed(float speed) {
        ((IAccessorMinecraft) mc).getTimer().timerSpeed = speed;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.workMode.getModeString()};
    }

    private static class SimulatedPlayer {
        final net.minecraft.client.entity.EntityPlayerSP player;
        final World world;
        final double gravity = -0.08;
        double posX;
        double posY;
        double posZ;
        double motionX;
        double motionY;
        double motionZ;
        boolean onGround;

        SimulatedPlayer(net.minecraft.client.entity.EntityPlayerSP player) {
            this.player = player;
            this.world = player.worldObj;
            this.posX = player.posX;
            this.posY = player.posY;
            this.posZ = player.posZ;
            this.motionX = player.motionX;
            this.motionY = player.motionY;
            this.motionZ = player.motionZ;
            this.onGround = player.onGround;
        }
    }
}
