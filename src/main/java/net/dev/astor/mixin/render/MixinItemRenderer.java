package net.dev.astor.mixin.render;

import net.dev.astor.event.EventManager;
import net.dev.astor.event.events.impl.render.RenderItemEvent;
import net.dev.astor.module.impl.render.Animations;
import net.dev.astor.module.impl.render.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {ItemRenderer.class}, priority = 9999)

@SuppressWarnings("deprecation")
public abstract class MixinItemRenderer {
    @Shadow
    private float prevEquippedProgress;

    @Shadow
    private float equippedProgress;

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    private ItemStack itemToRender;

    @Shadow
    private void rotateArroundXAndY(float angle, float angleY) {
        throw new AssertionError();
    }

    @Shadow
    private void setLightMapFromPlayer(AbstractClientPlayer clientPlayer) {
        throw new AssertionError();
    }

    @Shadow
    private void rotateWithPlayerRotations(EntityPlayerSP entityPlayerSPIn, float partialTicks) {
        throw new AssertionError();
    }

    @Shadow
    private void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress) {
        throw new AssertionError();
    }

    @Shadow
    private void transformFirstPersonItem(float equipProgress, float swingProgress) {
        throw new AssertionError();
    }

    @Shadow
    private void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks) {
        throw new AssertionError();
    }

    @Shadow
    private void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer) {
        throw new AssertionError();
    }

    @Shadow
    private void doItemUsedTransformations(float swingProgress) {
        throw new AssertionError();
    }

    @Shadow
    private void doBlockTransformations() {
        throw new AssertionError();
    }

    @Shadow
    private void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress) {
        throw new AssertionError();
    }

    @Shadow
    public abstract void renderItem(EntityLivingBase entityIn, ItemStack heldStack,
                                    net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType transform);

    @Inject(
            method = {"renderBlockInHand"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderBlockInHand(float partialTicks, TextureAtlasSprite atlas, CallbackInfo callbackInfo) {
        NoRender noRender = NoRender.get();
        if (noRender != null && noRender.headBlock.getValue()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"renderWaterOverlayTexture"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderWaterOverlayTexture(float partialTicks, CallbackInfo callbackInfo) {
        NoRender noRender = NoRender.get();
        if (noRender != null && noRender.waterFog.getValue()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"renderFireInFirstPerson"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderFireInFirstPerson(float partialTicks, CallbackInfo callbackInfo) {
        NoRender noRender = NoRender.get();
        if (noRender != null && noRender.fire.getValue()) {
            callbackInfo.cancel();
        }
    }

    @Overwrite
    public void renderItemInFirstPerson(float partialTicks) {
        float f = 1.0F - (this.prevEquippedProgress + (this.equippedProgress - this.prevEquippedProgress) * partialTicks);
        AbstractClientPlayer player = this.mc.thePlayer;

        Animations animations = Animations.get();
        boolean active = animations != null && animations.isEnabled();

        if (active && animations.noEquipAnimation.getValue()) {
            
            f = 0.0F;
        }

        float f1 = player.getSwingProgress(partialTicks);
        float f2 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float f3 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
        this.rotateArroundXAndY(f2, f3);
        this.setLightMapFromPlayer(player);
        this.rotateWithPlayerRotations((EntityPlayerSP) player, partialTicks);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();

        boolean leftHand = active && animations.leftHand.getValue();
        if (leftHand) {
            
            GlStateManager.scale(-1.0F, 1.0F, 1.0F);
            GL11.glFrontFace(GL11.GL_CW);
        }

        ItemStack stack = this.itemToRender;
        if (stack != null) {
            RenderItemEvent event = new RenderItemEvent(
                    stack.getItemUseAction(),
                    player.getItemInUseCount() > 0,
                    f,
                    partialTicks,
                    f1,
                    stack
            );
            EventManager.call(event);

            if (stack.getItem() instanceof ItemMap) {
                this.renderItemMap(player, f2, f, f1);
            } else if (event.isUseItem()) {
                if (!event.isCancelled()) {
                    
                    EnumAction action = event.getEnumAction();
                    if (action == EnumAction.NONE) {
                        this.transformFirstPersonItem(f, 0.0F);
                    } else if (action == EnumAction.EAT || action == EnumAction.DRINK) {
                        this.performDrinking(player, partialTicks);
                        this.transformFirstPersonItem(f, 0.0F);
                    } else if (action == EnumAction.BLOCK) {
                        this.transformFirstPersonItem(f, 0.0F);
                        this.doBlockTransformations();
                    } else if (action == EnumAction.BOW) {
                        this.transformFirstPersonItem(f, 0.0F);
                        this.doBowTransformations(partialTicks, player);
                    }
                }
            } else if (!event.isCancelled()) {
                this.doItemUsedTransformations(f1);
                this.transformFirstPersonItem(f, f1);
            }

            this.renderItem(player, stack, net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.FIRST_PERSON);
        } else if (!player.isInvisible()) {
            this.renderPlayerArm(player, f, f1);
        }

        if (leftHand) {
            GL11.glFrontFace(GL11.GL_CCW);
        }
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }
}

