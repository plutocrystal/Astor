package net.dev.astor.mixin.render;

import net.dev.astor.module.impl.render.CustomModel;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelPig;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@SideOnly(Side.CLIENT)
@Mixin(value = {ModelPlayer.class}, priority = 9998)
public abstract class MixinModelPlayer extends ModelBiped {
    @Shadow
    public ModelRenderer bipedLeftArmwear;

    @Shadow
    public ModelRenderer bipedRightArmwear;

    @Shadow
    public ModelRenderer bipedLeftLegwear;

    @Shadow
    public ModelRenderer bipedRightLegwear;

    @Shadow
    public ModelRenderer bipedBodyWear;

    private ModelRenderer left_leg;
    private ModelRenderer right_leg;
    private ModelRenderer body;
    private ModelRenderer eye;
    private ModelRenderer rabbitBone;
    private ModelRenderer rabbitRleg;
    private ModelRenderer rabbitLarm;
    private ModelRenderer rabbitRarm;
    private ModelRenderer rabbitLleg;
    private ModelRenderer rabbitHead;
    private ModelRenderer fredhead;
    private ModelRenderer armLeft;
    private ModelRenderer legRight;
    private ModelRenderer legLeft;
    private ModelRenderer armRight;
    private ModelRenderer fredbody;
    private ModelRenderer armLeftpad2;
    private ModelRenderer torso;
    private ModelRenderer earRightpad_1;
    private ModelRenderer armRightpad2;
    private ModelRenderer legLeftpad;
    private ModelRenderer hat;
    private ModelRenderer legLeftpad2;
    private ModelRenderer armRight2;
    private ModelRenderer legRight2;
    private ModelRenderer earRightpad;
    private ModelRenderer armLeft2;
    private ModelRenderer frednose;
    private ModelRenderer earLeft;
    private ModelRenderer footRight;
    private ModelRenderer legRightpad2;
    private ModelRenderer legRightpad;
    private ModelRenderer armLeftpad;
    private ModelRenderer legLeft2;
    private ModelRenderer footLeft;
    private ModelRenderer hat2;
    private ModelRenderer armRightpad;
    private ModelRenderer earRight;
    private ModelRenderer crotch;
    private ModelRenderer jaw;
    private ModelRenderer handRight;
    private ModelRenderer handLeft;

    private ModelPig pig;

    @Inject(method = {"render"}, at = {@At("HEAD")}, cancellable = true)
    private void renderHook(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks,
                            float netHeadYaw, float headPitch, float scale, CallbackInfo callbackInfo) {
        CustomModel customModel = CustomModel.get();
        if (customModel == null || !customModel.isEnabled()) {
            return;
        }
        callbackInfo.cancel();
        this.renderCustom(customModel, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
    }

    private void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }

