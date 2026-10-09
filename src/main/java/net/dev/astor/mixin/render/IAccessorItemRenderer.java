package net.dev.astor.mixin.render;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin({ItemRenderer.class})
public interface IAccessorItemRenderer {
    
    @Invoker("transformFirstPersonItem")
    void transformFirstPersonItem(float equipProgress, float swingProgress);

    @Invoker("doBlockTransformations")
    void blockTransformation();
}

