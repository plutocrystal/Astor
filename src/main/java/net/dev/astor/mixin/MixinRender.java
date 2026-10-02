package net.dev.astor.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * The half of Render that every subclass inherits. Nothing is injected here, it exists so that
 * RenderEntityItem's mixin can reach these members the same way MixinEntityPlayerSP reaches the
 * members MixinEntityPlayer shadows for it, rather than shadowing a superclass member from a mixin
 * that only targets the subclass.
 */
@SideOnly(Side.CLIENT)
@Mixin(value = {Render.class}, priority = 9999)
public abstract class MixinRender {
    @Shadow
    protected RenderManager renderManager;

    @Shadow
    protected abstract boolean bindEntityTexture(Entity entity);

    @Shadow
    protected abstract ResourceLocation getEntityTexture(Entity entity);

    @Shadow
    protected abstract void renderName(Entity entity, double x, double y, double z);

    @Shadow
    public abstract FontRenderer getFontRendererFromRenderManager();
}