    private void renderCustom(CustomModel customModel, Entity entityIn, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (this.left_leg == null) {
            this.generatemodel();
        }

        GlStateManager.pushMatrix();
        switch (customModel.mode.getValue()) {
            case 1:
                this.renderRabbit(limbSwing, limbSwingAmount, scale);
                break;
            case 2:
                this.renderFreddy(scale);
                break;
            case 3:
                this.renderPig(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                break;
            default:
                this.renderImposter(limbSwing, limbSwingAmount, netHeadYaw, headPitch, scale);
                break;
        }
        GlStateManager.popMatrix();
    }

    private void renderPig(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                           float netHeadYaw, float headPitch, float scale) {
        if (this.pig == null) {
            this.pig = new ModelPig();
            
            this.pig.isChild = false;
        }
        this.pig.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
    }

    private void renderImposter(float limbSwing, float limbSwingAmount, float netHeadYaw, float headPitch, float scale) {
        final int bodyCustomColor = new Color(197, 16, 17).getRGB();
        final int eyeCustomColor = new Color(254, 254, 254).getRGB();
        final int legsCustomColor = new Color(122, 7, 56).getRGB();

        this.bipedHead.rotateAngleY = netHeadYaw * 0.017453292F;
        this.bipedHead.rotateAngleX = headPitch * 0.017453292F;
        this.bipedBody.rotateAngleY = 0.0F;
        final float f = 1.0F;
        this.right_leg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount / f;
        this.left_leg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + 3.1415927F) * 1.4F * limbSwingAmount / f;
        this.right_leg.rotateAngleY = 0.0F;
        this.left_leg.rotateAngleY = 0.0F;
        this.right_leg.rotateAngleZ = 0.0F;
        this.left_leg.rotateAngleZ = 0.0F;

        if (this.isChild) {
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
            this.body.render(scale);
            this.left_leg.render(scale);
            this.right_leg.render(scale);
        } else {
            GlStateManager.translate(0.0D, -0.8D, 0.0D);
            GlStateManager.scale(1.8D, 1.6D, 1.6D);
            GlStateManager.color((bodyCustomColor >> 16 & 0xFF) / 255.0F, (bodyCustomColor >> 8 & 0xFF) / 255.0F,
                    (bodyCustomColor & 0xFF) / 255.0F, 1.0F);
            GlStateManager.translate(0.0D, 0.15D, 0.0D);
            this.body.render(scale);
            GlStateManager.color((eyeCustomColor >> 16 & 0xFF) / 255.0F, (eyeCustomColor >> 8 & 0xFF) / 255.0F,
                    (eyeCustomColor & 0xFF) / 255.0F, 1.0F);
            this.eye.render(scale);
            GlStateManager.color((legsCustomColor >> 16 & 0xFF) / 255.0F, (legsCustomColor >> 8 & 0xFF) / 255.0F,
                    (legsCustomColor & 0xFF) / 255.0F, 1.0F);
            GlStateManager.translate(0.0D, -0.15D, 0.0D);
            this.left_leg.render(scale);
            this.right_leg.render(scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void renderRabbit(float limbSwing, float limbSwingAmount, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.25D, 1.25D, 1.25D);
        GlStateManager.translate(0.0D, -0.3D, 0.0D);
        this.rabbitHead.rotateAngleX = this.bipedHead.rotateAngleX;
        this.rabbitHead.rotateAngleY = this.bipedHead.rotateAngleY;
        this.rabbitHead.rotateAngleZ = this.bipedHead.rotateAngleZ;
        this.rabbitLarm.rotateAngleX = this.bipedLeftArm.rotateAngleX;
        this.rabbitLarm.rotateAngleY = this.bipedLeftArm.rotateAngleY;
        this.rabbitLarm.rotateAngleZ = this.bipedLeftArm.rotateAngleZ;
        this.rabbitRarm.rotateAngleX = this.bipedRightArm.rotateAngleX;
        this.rabbitRarm.rotateAngleY = this.bipedRightArm.rotateAngleY;
        this.rabbitRarm.rotateAngleZ = this.bipedRightArm.rotateAngleZ;
        this.rabbitRleg.rotateAngleX = this.bipedRightLeg.rotateAngleX;
        this.rabbitRleg.rotateAngleY = this.bipedRightLeg.rotateAngleY;
        this.rabbitRleg.rotateAngleZ = this.bipedRightLeg.rotateAngleZ;
        this.rabbitLleg.rotateAngleX = this.bipedLeftLeg.rotateAngleX;
        this.rabbitLleg.rotateAngleY = this.bipedLeftLeg.rotateAngleY;
        this.rabbitLleg.rotateAngleZ = this.bipedLeftLeg.rotateAngleZ;
        this.rabbitBone.render(scale);
        GlStateManager.popMatrix();
    }

    private void renderFreddy(float scale) {
        this.fredhead.rotateAngleX = this.bipedHead.rotateAngleX;
        this.fredhead.rotateAngleY = this.bipedHead.rotateAngleY;
        this.fredhead.rotateAngleZ = this.bipedHead.rotateAngleZ;
        this.armLeft.rotateAngleX = this.bipedLeftArm.rotateAngleX;
        this.armLeft.rotateAngleY = this.bipedLeftArm.rotateAngleY;
        this.armLeft.rotateAngleZ = this.bipedLeftArm.rotateAngleZ;
        this.legRight.rotateAngleX = this.bipedRightLeg.rotateAngleX;
        this.legRight.rotateAngleY = this.bipedRightLeg.rotateAngleY;
        this.legRight.rotateAngleZ = this.bipedRightLeg.rotateAngleZ;
        this.legLeft.rotateAngleX = this.bipedLeftLeg.rotateAngleX;
        this.legLeft.rotateAngleY = this.bipedLeftLeg.rotateAngleY;
        this.legLeft.rotateAngleZ = this.bipedLeftLeg.rotateAngleZ;
        this.armRight.rotateAngleX = this.bipedRightArm.rotateAngleX;
        this.armRight.rotateAngleY = this.bipedRightArm.rotateAngleY;
        this.armRight.rotateAngleZ = this.bipedRightArm.rotateAngleZ;
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.75, 0.65, 0.75);
        GlStateManager.translate(0.0, 0.85, 0.0);
        this.fredbody.render(scale);
        GlStateManager.popMatrix();
    }

    private void generatemodel() {
        this.body = new ModelRenderer(this);
        this.body.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.body.setTextureOffset(34, 8).addBox(-4.0F, 6.0F, -3.0F, 8, 12, 6);
        this.body.setTextureOffset(15, 10).addBox(-3.0F, 9.0F, 3.0F, 6, 8, 3);
        this.body.setTextureOffset(26, 0).addBox(-3.0F, 5.0F, -3.0F, 6, 1, 6);
        this.eye = new ModelRenderer(this);
        this.eye.setTextureOffset(0, 10).addBox(-3.0F, 7.0F, -4.0F, 6, 4, 1);
        this.left_leg = new ModelRenderer(this);
        this.left_leg.setRotationPoint(-2.0F, 18.0F, 0.0F);
        this.left_leg.setTextureOffset(0, 0).addBox(2.9F, 0.0F, -1.5F, 3, 6, 3, 0.0F);
        this.right_leg = new ModelRenderer(this);
        this.right_leg.setRotationPoint(2.0F, 18.0F, 0.0F);
        this.right_leg.setTextureOffset(13, 0).addBox(-5.9F, 0.0F, -1.5F, 3, 6, 3);
        this.rabbitBone = new ModelRenderer(this);
        this.rabbitBone.setRotationPoint(0.0F, 24.0F, 0.0F);
        this.rabbitBone.cubeList.add(new ModelBox(this.rabbitBone, 28, 45, -5.0F, -13.0F, -5.0F, 10, 11, 8, 0.0F, false));
        this.rabbitRleg = new ModelRenderer(this);
        this.rabbitRleg.setRotationPoint(-3.0F, -2.0F, -1.0F);
        this.rabbitBone.addChild(this.rabbitRleg);
        this.rabbitRleg.cubeList.add(new ModelBox(this.rabbitRleg, 0, 0, -2.0F, 0.0F, -2.0F, 4, 2, 4, 0.0F, false));
        this.rabbitLarm = new ModelRenderer(this);
        this.rabbitLarm.setRotationPoint(5.0F, -13.0F, -1.0F);
        this.setRotationAngle(this.rabbitLarm, 0.0F, 0.0F, -0.0873F);
        this.rabbitBone.addChild(this.rabbitLarm);
        this.rabbitLarm.cubeList.add(new ModelBox(this.rabbitLarm, 0, 0, 0.0F, 0.0F, -2.0F, 2, 8, 4, 0.0F, false));
        this.rabbitRarm = new ModelRenderer(this);
        this.rabbitRarm.setRotationPoint(-5.0F, -13.0F, -1.0F);
        this.setRotationAngle(this.rabbitRarm, 0.0F, 0.0F, 0.0873F);
        this.rabbitBone.addChild(this.rabbitRarm);
        this.rabbitRarm.cubeList.add(new ModelBox(this.rabbitRarm, 0, 0, -2.0F, 0.0F, -2.0F, 2, 8, 4, 0.0F, false));
        this.rabbitLleg = new ModelRenderer(this);
        this.rabbitLleg.setRotationPoint(3.0F, -2.0F, -1.0F);
        this.rabbitBone.addChild(this.rabbitLleg);
        this.rabbitLleg.cubeList.add(new ModelBox(this.rabbitLleg, 0, 0, -2.0F, 0.0F, -2.0F, 4, 2, 4, 0.0F, false));
        this.rabbitHead = new ModelRenderer(this);
        this.rabbitHead.setRotationPoint(0.0F, -14.0F, -1.0F);
        this.rabbitBone.addChild(this.rabbitHead);
        this.rabbitHead.cubeList.add(new ModelBox(this.rabbitHead, 0, 0, -3.0F, 0.0F, -4.0F, 6, 1, 6, 0.0F, false));
        this.rabbitHead.cubeList.add(new ModelBox(this.rabbitHead, 56, 0, -5.0F, -9.0F, -5.0F, 2, 3, 2, 0.0F, false));
        this.rabbitHead.cubeList.add(new ModelBox(this.rabbitHead, 56, 0, 3.0F, -9.0F, -5.0F, 2, 3, 2, 0.0F, true));
        this.rabbitHead.cubeList.add(new ModelBox(this.rabbitHead, 0, 45, -4.0F, -11.0F, -4.0F, 8, 11, 8, 0.0F, false));
        this.rabbitHead.cubeList.add(new ModelBox(this.rabbitHead, 46, 0, 1.0F, -20.0F, 0.0F, 3, 9, 1, 0.0F, false));
        this.rabbitHead.cubeList.add(new ModelBox(this.rabbitHead, 46, 0, -4.0F, -20.0F, 0.0F, 3, 9, 1, 0.0F, false));
        this.textureWidth = 100;
        this.textureHeight = 80;
        final ModelRenderer footRightLocal = new ModelRenderer(this, 22, 39);
        this.footRight = footRightLocal;
        footRightLocal.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.footRight.addBox(-2.5F, 0.0F, -6.0F, 5, 3, 8, 0.0F);
        this.setRotationAngle(this.footRight, -0.034906585F, 0.0F, 0.0F);
        final ModelRenderer earRightLocal = new ModelRenderer(this, 8, 0);
        this.earRight = earRightLocal;
        earRightLocal.setRotationPoint(-4.5F, -5.5F, 0.0F);
        this.earRight.addBox(-1.0F, -3.0F, -0.5F, 2, 3, 1, 0.0F);
        this.setRotationAngle(this.earRight, 0.05235988F, 0.0F, -1.0471976F);
        final ModelRenderer legLeftpadLocal = new ModelRenderer(this, 48, 39);
        this.legLeftpad = legLeftpadLocal;
        legLeftpadLocal.setRotationPoint(0.0F, 0.5F, 0.0F);
        this.legLeftpad.addBox(-3.0F, 0.0F, -3.0F, 6, 9, 6, 0.0F);
        final ModelRenderer earRightpad1 = new ModelRenderer(this, 40, 39);
        this.earRightpad_1 = earRightpad1;
        earRightpad1.setRotationPoint(0.0F, -1.0F, 0.0F);
        this.earRightpad_1.addBox(-2.0F, -5.0F, -1.0F, 4, 4, 2, 0.0F);
        final ModelRenderer legLeftLocal = new ModelRenderer(this, 54, 10);
        this.legLeft = legLeftLocal;
        legLeftLocal.setRotationPoint(3.3F, 12.5F, 0.0F);
        this.legLeft.addBox(-1.0F, 0.0F, -1.0F, 2, 10, 2, 0.0F);
        final ModelRenderer armRightpad2Local = new ModelRenderer(this, 0, 26);
        this.armRightpad2 = armRightpad2Local;
        armRightpad2Local.setRotationPoint(0.0F, 0.5F, 0.0F);
        this.armRightpad2.addBox(-2.5F, 0.0F, -2.5F, 5, 7, 5, 0.0F);
        final ModelRenderer handLeftLocal = new ModelRenderer(this, 58, 56);
        this.handLeft = handLeftLocal;
        handLeftLocal.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.handLeft.addBox(-2.0F, 0.0F, -2.5F, 4, 4, 5, 0.0F);
        this.setRotationAngle(this.handLeft, 0.0F, 0.0F, 0.05235988F);
        final ModelRenderer armLeftLocal = new ModelRenderer(this, 62, 10);
        this.armLeft = armLeftLocal;
        armLeftLocal.setRotationPoint(6.5F, -8.0F, 0.0F);
        this.armLeft.addBox(-1.0F, 0.0F, -1.0F, 2, 10, 2, 0.0F);
        this.setRotationAngle(this.armLeft, 0.0F, 0.0F, -0.2617994F);
        final ModelRenderer legRightLocal = new ModelRenderer(this, 90, 8);
        this.legRight = legRightLocal;
        legRightLocal.setRotationPoint(-3.3F, 12.5F, 0.0F);
        this.legRight.addBox(-1.0F, 0.0F, -1.0F, 2, 10, 2, 0.0F);
        final ModelRenderer armLeft2Local = new ModelRenderer(this, 90, 48);
        this.armLeft2 = armLeft2Local;
        armLeft2Local.setRotationPoint(0.0F, 9.6F, 0.0F);
        this.armLeft2.addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2, 0.0F);
        this.setRotationAngle(this.armLeft2, -0.17453292F, 0.0F, 0.0F);
        final ModelRenderer legRight2Local = new ModelRenderer(this, 20, 35);
        this.legRight2 = legRight2Local;
        legRight2Local.setRotationPoint(0.0F, 9.6F, 0.0F);
        this.legRight2.addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2, 0.0F);
        this.setRotationAngle(this.legRight2, 0.034906585F, 0.0F, 0.0F);
        final ModelRenderer armLeftpad2Local = new ModelRenderer(this, 0, 58);
        this.armLeftpad2 = armLeftpad2Local;
        armLeftpad2Local.setRotationPoint(0.0F, 0.5F, 0.0F);
        this.armLeftpad2.addBox(-2.5F, 0.0F, -2.5F, 5, 7, 5, 0.0F);
        final ModelRenderer legLeft2Local = new ModelRenderer(this, 72, 48);
        this.legLeft2 = legLeft2Local;
        legLeft2Local.setRotationPoint(0.0F, 9.6F, 0.0F);
        this.legLeft2.addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2, 0.0F);
        this.setRotationAngle(this.legLeft2, 0.034906585F, 0.0F, 0.0F);
        final ModelRenderer hatLocal = new ModelRenderer(this, 70, 24);
        this.hat = hatLocal;
        hatLocal.setRotationPoint(0.0F, -8.4F, 0.0F);
        this.hat.addBox(-3.0F, -0.5F, -3.0F, 6, 1, 6, 0.0F);
        this.setRotationAngle(this.hat, -0.017453292F, 0.0F, 0.0F);
        final ModelRenderer earRightpadLocal = new ModelRenderer(this, 85, 0);
        this.earRightpad = earRightpadLocal;
        earRightpadLocal.setRotationPoint(0.0F, -1.0F, 0.0F);
        this.earRightpad.addBox(-2.0F, -5.0F, -1.0F, 4, 4, 2, 0.0F);
        final ModelRenderer crotchLocal = new ModelRenderer(this, 56, 0);
        this.crotch = crotchLocal;
        crotchLocal.setRotationPoint(0.0F, 9.5F, 0.0F);
        this.crotch.addBox(-5.5F, 0.0F, -3.5F, 11, 3, 7, 0.0F);
        final ModelRenderer torsoLocal = new ModelRenderer(this, 8, 0);
        this.torso = torsoLocal;
        torsoLocal.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.torso.addBox(-6.0F, -9.0F, -4.0F, 12, 18, 8, 0.0F);
        this.setRotationAngle(this.torso, 0.017453292F, 0.0F, 0.0F);
        final ModelRenderer armRight2Local = new ModelRenderer(this, 90, 20);
        this.armRight2 = armRight2Local;
        armRight2Local.setRotationPoint(0.0F, 9.6F, 0.0F);
        this.armRight2.addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2, 0.0F);
        this.setRotationAngle(this.armRight2, -0.17453292F, 0.0F, 0.0F);
        final ModelRenderer handRightLocal = new ModelRenderer(this, 20, 26);
        this.handRight = handRightLocal;
        handRightLocal.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.handRight.addBox(-2.0F, 0.0F, -2.5F, 4, 4, 5, 0.0F);
        this.setRotationAngle(this.handRight, 0.0F, 0.0F, -0.05235988F);
        final ModelRenderer fredbodyLocal = new ModelRenderer(this, 0, 0);
        this.fredbody = fredbodyLocal;
        fredbodyLocal.setRotationPoint(0.0F, -9.0F, 0.0F);
        this.fredbody.addBox(-1.0F, -14.0F, -1.0F, 2, 24, 2, 0.0F);
        final ModelRenderer fredheadLocal = new ModelRenderer(this, 39, 22);
        this.fredhead = fredheadLocal;
        fredheadLocal.setRotationPoint(0.0F, -13.0F, -0.5F);
        this.fredhead.addBox(-5.5F, -8.0F, -4.5F, 11, 8, 9, 0.0F);
        final ModelRenderer legRightpadLocal = new ModelRenderer(this, 73, 33);
        this.legRightpad = legRightpadLocal;
        legRightpadLocal.setRotationPoint(0.0F, 0.5F, 0.0F);
        this.legRightpad.addBox(-3.0F, 0.0F, -3.0F, 6, 9, 6, 0.0F);
        final ModelRenderer frednoseLocal = new ModelRenderer(this, 17, 67);
        this.frednose = frednoseLocal;
        frednoseLocal.setRotationPoint(0.0F, -2.0F, -4.5F);
        this.frednose.addBox(-4.0F, -2.0F, -3.0F, 8, 4, 3, 0.0F);
        final ModelRenderer legLeftpad2Local = new ModelRenderer(this, 16, 50);
        this.legLeftpad2 = legLeftpad2Local;
        legLeftpad2Local.setRotationPoint(0.0F, 0.5F, 0.0F);
        this.legLeftpad2.addBox(-2.5F, 0.0F, -3.0F, 5, 7, 6, 0.0F);
        final ModelRenderer armRightpad3 = new ModelRenderer(this, 70, 10);
        this.armRightpad = armRightpad3;
        armRightpad3.setRotationPoint(0.0F, 0.5F, 0.0F);
        this.armRightpad.addBox(-2.5F, 0.0F, -2.5F, 5, 9, 5, 0.0F);
        final ModelRenderer armLeftpad3 = new ModelRenderer(this, 38, 54);
        this.armLeftpad = armLeftpad3;
        armLeftpad3.setRotationPoint(0.0F, 0.5F, 0.0F);
        this.armLeftpad.addBox(-2.5F, 0.0F, -2.5F, 5, 9, 5, 0.0F);
        final ModelRenderer hat2Local = new ModelRenderer(this, 78, 61);
        this.hat2 = hat2Local;
        hat2Local.setRotationPoint(0.0F, 0.1F, 0.0F);
        this.hat2.addBox(-2.0F, -4.0F, -2.0F, 4, 4, 4, 0.0F);
        this.setRotationAngle(this.hat2, -0.017453292F, 0.0F, 0.0F);
        final ModelRenderer legRightpad2Local = new ModelRenderer(this, 0, 39);
        this.legRightpad2 = legRightpad2Local;
        legRightpad2Local.setRotationPoint(0.0F, 0.5F, 0.0F);
        this.legRightpad2.addBox(-2.5F, 0.0F, -3.0F, 5, 7, 6, 0.0F);
        final ModelRenderer jawLocal = new ModelRenderer(this, 49, 65);
        this.jaw = jawLocal;
        jawLocal.setRotationPoint(0.0F, 0.5F, 0.0F);
        this.jaw.addBox(-5.0F, 0.0F, -4.5F, 10, 3, 9, 0.0F);
        this.setRotationAngle(this.jaw, 0.08726646F, 0.0F, 0.0F);
        final ModelRenderer armRight3 = new ModelRenderer(this, 48, 0);
        this.armRight = armRight3;
        armRight3.setRotationPoint(-6.5F, -8.0F, 0.0F);
        this.armRight.addBox(-1.0F, 0.0F, -1.0F, 2, 10, 2, 0.0F);
        this.setRotationAngle(this.armRight, 0.0F, 0.0F, 0.2617994F);
        final ModelRenderer footLeftLocal = new ModelRenderer(this, 72, 50);
        this.footLeft = footLeftLocal;
        footLeftLocal.setRotationPoint(0.0F, 8.0F, 0.0F);
        this.footLeft.addBox(-2.5F, 0.0F, -6.0F, 5, 3, 8, 0.0F);
        this.setRotationAngle(this.footLeft, -0.034906585F, 0.0F, 0.0F);
        final ModelRenderer earLeftLocal = new ModelRenderer(this, 40, 0);
        this.earLeft = earLeftLocal;
        earLeftLocal.setRotationPoint(4.5F, -5.5F, 0.0F);
        this.earLeft.addBox(-1.0F, -3.0F, -0.5F, 2, 3, 1, 0.0F);
        this.setRotationAngle(this.earLeft, 0.05235988F, 0.0F, 1.0471976F);
        this.legRight2.addChild(this.footRight);
        this.fredhead.addChild(this.earRight);
        this.legLeft.addChild(this.legLeftpad);
        this.earLeft.addChild(this.earRightpad_1);
        this.fredbody.addChild(this.legLeft);
        this.armRight2.addChild(this.armRightpad2);
        this.armLeft2.addChild(this.handLeft);
        this.fredbody.addChild(this.armLeft);
        this.fredbody.addChild(this.legRight);
        this.armLeft.addChild(this.armLeft2);
        this.legRight.addChild(this.legRight2);
        this.armLeft2.addChild(this.armLeftpad2);
        this.legLeft.addChild(this.legLeft2);
        this.fredhead.addChild(this.hat);
        this.earRight.addChild(this.earRightpad);
        this.fredbody.addChild(this.crotch);
        this.fredbody.addChild(this.torso);
        this.armRight.addChild(this.armRight2);
        this.armRight2.addChild(this.handRight);
        this.fredbody.addChild(this.fredhead);
        this.legRight.addChild(this.legRightpad);
        this.fredhead.addChild(this.frednose);
        this.legLeft2.addChild(this.legLeftpad2);
        this.armRight.addChild(this.armRightpad);
        this.armLeft.addChild(this.armLeftpad);
        this.hat.addChild(this.hat2);
        this.legRight2.addChild(this.legRightpad2);
        this.fredhead.addChild(this.jaw);
        this.fredbody.addChild(this.armRight);
        this.legLeft2.addChild(this.footLeft);
        this.fredhead.addChild(this.earLeft);
    }
}