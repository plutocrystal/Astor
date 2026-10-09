package net.dev.astor.mixin.render;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.render.ItemPhysics;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;

import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@SideOnly(Side.CLIENT)
@Mixin(value = {RenderEntityItem.class}, priority = 9999)

@SuppressWarnings("deprecation")
public abstract class MixinRenderEntityItem extends MixinRender {
    
    private static final net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType GROUND =
            net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.GROUND;

    @Shadow
    private Random field_177079_e;
    @Shadow
    private RenderItem itemRenderer;
    @Shadow
    protected int func_177078_a(ItemStack stack) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    public abstract boolean shouldBob();

    @Shadow(remap = false)
    public abstract boolean shouldSpreadItems();

    @Inject(
            method = {"doRender"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void doRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo callbackInfo) {
        ItemPhysics itemPhysics = this.getModule();
        if (itemPhysics == null || !itemPhysics.isEnabled()) {
            return;
        }
        ItemStack stack = entity.getEntityItem();
        this.field_177079_e.setSeed(187L);
        boolean blurred = false;
        if (this.bindEntityTexture(entity)) {
            this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).setBlurMipmap(false, false);
            blurred = true;
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();
        IBakedModel model = this.itemRenderer.getItemModelMesher().getItemModel(stack);

        Vector3f groundScale = model.getItemCameraTransforms().ground.scale;
        int count = this.physicsLayout(itemPhysics, entity, x, y, z, partialTicks, model);
        for (int j = 0; j < count; ++j) {
            if (model.isGui3d()) {
                GlStateManager.pushMatrix();
                if (j > 0) {
                    float jitter = itemPhysics.is17Mode() ? 0.2F : 0.15F;
                    GlStateManager.translate(
                            this.shouldSpreadItems() ? this.jitter(jitter) : 0.0F,
                            this.shouldSpreadItems() ? this.jitter(jitter) : 0.0F,
                            this.jitter(jitter)
                    );
                }
                float scale = 0.5F * itemPhysics.getScale();
                GlStateManager.scale(scale, scale, scale);
                model = ForgeHooksClient.handleCameraTransforms(model, GROUND);
                this.itemRenderer.renderItem(stack, model);
                GlStateManager.popMatrix();
            } else {
                GlStateManager.pushMatrix();
                if (itemPhysics.is17Mode() && j > 0) {
                    GlStateManager.translate(
                            this.shouldSpreadItems() ? this.jitter(0.15F) : 0.0F,
                            this.shouldSpreadItems() ? this.jitter(0.15F) : 0.0F,
                            this.jitter(0.15F)
                    );
                }
                if (itemPhysics.is17Mode() && !this.isFancy()) {
                    GlStateManager.rotate(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
                }
                float scale = itemPhysics.getScale();
                GlStateManager.scale(scale, scale, scale);
                model = ForgeHooksClient.handleCameraTransforms(model, GROUND);
                this.itemRenderer.renderItem(stack, model);
                GlStateManager.popMatrix();
                float step = itemPhysics.is17Mode()
                        ? (this.isFancy() ? 0.0421875F : 0.0F)
                        : 0.046875F * groundScale.z;
                GlStateManager.translate(0.0F * groundScale.x, 0.0F * groundScale.y, step);
            }
        }
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        this.bindEntityTexture(entity);
        if (blurred) {
            this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).restoreLastBlurMipmap();
        }
        this.renderOverlay(itemPhysics, entity, x, y, z, partialTicks);
        this.renderName(entity, x, y, z);
        callbackInfo.cancel();
    }

    @Unique
    private int physicsLayout(ItemPhysics itemPhysics, EntityItem entity, double x, double y, double z, float partialTicks, IBakedModel model) {
        ItemStack stack = entity.getEntityItem();
        if (stack.getItem() == null) {
            return 0;
        }
        boolean is3D = model.isGui3d();
        boolean fancy = this.isFancy();
        int count = itemPhysics.isCountEnabled() ? 1
                : (itemPhysics.is17Mode()
                ? (fancy && !is3D ? ItemPhysics.get17FlatCount(stack) : ItemPhysics.get17BlockCount(stack))
                : this.func_177078_a(stack));
        float bob = itemPhysics.isPhysicsMode() ? 0.0F
                : (this.shouldBob()
                ? MathHelper.sin((entity.getAge() + partialTicks) / 10.0F + entity.hoverStart) * 0.1F + 0.1F
                : 0.0F);
        float modelScale = model.getItemCameraTransforms().getTransform(GROUND).scale.y;
        if (itemPhysics.is17Mode() && !is3D) {
            GlStateManager.translate((float) x, (float) y + bob + 0.05F * modelScale + 0.2F, (float) z);
        } else if (itemPhysics.isPhysicsMode()) {
            GlStateManager.translate((float) x, (float) y + bob + 0.05F * modelScale, (float) z);
        } else {
            GlStateManager.translate((float) x, (float) y + bob + 0.25F * modelScale, (float) z);
        }
        if (is3D || this.renderManager.options != null) {
            float spin = ((entity.getAge() + partialTicks) / 20.0F + entity.hoverStart) * 57.295776F;

            if (!(itemPhysics.is17Mode() && !is3D && !fancy)) {
                if (itemPhysics.isPhysicsMode()) {
                    if (entity.onGround) {

                        GL11.glRotatef(entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                        GL11.glRotatef(entity.rotationPitch + 90.0F, 1.0F, 0.0F, 0.0F);
                    } else {
                        for (int a = 0; a < 10; ++a) {
                            GL11.glRotatef(spin, 0.7F, 0.7F, 0.0F);
                        }
                    }
                } else {
                    GlStateManager.rotate(spin, 0.0F, 1.0F, 0.0F);
                }
            }
        }
        if (!is3D) {
            float step = itemPhysics.is17Mode()
                    ? (fancy ? -0.02109375F * count + 0.0421875F : 0.0F)
                    : -0.046875F * (count - 1) * 0.5F;
            GlStateManager.translate(-0.0F * (count - 1) * 0.5F, -0.0F * (count - 1) * 0.5F, step);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        return count;
    }

    @Unique
    private void renderOverlay(ItemPhysics itemPhysics, EntityItem entity, double x, double y, double z, float partialTicks) {
        ItemStack stack = entity.getEntityItem();
        if (stack == null || stack.getItem() == null) {
            return;
        }
        boolean drawCount = itemPhysics.isCountEnabled() && stack.stackSize > 1;
        boolean drawDurability = itemPhysics.isDurabilityEnabled() && stack.isItemStackDamageable() && stack.getItemDamage() > 0;
        if (!drawCount && !drawDurability) {
            return;
        }
        FontRenderer fontRenderer = this.getFontRendererFromRenderManager();
        if (fontRenderer == null) {
            return;
        }
        float bob = MathHelper.sin((entity.getAge() + partialTicks) / 10.0F + entity.hoverStart) * 0.1F + 0.1F;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + bob + 0.45F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.02F, -0.02F, 0.02F);
        GlStateManager.translate(-8.0F, 0.0F, 0.0F);
        
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        if (drawDurability) {
            int width = (int) Math.round(13.0D - (double) stack.getItemDamage() * 13.0D / (double) stack.getMaxDamage());
            int green = (int) Math.round(255.0D - (double) stack.getItemDamage() * 255.0D / (double) stack.getMaxDamage());
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();
            this.drawOverlayQuad(2, 13, 13, 2, 0, 0, 0, 255);
            this.drawOverlayQuad(2, 13, 12, 1, (255 - green) / 4, 64, 0, 255);
            this.drawOverlayQuad(2, 13, width, 1, 255 - green, green, 0, 255);
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
        }
        if (drawCount) {
            String text = String.valueOf(stack.stackSize);
            GlStateManager.disableBlend();
            fontRenderer.drawStringWithShadow(text, 17.0F - (float) fontRenderer.getStringWidth(text), 9.0F, 16777215);
            GlStateManager.enableBlend();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    @Unique
    private void drawOverlayQuad(int x, int y, int width, int height, int red, int green, int blue, int alpha) {
        if (width <= 0 || height <= 0) {
            return;
        }
        WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(x, y, 0.0D).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x, y + height, 0.0D).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0D).color(red, green, blue, alpha).endVertex();
        worldRenderer.pos(x + width, y, 0.0D).color(red, green, blue, alpha).endVertex();
        Tessellator.getInstance().draw();
    }

    @Unique
    private float jitter(float amount) {
        return (this.field_177079_e.nextFloat() * 2.0F - 1.0F) * amount;
    }

    @Unique
    private boolean isFancy() {
        return this.renderManager.options == null || this.renderManager.options.fancyGraphics;
    }

    private ItemPhysics getModule() {
        if (Astor.moduleManager == null) {
            return null;
        }
        return (ItemPhysics) Astor.moduleManager.modules.get(ItemPhysics.class);
    }
}