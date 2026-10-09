package net.dev.astor.util;

import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;

public class PacketUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    @SuppressWarnings("unchecked")
    private static final GenericFutureListener<? extends Future<? super Void>>[] NO_LISTENERS =
            new GenericFutureListener[0];

    public static void sendPacket(Packet<?> packet) {
        mc.getNetHandler().getNetworkManager().sendPacket(packet);
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        mc.getNetHandler().getNetworkManager().sendPacket(packet, null, NO_LISTENERS);
    }
}