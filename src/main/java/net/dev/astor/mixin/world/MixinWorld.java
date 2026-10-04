package net.dev.astor.mixin.world;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.movement.Jesus;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(value = {World.class}, priority = 9999)
public abstract class MixinWorld {
    @Redirect(
            method = {"handleMaterialAcceleration"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isPushedByWater()Z"
            )
    )
    private boolean handleMaterialAcceleration(Entity entity) {
        if (entity instanceof EntityPlayerSP && Astor.moduleManager != null) {
            Jesus jesus = (Jesus) Astor.moduleManager.modules.get(Jesus.class);
            if (jesus.isEnabled() && jesus.noPush.getValue()) {
                return false;
            }
        }
        return entity.isPushedByWater();
    }
}