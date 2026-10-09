package net.dev.astor.mixin.movement;

import net.dev.astor.module.impl.movement.Sneak;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Lets Sneak replace the factor vanilla scales crouching movement input by.
 *
 * <p>The target is the one place in this version that does it - {@code MovementInputFromOptions}
 * multiplies {@code moveStrafe} and {@code moveForward} by 0.3 whenever the sneak key is down, and
 * nothing touches them again before the movement packet goes out. Swapping the constant is therefore
 * exactly the change the module is asking for, rather than an approximation of it applied further
 * downstream where the original value would have to be divided back out.</p>
 *
 * <p>Both multiplications share the constant, so one handler covers the strafe and the forward axis.</p>
 */
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