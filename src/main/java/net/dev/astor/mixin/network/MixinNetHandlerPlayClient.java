package net.dev.astor.mixin.network;

import net.dev.astor.module.impl.render.Ambience;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.network.NetHandlerPlayClient;

/**
 * Lets Ambience hold the time of day.
 *
 * <p>The server owns the clock here: S03PacketTimeUpdate arrives roughly once a second and its handler
 * writes straight into the world. Replacing that write is what makes a locked time stick - re-applying
 * it every tick would only ever win the tick it happened on and leave the sky stepping in between.</p>
 *
 * <p>Total world time is deliberately left alone. It is the day counter, not the visible time, and
 * nothing in the sky rendering depends on it.</p>
 */
@SideOnly(Side.CLIENT)
@Mixin(value = {NetHandlerPlayClient.class}, priority = 9999)
public abstract class MixinNetHandlerPlayClient {
    @Redirect(
            method = {"handleTimeUpdate"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;setWorldTime(J)V"
            )
    )
    private void ambiencesLockedTime(WorldClient world, long serverTime) {
        Ambience ambience = Ambience.get();
        Long locked = ambience == null ? null : ambience.getLockedTime();
        world.setWorldTime(locked == null ? serverTime : locked);
    }
}