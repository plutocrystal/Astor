package net.dev.astor.module.impl.combat;

import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.misc.Target;
import net.dev.astor.mixin.render.IAccessorRenderManager;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.util.PacketUtil;
import net.dev.astor.util.RandomUtil;
import net.dev.astor.util.RenderUtil;
import net.dev.astor.util.TimerUtil;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.attack.AttackEvent;
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
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Ported from the RN_Random_Name "LagRange" script (LiquidBounce based). Buffers outgoing movement
 * packets and trickles them back out on a millisecond schedule instead of by tick count, so the lag the
 * opponent sees varies the way a real network does rather than in fixed 50 ms steps.
 *
 * <p>This replaces the previous tick-count implementation, which drove {@code Astor.lagManager}. The
 * manager is untouched and simply stays at delay 0, which makes it a pass-through.</p>
 */
public class LagRange extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int MAX_BUFFERED_PACKETS = 512;

    public final BooleanProperty dynamic = new BooleanProperty("Dynamic", true);
    public final IntProperty minDelay = new IntProperty("MinDelay", 350, 0, 2000);
    public final IntProperty maxDelay = new IntProperty("MaxDelay", 400, 0, 2000);
    public final IntProperty coolDownTimer = new IntProperty("CoolDownTimer", 0, 0, 1000);
    public final FloatProperty attackRange = new FloatProperty("AttackRange", 3.0F, 0.0F, 6.0F);
    public final FloatProperty initialRange = new FloatProperty("InitialRange", 8.0F, 0.0F, 12.0F);
    public final IntProperty hurtTime = new IntProperty("HurtTime", 3, 0, 10);
    public final BooleanProperty cancelC0F = new BooleanProperty("CancelC0F", true);
    public final BooleanProperty onlyAura = new BooleanProperty("OnlyAura", true);
    public final BooleanProperty thirdPersonViewRender = new BooleanProperty("ThirdPersonViewRender", true);
    public final BooleanProperty debug = new BooleanProperty("Debug", false);

    private final Deque<QueuedPacket> queue = new ArrayDeque<>();
    private final TimerUtil coolDown = new TimerUtil();

    private EntityLivingBase target = null;
    private boolean blockingPacket = false;
    private boolean working = true;
    private boolean release = false;
    private double range = 0.0;
    private long ping = 0L;
    private double realX = 0.0;
    private double realY = 0.0;
    private double realZ = 0.0;
    private double previousX = 0.0;
    private double previousY = 0.0;
    private double previousZ = 0.0;
    private double smoothX = 0.0;
    private double smoothY = 0.0;
    private double smoothZ = 0.0;
    private double lastRenderX = 0.0;
    private double lastRenderY = 0.0;
    private double lastRenderZ = 0.0;

    @Override
    public String getDescription() {
        return "Buffers movement packets and releases them on a millisecond schedule, so the lag an opponent sees varies the way a real network does instead of in fixed 50ms steps.";
    }

    public LagRange() {
        super("LagRange", Category.COMBAT, false);
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void onEnabled() {
        this.range = this.rollRange();
        this.blockingPacket = false;
        this.target = null;
        this.working = true;
        this.release = false;
        this.queue.clear();
        this.coolDown.reset();
    }

    @Override
    public void onDisabled() {
        this.releaseAllPackets();
        this.blockingPacket = false;
        this.release = false;
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.releaseAllPackets();
        this.target = null;
        this.working = true;
        this.blockingPacket = false;
    }

    // ------------------------------------------------------------------ tick

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        // BackTrack holds C03 in its own queue; two buffers in series would double the lag.
        if (this.isConflict(BackTrack.class)) {
            this.releaseAllPackets();
            return;
        }

        if (!this.queue.isEmpty()) {
            this.ping = System.currentTimeMillis() - this.queue.peekFirst().time;
        }
        this.lastRenderX = this.smoothX;
        this.lastRenderY = this.smoothY;
        this.lastRenderZ = this.smoothZ;

        this.target = this.getClosestTarget(this.initialRange.getValue() + 1.0);
        if (this.target == null
                || mc.thePlayer.ticksExisted < 20
                || (this.onlyAura.getValue() && !this.isAuraActive())) {
            this.releaseAllPackets();
            return;
        }
        if (this.release) {
            this.resetCoolDown();
            this.releaseAllPackets();
            this.release = !this.queue.isEmpty();
            return;
        }

        Vec3 eye = new Vec3(this.realX, this.realY + mc.thePlayer.getEyeHeight(), this.realZ);
        float border = this.target.getCollisionBorderSize();
        AxisAlignedBB box = this.target.getEntityBoundingBox().expand(border, border, border);
        double realDistance = this.distanceToBox(eye, box);
        if (realDistance >= this.attackRange.getValue()) {
            this.working = true;
        }
        if (!this.working) {
            this.resetCoolDown();
            this.releaseAllPackets();
            return;
        }

        boolean movingCloser = this.isPlayerMovingCloser();
        this.previousX = this.realX;
        this.previousY = this.realY;
        this.previousZ = this.realZ;
        if (!movingCloser) {
            this.resetCoolDown();
            this.releaseAllPackets();
            return;
        }

        if (!this.coolDown.hasTimeElapsed(this.coolDownTimer.getValue())) {
            this.releaseAllPackets();
            return;
        }
        if (realDistance < this.initialRange.getValue()) {
            this.releasePacketsByDelay();
        } else {
            this.releaseAllPackets();
        }
    }

    private boolean isPlayerMovingCloser() {
        return (mc.thePlayer.posX - this.previousX) * (this.target.posX - mc.thePlayer.posX)
                + (mc.thePlayer.posY - this.previousY) * (this.target.posY - mc.thePlayer.posY)
                + (mc.thePlayer.posZ - this.previousZ) * (this.target.posZ - mc.thePlayer.posZ) > 0.0;
    }

    private void resetCoolDown() {
        if (!this.queue.isEmpty()) {
            this.coolDown.reset();
        }
    }

    private double rollRange() {
        return RandomUtil.randomGaussianInRange(this.minDelay.getValue(), this.maxDelay.getValue(), true);
    }

    // ------------------------------------------------------------------ attack

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || this.target == null) {
            return;
        }
        if (this.target.hurtTime <= this.hurtTime.getValue()) {
            this.release = true;
            this.working = false;
            this.realX = mc.thePlayer.posX;
            this.realY = mc.thePlayer.posY;
            this.realZ = mc.thePlayer.posZ;
        } else {
            this.working = true;
        }
    }

    // ------------------------------------------------------------------ packets

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (event.getType() == EventType.RECEIVE && packet instanceof S08PacketPlayerPosLook) {
            if (this.debug.getValue()) {
                net.dev.astor.util.ChatUtil.sendFormatted(String.format("%s[LagRange] Reset", net.dev.astor.enums.ChatColors.RED));
            }
        }
        if (event.getType() == EventType.SEND && this.queue.isEmpty() && packet instanceof C03PacketPlayer) {
            // Follow the position the server is being told, not the local one, so the distance maths
            // matches what the opponent will compute.
            C03PacketPlayer c03 = (C03PacketPlayer) packet;
            if (c03.getPositionX() != 0.0) {
                this.realX = c03.getPositionX();
            }
            if (c03.getPositionY() != 0.0) {
                this.realY = c03.getPositionY();
            }
            if (c03.getPositionZ() != 0.0) {
                this.realZ = c03.getPositionZ();
            }
        }
        if (this.blockingPacket) {
            this.blockPacket(packet, event);
        }
    }

    private void blockPacket(Packet<?> packet, PacketEvent event) {
        if (event.getType() != EventType.SEND) {
            return;
        }
        if (!this.cancelC0F.getValue() && packet instanceof C0FPacketConfirmTransaction) {
            return;
        }
        if (this.queue.size() >= MAX_BUFFERED_PACKETS) {
            return;
        }
        this.queue.addLast(new QueuedPacket(packet, System.currentTimeMillis()));
        event.setCancelled(true);
    }

    /**
     * Releases each buffered packet once it has waited longer than a freshly rolled delay, which is what
     * makes the added lag jitter instead of arriving in even steps.
     */
    private void releasePacketsByDelay() {
        if (this.queue.isEmpty()) {
            this.blockingPacket = true;
            return;
        }
        this.range = this.rollRange();
        while (!this.queue.isEmpty()) {
            long delay = System.currentTimeMillis() - this.queue.peekFirst().time;
            if (delay < this.range) {
                break;
            }
            QueuedPacket head = this.queue.pollFirst();
            if (head != null) {
                this.trackPosition(head.packet);
                PacketUtil.sendPacketNoEvent(head.packet);
            }
            if (!this.dynamic.getValue() || this.queue.isEmpty()) {
                break;
            }
        }
        this.blockingPacket = true;
    }

    private void releaseAllPackets() {
        if (mc.getNetHandler() == null) {
            this.queue.clear();
            this.blockingPacket = false;
            return;
        }
        while (!this.queue.isEmpty()) {
            QueuedPacket head = this.queue.pollFirst();
            if (head != null) {
                this.trackPosition(head.packet);
                PacketUtil.sendPacketNoEvent(head.packet);
            }
        }
        this.blockingPacket = false;
    }

    private void trackPosition(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer c03 = (C03PacketPlayer) packet;
            if (c03.getPositionX() != 0.0) {
                this.realX = c03.getPositionX();
            }
            if (c03.getPositionY() != 0.0) {
                this.realY = c03.getPositionY();
            }
            if (c03.getPositionZ() != 0.0) {
                this.realZ = c03.getPositionZ();
            }
        }
    }

    // ------------------------------------------------------------------ helpers

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
        double bestSq = rangeSq * rangeSq;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            double dx = entity.posX - mc.thePlayer.posX;
            double dy = entity.posY - mc.thePlayer.posY;
            double dz = entity.posZ - mc.thePlayer.posZ;
            double sq = dx * dx + dy * dy + dz * dz;
            if (sq < bestSq && Target.get().isValidTarget(living)) {
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

    // ------------------------------------------------------------------ render

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.theWorld == null) {
            return;
        }
        if (!this.queue.isEmpty() && (mc.gameSettings.thirdPersonView != 0 || !this.thirdPersonViewRender.getValue())) {
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
            RenderUtil.drawBoundingBox(box, 114, 230, 255, 255, 2.0F);
            RenderUtil.drawFilledBox(box, 114, 230, 255, 40);
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
        final long time;

        QueuedPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }
    }
}
