package net.dev.astor.mixin.network;

import net.dev.astor.module.impl.render.Ambience;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.network.NetHandlerPlayClient;

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