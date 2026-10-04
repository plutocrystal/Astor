package net.dev.astor.mixin.render;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.render.Glint;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@SideOnly(Side.CLIENT)
@Mixin(value = {RenderItem.class}, priority = 9999)
public abstract class MixinRenderItem {
    @ModifyConstant(
            method = {"renderEffect"},
            constant = @Constant(intValue = -8372020)
    )
    private int glintTint(int original) {
        if (Astor.moduleManager == null) {
            return original;
        }
        Glint glint = (Glint) Astor.moduleManager.modules.get(Glint.class);
        return glint.isEnabled() ? glint.getTint() : original;
    }
}