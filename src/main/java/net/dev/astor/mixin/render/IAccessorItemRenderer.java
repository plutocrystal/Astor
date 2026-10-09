package net.dev.astor.mixin.render;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches the two private transforms in {@code ItemRenderer} that the hand render is built from.
 *
 * <p>Both are private in this build, which is why vanilla's own {@code renderItemInFirstPerson} can call
 * them but nothing else can. Animations needs them to reproduce vanilla's transform as a starting point
 * and then layer its own matrix on top.</p>
 */
@SideOnly(Side.CLIENT)
@Mixin({ItemRenderer.class})
public interface IAccessorItemRenderer {
    /**
     * @see net.minecraft.client.renderer.ItemRenderer#transformFirstPersonItem(float, float)
     */
    @Invoker("transformFirstPersonItem")
    void transformFirstPersonItem(float equipProgress, float swingProgress);

    /**
     * @see net.minecraft.client.renderer.ItemRenderer#doBlockTransformations()
     */
    @Invoker("doBlockTransformations")
    void blockTransformation();
}
