package net.dev.astor.module.impl.movement;

import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.event.events.impl.player.UpdateEvent;
import net.dev.astor.event.types.EventType;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class Freeze extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private double motionX;
    private double motionY;
    private double motionZ;
    private double x;
    private double y;
    private double z;

    @Override
    public String getDescription() {
        return "Pins you in place by dropping outgoing movement packets and forcing your position back, so the server never sees you move.";
    }

    public Freeze() {
        super("Freeze", Category.MOVEMENT, false);
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer == null) {
            return;
        }
        this.x = mc.thePlayer.posX;
        this.y = mc.thePlayer.posY;
        this.z = mc.thePlayer.posZ;
        this.motionX = mc.thePlayer.motionX;
        this.motionY = mc.thePlayer.motionY;
        this.motionZ = mc.thePlayer.motionZ;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE || mc.thePlayer == null) {
            return;
        }
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionY = 0;
        mc.thePlayer.motionZ = 0;
        mc.thePlayer.setPositionAndRotation(this.x, this.y, this.z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }

    @EventTarget
    public void onPacketSend(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.SEND || !(event.getPacket() instanceof C03PacketPlayer)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventTarget
    public void onPacketReceive(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE
                || !(event.getPacket() instanceof S08PacketPlayerPosLook)) {
            return;
        }
        S08PacketPlayerPosLook posLook = (S08PacketPlayerPosLook) event.getPacket();
        this.x = posLook.getX();
        this.y = posLook.getY();
        this.z = posLook.getZ();
        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer == null) {
            return;
        }
        mc.thePlayer.motionX = this.motionX;
        mc.thePlayer.motionY = this.motionY;
        mc.thePlayer.motionZ = this.motionZ;
        mc.thePlayer.setPositionAndRotation(this.x, this.y, this.z, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }
}

