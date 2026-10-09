package net.dev.astor.module.impl.combat;

import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.mixin.render.IAccessorRenderManager;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.util.PacketUtil;
import net.dev.astor.util.RandomUtil;
import net.dev.astor.util.RenderUtil;
import net.dev.astor.util.TimerUtil;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.event.events.impl.player.LoadWorldEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.event.events.impl.render.Render3DEvent;
import net.dev.astor.event.types.EventType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

public class BackTrack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int MAX_BUFFERED_PACKETS = 1024;

    public final FloatProperty minHitRange = new FloatProperty("MinHitRange", 2.0F, 0.0F, 8.0F);
    public final FloatProperty maxHitRange = new FloatProperty("MaxHitRange", 6.0F, 0.0F, 8.0F);
    public final BooleanProperty dynamic = new BooleanProperty("Dynamic", true);
    public final IntProperty minDelay = new IntProperty("MinDelay", 600, 0, 1000);
    public final IntProperty maxDelay = new IntProperty("MaxDelay", 800, 0, 1000);
    public final IntProperty coolDownTimer = new IntProperty("CoolDownTimer", 600, 0, 1000);
    public final BooleanProperty onlyWhenNeed = new BooleanProperty("OnlyWhenNeed", true);
    public final BooleanProperty releaseOnS12 = new BooleanProperty("ReleaseOnS12", true);
    public final ModeProperty distanceMode = new ModeProperty(
            "DistanceMode", 1, new String[]{"ServerPrediction", "MotionPrediction"}
    );
    public final BooleanProperty cancelS32 = new BooleanProperty("CancelS32", true);
    public final BooleanProperty cancelS00 = new BooleanProperty("CancelS00", true);
    public final BooleanProperty handleS08 = new BooleanProperty("HandleS08", true);
    public final BooleanProperty handleS12 = new BooleanProperty("HandleS12", true);
    public final BooleanProperty handleS27 = new BooleanProperty("HandleS27", true);
    public final BooleanProperty onlyAura = new BooleanProperty("OnlyAura", true);

    private final Deque<QueuedPacket> queue = new ArrayDeque<>();
    private final TimerUtil coolDown = new TimerUtil();

    private EntityLivingBase target = null;
    private EntityLivingBase prevTarget = null;
    private boolean backtracking = false;
    private boolean updatedPreviousPosition = true;
    private double realX = 0.0;
    private double realY = 0.0;
    private double realZ = 0.0;
    private double previousX = 0.0;
    private double previousY = 0.0;
    private double previousZ = 0.0;
    private double packetX = 0.0;
    private double packetY = 0.0;
    private double packetZ = 0.0;
    private double currentDistance = 0.0;
    private double futureDistance = 0.0;
    private double range = 0.0;
    private long ping = 0L;
    private boolean blockingPacket = false;
    private double smoothX = 0.0;
    private double smoothY = 0.0;
    private double smoothZ = 0.0;
    private double lastRenderX = 0.0;
    private double lastRenderY = 0.0;
    private double lastRenderZ = 0.0;

    @Override
    public String getDescription() {
        return "Holds your movement packets back so a target walks past your server-side reach, then releases them to land hits from further away than you could normally reach.";
    }

    public BackTrack() {
        super("BackTrack", Category.COMBAT, false);
    }

    @Override
    public void onEnabled() {
        this.blockingPacket = false;
        this.backtracking = false;
        this.queue.clear();
        this.updatedPreviousPosition = false;
        this.range = this.rollRange();
        this.coolDown.reset();
        if (mc.thePlayer != null) {
            this.realX = mc.thePlayer.posX;
            this.realY = mc.thePlayer.posY;
            this.realZ = mc.thePlayer.posZ;
        }
    }

    @Override
    public void onDisabled() {
        this.releaseAllPackets();
        this.backtracking = false;
        this.blockingPacket = false;
        this.queue.clear();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.releaseAllPackets();
        this.backtracking = false;
        this.queue.clear();
        this.target = null;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (mc.theWorld == null || mc.thePlayer == null) {
            this.backtracking = false;
            this.queue.clear();
            return;
        }
        
        if (this.isConflict(LagRange.class)) {
            this.backtracking = false;
            this.releaseAllPackets();
            return;
        }

        this.target = this.getClosestTarget(10.0);
        if (!this.queue.isEmpty()) {
            this.ping = System.currentTimeMillis() - this.queue.peekFirst().time;
        }
        if (this.target == null || (this.onlyAura.getValue() && !this.isAuraActive())) {
            this.updatedPreviousPosition = false;
            this.backtracking = false;
            this.releaseAllPackets();
            return;
        }
        if (this.prevTarget != this.target) {
            this.realX = this.target.posX;
            this.realY = this.target.posY;
            this.realZ = this.target.posZ;
            this.updatedPreviousPosition = false;
            this.prevTarget = this.target;
        }
        if (!this.updatedPreviousPosition) {
            this.updatePreviousPosition();
        }

        double predictedX;
        double predictedY;
        double predictedZ;
        if (this.distanceMode.getValue() == 0) {
            predictedX = this.target.serverPosX / 32.0;
            predictedY = this.target.serverPosY / 32.0;
            predictedZ = this.target.serverPosZ / 32.0;
        } else {
            predictedX = 2.0 * this.target.posX - this.previousX;
            predictedY = 2.0 * this.target.posY - this.previousY;
            predictedZ = 2.0 * this.target.posZ - this.previousZ;
        }

        Vec3 eye = mc.thePlayer.getPositionEyes(1.0F);
        float size = this.target.getCollisionBorderSize();
        AxisAlignedBB bb = this.target.getEntityBoundingBox().expand(size, size, size);
        AxisAlignedBB futureBox = this.boxAt(bb, predictedX, predictedY, predictedZ, size);
        AxisAlignedBB realBox = this.boxAt(bb, this.realX, this.realY, this.realZ, size);
        this.currentDistance = this.distanceToBox(eye, bb);
        this.futureDistance = this.backtracking
                ? this.distanceToBox(eye, realBox)
                : this.distanceToBox(eye, futureBox);

        this.previousX = this.target.posX;
        this.previousY = this.target.posY;
        this.previousZ = this.target.posZ;

        this.lastRenderX = this.smoothX;
        this.lastRenderY = this.smoothY;
        this.lastRenderZ = this.smoothZ;

        this.range = this.rollRange();

        boolean inWindow = this.currentDistance > this.minHitRange.getValue()
                && this.currentDistance < this.maxHitRange.getValue();
        if (inWindow && (!this.onlyWhenNeed.getValue() || this.target.hurtTime * 50 <= this.range)) {
            this.backtracking = true;
        }
        if (this.currentDistance >= this.futureDistance
                || this.futureDistance < this.minHitRange.getValue()
                || this.futureDistance > this.maxHitRange.getValue()) {
            this.backtracking = false;
        }
        if (!this.coolDown.hasTimeElapsed(this.coolDownTimer.getValue())) {
            this.backtracking = false;
        }

        if (this.backtracking) {
            this.releasePacketToDistance();
        } else {
            this.releaseAllPackets();
        }
    }

    private void updatePreviousPosition() {
        this.previousX = this.target.posX;
        this.previousY = this.target.posY;
        this.previousZ = this.target.posZ;
        this.updatedPreviousPosition = true;
    }

    private double rollRange() {
        return RandomUtil.randomGaussianInRange(this.minDelay.getValue(), this.maxDelay.getValue(), true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || this.target == null) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (event.getType() == EventType.RECEIVE) {
            this.trackServerPosition(packet);
        }
        if (this.blockingPacket) {
            this.blockPacket(packet, event);
        }
    }

    private void trackServerPosition(Packet<?> packet) {
        if (packet instanceof S14PacketEntity) {
            S14PacketEntity s14 = (S14PacketEntity) packet;
            if (s14.getEntity(mc.theWorld) == this.target) {
                this.realX += s14.func_149062_c() / 32.0;
                this.realY += s14.func_149061_d() / 32.0;
                this.realZ += s14.func_149064_e() / 32.0;
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport s18 = (S18PacketEntityTeleport) packet;
            if (s18.getEntityId() == this.target.getEntityId()) {
                this.realX = s18.getX() / 32.0;
                this.realY = s18.getY() / 32.0;
                this.realZ = s18.getZ() / 32.0;
            }
        }
    }

    private void blockPacket(Packet<?> packet, PacketEvent event) {
        if (mc.thePlayer.ticksExisted < 20) {
            return;
        }
        if (this.releaseOnS12.getValue()
                && packet instanceof S12PacketEntityVelocity
                && ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId()) {
            this.backtracking = false;
        }
        if (this.isPacketToBeBlocked(packet)) {
            if (this.queue.size() >= MAX_BUFFERED_PACKETS) {
                return;
            }
            this.queue.addLast(new QueuedPacket(packet, event.getType(), System.currentTimeMillis()));
            event.setCancelled(true);
        }
    }

    private boolean isPacketToBeBlocked(Packet<?> packet) {
        return (packet instanceof S00PacketKeepAlive && this.cancelS00.getValue())
                || packet instanceof S12PacketEntityVelocity
                || packet instanceof S27PacketExplosion
                || (packet instanceof S32PacketConfirmTransaction && this.cancelS32.getValue())
                || packet instanceof S14PacketEntity
                || packet instanceof S18PacketEntityTeleport
                || packet instanceof S19PacketEntityHeadLook
                || packet instanceof net.minecraft.network.play.server.S0FPacketSpawnMob
                || packet instanceof S08PacketPlayerPosLook;
    }

    private void releasePacketToDistance() {
        if (this.queue.isEmpty()) {
            this.blockingPacket = true;
            return;
        }
        this.packetX = this.target.posX;
        this.packetY = this.target.posY;
        this.packetZ = this.target.posZ;
        double maxDistance = this.maxHitRange.getValue();
        double minDistance = this.minHitRange.getValue();
        double distance = this.currentDistance;
        float size = this.target.getCollisionBorderSize();
        AxisAlignedBB bb = this.target.getEntityBoundingBox().expand(size, size, size);
        Vec3 eye = mc.thePlayer.getPositionEyes(1.0F);

        while (!this.queue.isEmpty()) {
            long waited = System.currentTimeMillis() - this.queue.peekFirst().time;
            boolean outOfWindow = distance < minDistance || distance > maxDistance;
            if (!outOfWindow && waited <= this.range) {
                break;
            }
            if (mc.getNetHandler() == null) {
                return;
            }
            QueuedPacket head = this.queue.pollFirst();
            if (head != null) {
                this.deliver(head);
            }
            if (this.queue.isEmpty()) {
                break;
            }
            this.applyHeadPositionToPacketState();
            if (this.dynamic.getValue()) {
                distance = this.distanceToBox(eye, this.boxAt(bb, this.packetX, this.packetY, this.packetZ, size));
            }
        }
        this.blockingPacket = true;
    }

    private void applyHeadPositionToPacketState() {
        if (this.queue.isEmpty()) {
            return;
        }
        Packet<?> head = this.queue.peekFirst().packet;
        if (head instanceof S14PacketEntity) {
            S14PacketEntity s14 = (S14PacketEntity) head;
            if (s14.getEntity(mc.theWorld) == this.target) {
                this.packetX += s14.func_149062_c() / 32.0;
                this.packetY += s14.func_149061_d() / 32.0;
                this.packetZ += s14.func_149064_e() / 32.0;
            }
        } else if (head instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport s18 = (S18PacketEntityTeleport) head;
            if (s18.getEntityId() == this.target.getEntityId()) {
                this.packetX = s18.getX() / 32.0;
                this.packetY = s18.getY() / 32.0;
                this.packetZ = s18.getZ() / 32.0;
            }
        }
    }

    private void releaseAllPackets() {
        if (mc.getNetHandler() == null) {
            this.queue.clear();
            this.blockingPacket = false;
            return;
        }
        while (!this.queue.isEmpty()) {
            QueuedPacket queued = this.queue.pollFirst();
            if (queued != null) {
                this.deliver(queued);
            }
        }
        this.blockingPacket = false;
    }

    private void deliver(QueuedPacket queued) {
        Packet<?> packet = queued.packet;
        if (queued.type == EventType.SEND) {
            PacketUtil.sendPacketNoEvent(packet);
            return;
        }
        if (packet instanceof S08PacketPlayerPosLook && !this.handleS08.getValue()) {
            return;
        }
        if (packet instanceof S12PacketEntityVelocity && !this.handleS12.getValue()) {
            return;
        }
        if (packet instanceof S27PacketExplosion && !this.handleS27.getValue()) {
            return;
        }
        this.processClientbound(packet);
    }

    @SuppressWarnings("unchecked")
    private void processClientbound(Packet<?> packet) {
        
        ((Packet<net.minecraft.network.play.INetHandlerPlayClient>) packet).processPacket(mc.getNetHandler());
    }

    private AxisAlignedBB boxAt(AxisAlignedBB box, double x, double y, double z, float size) {
        double halfWidth = (box.maxX - box.minX) / 2.0 - size;
        double height = box.maxY - box.minY - 2.0 * size;
        return new AxisAlignedBB(
                x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + height, z + halfWidth
        );
    }

    private double distanceToBox(Vec3 from, AxisAlignedBB box) {
        double dx = Math.max(box.minX - from.xCoord, Math.max(0.0, from.xCoord - box.maxX));
        double dy = Math.max(box.minY - from.yCoord, Math.max(0.0, from.yCoord - box.maxY));
        double dz = Math.max(box.minZ - from.zCoord, Math.max(0.0, from.zCoord - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private EntityLivingBase getClosestTarget(double rangeSq) {
        if (mc.theWorld == null) {
            return null;
        }
        EntityLivingBase best = null;
        double bestSq = rangeSq;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            double dx = entity.posX - mc.thePlayer.posX;
            double dy = entity.posY - mc.thePlayer.posY;
            double dz = entity.posZ - mc.thePlayer.posZ;
            double sq = dx * dx + dy * dy + dz * dz;
            if (sq < bestSq && net.dev.astor.module.impl.misc.Target.get().isValidTarget(living)) {
                bestSq = sq;
                best = living;
            }
        }
        return best;
    }

    private boolean isAuraActive() {
        KillAura killAura = (KillAura) Astor.moduleManager.modules.get(KillAura.class);
        return killAura != null && killAura.isEnabled();
    }

    private boolean isConflict(Class<? extends Module> other) {
        Module m = Astor.moduleManager.modules.get(other);
        return m != null && m.isEnabled();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || this.target == null) {
            return;
        }
        if (!this.queue.isEmpty()) {
            double partialTicks = event.getPartialTicks();
            this.smoothX = this.lastRenderX + (this.realX - this.lastRenderX) * partialTicks;
            this.smoothY = this.lastRenderY + (this.realY - this.lastRenderY) * partialTicks;
            this.smoothZ = this.lastRenderZ + (this.realZ - this.lastRenderZ) * partialTicks;
            AxisAlignedBB box = new AxisAlignedBB(
                    this.smoothX - mc.thePlayer.width / 2.0, this.smoothY, this.smoothZ - mc.thePlayer.width / 2.0,
                    this.smoothX + mc.thePlayer.width / 2.0, this.smoothY + mc.thePlayer.height, this.smoothZ + mc.thePlayer.width / 2.0
            ).offset(
                    -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                    -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                    -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
            );
            RenderUtil.enableRenderState();
            RenderUtil.drawBoundingBox(box, 160, 255, 195, 255, 2.0F);
            RenderUtil.drawFilledBox(box, 160, 255, 195, 40);
            RenderUtil.disableRenderState();
        } else {
            this.smoothX = this.realX;
            this.smoothY = this.realY;
            this.smoothZ = this.realZ;
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.ping + "ms"};
    }

    static class QueuedPacket {
        final Packet<?> packet;
        final EventType type;
        final long time;

        QueuedPacket(Packet<?> packet, EventType type, long time) {
            this.packet = packet;
            this.type = type;
            this.time = time;
        }
    }
}

