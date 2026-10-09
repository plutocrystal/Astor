package net.dev.astor.mixin.movement;

import net.dev.astor.module.impl.movement.Sneak;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@SideOnly(Side.CLIENT)
@Mixin(value = {MovementInputFromOptions.class}, priority = 9998)
public abstract class MixinMovementInputFromOptions {
    @ModifyConstant(
            method = {"updatePlayerMoveState"},
            constant = {@Constant(doubleValue = 0.3D)}
    )
    private double sneakSpeed(double speed) {
        Sneak sneak = Sneak.get();
        return sneak != null && sneak.isEnabled() ? sneak.speed.getValue().doubleValue() : speed;
    }
}