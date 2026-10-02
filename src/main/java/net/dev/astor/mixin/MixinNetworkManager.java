package net.dev.astor.mixin;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import net.dev.astor.Astor;
import net.dev.astor.event.EventManager;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.event.types.EventType;
import net.dev.astor.module.impl.exploit.ExploitFixer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Future;

@SideOnly(Side.CLIENT)
@Mixin(value = {NetworkManager.class}, priority = 9999)
public abstract class MixinNetworkManager {
    @Inject(
            method = {"channelRead0*"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo callbackInfo) {
        if (!packet.getClass().getName().startsWith("net.minecraft.network.play.client")) {
            if (Astor.delayManager != null && Astor.delayManager.shouldDelay((Packet<INetHandlerPlayClient>) packet)) {
                callbackInfo.cancel();
            } else {
                PacketEvent event = new PacketEvent(EventType.RECEIVE, packet);
                EventManager.call(event);
                if (event.isCancelled()) {
                    callbackInfo.cancel();
                }
            }
        }
    }

    /**
     * Lets a malformed or hostile packet throw instead of taking the client down with it.
     * ThreadQuickExitException is how a handler deliberately abandons the rest of a packet, so it
     * stays swallowed silently. The explicit descriptor is required: the class also holds the
     * synthetic bridge for channelRead0(Context, Object), which never calls processPacket and would
     * leave this redirect without a target.
     */
    @Redirect(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Packet;processPacket(Lnet/minecraft/network/INetHandler;)V"
            )
    )
    private void safeProcessPacket(Packet<?> packet, INetHandler handler) {
        try {
            ((Packet<INetHandler>) packet).processPacket(handler);
        } catch (ThreadQuickExitException ignored) {
            // Minecraft uses this to abort a packet on purpose, nothing went wrong.
        } catch (Exception e) {
            ExploitFixer.reportPacketFailure(packet, e);
        }
    }

    @Inject(
            method = {"sendPacket(Lnet/minecraft/network/Packet;)V"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void sendPacket(Packet<?> packet, CallbackInfo callbackInfo) {
        if (!packet.getClass().getName().startsWith("net.minecraft.network.play.server")) {
            PacketEvent event = new PacketEvent(EventType.SEND, packet);
            EventManager.call(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
            } else if (Astor.playerStateManager != null && Astor.blinkManager != null && Astor.lagManager != null) {
                if (!Astor.lagManager.isFlushing()) {
                    Astor.playerStateManager.handlePacket(packet);
                    if (Astor.blinkManager.isBlinking()) {
                        if (Astor.blinkManager.offerPacket(packet)) {
                            callbackInfo.cancel();
                            return;
                        }
                    }
                    if (Astor.lagManager.handlePacket(packet)) {
                        callbackInfo.cancel();
                    }
                }
            }
        }
    }

    @Inject(
            method = {"sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;[Lio/netty/util/concurrent/GenericFutureListener;)V"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void sendPacket2(
            Packet<?> packet,
            GenericFutureListener<? extends Future<? super Void>> genericFutureListener,
            GenericFutureListener<? extends Future<? super Void>>[] arr,
            CallbackInfo callbackInfo
    ) {
        if (!packet.getClass().getName().startsWith("net.minecraft.network.play.server")) {
            if (Astor.playerStateManager != null && Astor.blinkManager != null && Astor.lagManager != null) {
                if (!Astor.lagManager.isFlushing()) {
                    Astor.playerStateManager.handlePacket(packet);
                    if (Astor.blinkManager.isBlinking()) {
                        if (Astor.blinkManager.offerPacket(packet)) {
                            callbackInfo.cancel();
                            return;
                        }
                    }
                    if (Astor.lagManager.handlePacket(packet)) {
                        callbackInfo.cancel();
                    }
                }
            }
        }
    }
}
