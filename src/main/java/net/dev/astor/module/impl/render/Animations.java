package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.event.events.impl.player.UpdateEvent;
import net.dev.astor.event.events.impl.render.RenderItemEvent;
import net.dev.astor.event.types.EventType;
import net.dev.astor.mixin.render.IAccessorItemRenderer;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.ListProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

public class Animations extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int VANILLA = 0;

    private static final float HAND_ANCHOR_X = 0.56F;
    private static final float HAND_ANCHOR_Y = -0.52F;
    private static final float HAND_ANCHOR_Z = -0.71999997F;

    public final ModeProperty blockAnimation = new ModeProperty(
            "BlockAnimation", 1, new String[]{
            "None", "1.7", "Smooth", "Exhibition", "Stab", "Spin", "Sigma",
            "Wood", "Swong", "Chill", "Komorebi", "Rhys", "Allah"
    });
    public final ModeProperty swingAnimation = new ModeProperty(
            "SwingAnimation", VANILLA, new String[]{"None", "1.9+", "Smooth", "Punch", "Shove"});
    public final ModeProperty otherAnimation = new ModeProperty(
            "OtherAnimation", 1, new String[]{"None", "1.7"});
    public final BooleanProperty swingWhileDigging = new BooleanProperty("SwingWhileDigging", true);
    public final BooleanProperty clientSide = new BooleanProperty("ClientSide", true, this.swingWhileDigging::getValue);
    public final FloatProperty x = new FloatProperty("X", 0.0F, -1.0F, 1.0F, 2);
    public final FloatProperty y = new FloatProperty("Y", 0.0F, -1.0F, 1.0F, 2);
    public final FloatProperty z = new FloatProperty("Z", 0.0F, -1.0F, 1.0F, 2);

    public final FloatProperty xRotation = new FloatProperty("XRotation", 0.0F, -180.0F, 180.0F, 0);
    public final FloatProperty yRotation = new FloatProperty("YRotation", 0.0F, -180.0F, 180.0F, 0);
    public final FloatProperty zRotation = new FloatProperty("ZRotation", 0.0F, -180.0F, 180.0F, 0);

    public final FloatProperty size = new FloatProperty("Size", 1.0F, 0.1F, 3.0F, 2);
    public final FloatProperty swingSpeed = new FloatProperty("SwingSpeed", 0.0F, -200.0F, 50.0F, 0);

    public final FloatProperty aX = new FloatProperty("AX", 0.0F, -1.0F, 1.0F, 2);
    public final FloatProperty aY = new FloatProperty("AY", 0.0F, -1.0F, 1.0F, 2);
    public final FloatProperty aZ = new FloatProperty("AZ", 0.0F, -1.0F, 1.0F, 2);
    public final FloatProperty aXRotation = new FloatProperty("AXRotation", 0.0F, -180.0F, 180.0F, 0);
    public final FloatProperty aYRotation = new FloatProperty("AYRotation", 0.0F, -180.0F, 180.0F, 0);
    public final FloatProperty aZRotation = new FloatProperty("AZRotation", 0.0F, -180.0F, 180.0F, 0);
    public final FloatProperty aSize = new FloatProperty("ASize", 1.0F, 0.1F, 3.0F, 2);
    public final FloatProperty aSwingSpeed = new FloatProperty("ASwingSpeed", 0.0F, -200.0F, 50.0F, 0);

    public final ListProperty hand = new ListProperty(
            "Hand", null, this.x, this.y, this.z,
            this.xRotation, this.yRotation, this.zRotation, this.size, this.swingSpeed);

    public final ListProperty antiSwing = new ListProperty(
            "AntiSwing", null, this.aX, this.aY, this.aZ,
            this.aXRotation, this.aYRotation, this.aZRotation, this.aSize, this.aSwingSpeed);

    public final BooleanProperty noEquipAnimation = new BooleanProperty("NoEquipAnimation", false);

    public final BooleanProperty leftHand = new BooleanProperty("LeftHand", false);

    private int swing = 0;

    @Override
    public String getDescription() {
        return "Replaces the first-person block swing and hand animations.";
    }

    public Animations() {
        super("Animations", Category.RENDER, false);
    }

    public static Animations get() {
        if (Astor.moduleManager == null) {
            return null;
        }
        return (Animations) Astor.moduleManager.modules.get(Animations.class);
    }

    @EventTarget
    public void onSendPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.SEND
                && this.swingWhileDigging.getValue() && this.clientSide.getValue()
                && event.getPacket() instanceof C0APacketAnimation && mc.thePlayer.isUsingItem()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE && mc.thePlayer != null) {
            this.swing = mc.thePlayer.swingProgressInt == 1 ? 9 : Math.max(0, this.swing - 1);
        }
    }

    @EventTarget
    public void onRenderItem(RenderItemEvent event) {
        try {
            if (!this.isEnabled() || event.getItemToRender().getItem() instanceof ItemMap) {
                return;
            }
            EnumAction itemAction = event.getEnumAction();
            IAccessorItemRenderer itemRenderer = (IAccessorItemRenderer) mc.getItemRenderer();
            float animationProgression = event.getAnimationProgression();
            float swingProgress = event.getSwingProgress();
            float partialTicks = event.getPartialTicks();
            float convertedProgress = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);

            if (this.isAntiSwingActive(event.isUseItem(), itemAction)) {
                this.applyTransform(this.aX, this.aY, this.aZ,
                        this.aXRotation, this.aYRotation, this.aZRotation, this.aSize);
            } else {
                this.applyTransform(this.x, this.y, this.z,
                        this.xRotation, this.yRotation, this.zRotation, this.size);
            }

            if (event.isUseItem()) {
                
                if (itemAction == EnumAction.NONE) {
                    switch ((int) this.otherAnimation.getValue().longValue()) {
                        case 0:
                            itemRenderer.transformFirstPersonItem(animationProgression, 0.0F);
                            break;
                        case 1:
                            itemRenderer.transformFirstPersonItem(animationProgression, swingProgress);
                            break;
                        default:
                            break;
                    }
                } else if (itemAction == EnumAction.BLOCK) {
                    
                    switch ((int) this.blockAnimation.getValue().longValue()) {
                            case 0:
                                itemRenderer.transformFirstPersonItem(animationProgression, 0.0F);
                                itemRenderer.blockTransformation();
                                break;

                            case 1:
                                itemRenderer.transformFirstPersonItem(animationProgression, swingProgress);
                                itemRenderer.blockTransformation();
                                break;

                            case 2: {
                                itemRenderer.transformFirstPersonItem(animationProgression, 0.0F);
                                float bob = -convertedProgress * 2.0F;
                                GlStateManager.translate(0.0F, bob / 10.0F + 0.1F, 0.0F);
                                GlStateManager.rotate(bob * 10.0F, 0.0F, 1.0F, 0.0F);
                                GlStateManager.rotate(250.0F, 0.2F, 1.0F, -0.6F);
                                GlStateManager.rotate(-10.0F, 1.0F, 0.5F, 1.0F);
                                GlStateManager.rotate(-bob * 20.0F, 1.0F, 0.5F, 1.0F);
                                break;
                            }

                            case 3:
                                itemRenderer.transformFirstPersonItem(animationProgression / 2.0F, 0.0F);
                                GlStateManager.translate(0.0F, 0.3F, -0.0F);
                                GlStateManager.rotate(-convertedProgress * 31.0F, 1.0F, 0.0F, 2.0F);
                                GlStateManager.rotate(-convertedProgress * 33.0F, 1.5F, convertedProgress / 1.1F, 0.0F);
                                itemRenderer.blockTransformation();
                                break;

                            case 4: {
                                float spin = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
                                GlStateManager.translate(0.6F, 0.3F, -0.6F + -spin * 0.7F);
                                GlStateManager.rotate(6090.0F, 0.0F, 0.0F, 0.1F);
                                GlStateManager.rotate(6085.0F, 0.0F, 0.1F, 0.0F);
                                GlStateManager.rotate(6110.0F, 0.1F, 0.0F, 0.0F);
                                itemRenderer.transformFirstPersonItem(0.0F, 0.0F);
                                itemRenderer.blockTransformation();
                                break;
                            }

                            case 5:
                                itemRenderer.transformFirstPersonItem(animationProgression, 0.0F);
                                GlStateManager.translate(0.0F, 0.2F, -1.0F);
                                GlStateManager.rotate(-59.0F, -1.0F, 0.0F, 3.0F);
                                
                                GlStateManager.rotate(-(System.currentTimeMillis() / 2L % 360L), 1.0F, 0.0F, 0.0F);
                                GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                                break;

                            case 6:
                                itemRenderer.transformFirstPersonItem(animationProgression, 0.0F);
                                GlStateManager.translate(0.0F, 0.1F, 0.0F);
                                itemRenderer.blockTransformation();
                                GlStateManager.rotate(convertedProgress * 35.0F / 2.0F, 0.0F, 1.0F, 1.5F);
                                GlStateManager.rotate(-convertedProgress * 135.0F / 4.0F, 1.0F, 1.0F, 0.0F);
                                break;

                            case 7:
                                itemRenderer.transformFirstPersonItem(animationProgression / 2.0F, 0.0F);
                                GlStateManager.translate(0.0F, 0.3F, -0.0F);
                                GlStateManager.rotate(-convertedProgress * 30.0F, 1.0F, 0.0F, 2.0F);
                                GlStateManager.rotate(-convertedProgress * 44.0F, 1.5F, convertedProgress / 1.2F, 0.0F);
                                itemRenderer.blockTransformation();
                                break;

                            case 8:
                                itemRenderer.transformFirstPersonItem(animationProgression / 2.0F, swingProgress);
                                GlStateManager.rotate(convertedProgress * 30.0F / 2.0F, -convertedProgress, -0.0F, 9.0F);
                                GlStateManager.rotate(convertedProgress * 40.0F, 1.0F, -convertedProgress / 2.0F, -0.0F);
                                GlStateManager.translate(0.0F, 0.2F, 0.0F);
                                itemRenderer.blockTransformation();
                                break;

                            case 9:
                                itemRenderer.transformFirstPersonItem(-0.25F, 1.0F + convertedProgress / 10.0F);
                                GL11.glRotated(-convertedProgress * 25.0F, 1.0F, 0.0F, 0.0F);
                                itemRenderer.blockTransformation();
                                break;

                            case 10: {
                                
                                GlStateManager.translate(0.41F, -0.25F, -0.5555557F);
                                GlStateManager.translate(0.0F, 0.0F, 0.0F);
                                GlStateManager.rotate(35.0F, 0.0F, 1.5F, 0.0F);
                                
                                float racism = MathHelper.sin(swingProgress * swingProgress / 64 * (float) Math.PI);
                                GlStateManager.rotate(racism * -5.0F, 0.0F, 0.0F, 0.0F);
                                GlStateManager.rotate(convertedProgress * -12.0F, 0.0F, 0.0F, 1.0F);
                                GlStateManager.rotate(convertedProgress * -65.0F, 1.0F, 0.0F, 0.0F);
                                itemRenderer.blockTransformation();
                                break;
                            }

                            case 11:
                                itemRenderer.transformFirstPersonItem(animationProgression, swingProgress);
                                itemRenderer.blockTransformation();
                                GlStateManager.translate(-0.3F, -0.1F, -0.0F);
                                break;

                            default:
                                
                                break;
                        }
                } else if (itemAction == EnumAction.EAT || itemAction == EnumAction.DRINK) {
                    switch ((int) this.otherAnimation.getValue().longValue()) {
                        case 0:
                            this.performDrinking(mc.thePlayer.getHeldItem(), mc.thePlayer, partialTicks);
                            itemRenderer.transformFirstPersonItem(animationProgression, 0.0F);
                            break;
                        case 1:
                            this.performDrinking(mc.thePlayer.getHeldItem(), mc.thePlayer, partialTicks);
                            itemRenderer.transformFirstPersonItem(animationProgression, swingProgress);
                            break;
                        default:
                            break;
                    }
                } else if (itemAction == EnumAction.BOW) {
                    switch ((int) this.otherAnimation.getValue().longValue()) {
                        case 0:
                            itemRenderer.transformFirstPersonItem(animationProgression, 0.0F);
                            this.doBowTransformations(mc.thePlayer.getHeldItem(), partialTicks, mc.thePlayer);
                            break;
                        case 1:
                            itemRenderer.transformFirstPersonItem(animationProgression, swingProgress);
                            this.doBowTransformations(mc.thePlayer.getHeldItem(), partialTicks, mc.thePlayer);
                            break;
                        default:
                            break;
                    }
                }
                event.setCancelled(true);
            } else {
                switch ((int) this.swingAnimation.getValue().longValue()) {
                    case 0:
                        this.doItemUsedTransformations(swingProgress);
                        itemRenderer.transformFirstPersonItem(animationProgression, swingProgress);
                        break;

                    case 1:
                        this.doItemUsedTransformations(swingProgress);
                        itemRenderer.transformFirstPersonItem(animationProgression, swingProgress);
                        GlStateManager.translate(0.0F, -((this.swing - 1)
                                - (this.swing == 0 ? 0.0F : partialTicks)) / 5.0F, 0.0F);
                        break;

                    case 2:
                        itemRenderer.transformFirstPersonItem(animationProgression, swingProgress);
                        this.doItemUsedTransformations(animationProgression);
                        break;

                    case 3:
                        itemRenderer.transformFirstPersonItem(animationProgression, swingProgress);
                        this.doItemUsedTransformations(swingProgress);
                        break;

                    case 4:
                        itemRenderer.transformFirstPersonItem(animationProgression, animationProgression);
                        this.doItemUsedTransformations(swingProgress);
                        break;

                    default:
                        break;
                }
                event.setCancelled(true);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyTransform(FloatProperty translateX, FloatProperty translateY, FloatProperty translateZ,
                                FloatProperty rotateX, FloatProperty rotateY, FloatProperty rotateZ,
                                FloatProperty scale) {
        GlStateManager.translate(translateX.getValue(), translateY.getValue(), translateZ.getValue());
        this.rotate(rotateX, 1.0F, 0.0F, 0.0F);
        this.rotate(rotateY, 0.0F, 1.0F, 0.0F);
        this.rotate(rotateZ, 0.0F, 0.0F, 1.0F);
        float scaleValue = scale.getValue();
        if (scaleValue != 1.0F) {
            
            GlStateManager.translate(HAND_ANCHOR_X, HAND_ANCHOR_Y, HAND_ANCHOR_Z);
            GlStateManager.scale(scaleValue, scaleValue, scaleValue);
            GlStateManager.translate(-HAND_ANCHOR_X, -HAND_ANCHOR_Y, -HAND_ANCHOR_Z);
        }
    }

    private void rotate(FloatProperty property, float axisX, float axisY, float axisZ) {
        float value = property.getValue();
        if (value != 0.0F) {
            GlStateManager.rotate(value, axisX, axisY, axisZ);
        }
    }

    public boolean isAntiSwingActive(boolean itemInUse, EnumAction action) {
        return itemInUse && action == EnumAction.BLOCK;
    }

    public boolean isSwordBlocking() {
        if (mc.thePlayer == null) {
            return false;
        }
        
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && this.isAntiSwingActive(mc.thePlayer.getItemInUseCount() > 0, held.getItemUseAction());
    }

    public FloatProperty getActiveSwingSpeed() {
        return this.isSwordBlocking() ? this.aSwingSpeed : this.swingSpeed;
    }

    private void doItemUsedTransformations(float swingProgress) {
        float f = -0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F * 2.0F);
        float f2 = -0.2F * MathHelper.sin(swingProgress * 3.1415927F);
        GlStateManager.translate(f, f1, f2);
    }

    private void performDrinking(ItemStack itemToRender, AbstractClientPlayer player, float partialTicks) {
        if (itemToRender == null) {
            return;
        }
        float f = (float) player.getItemInUseCount() - partialTicks + 1.0F;
        float f1 = f / (float) itemToRender.getMaxItemUseDuration();
        float f2 = MathHelper.abs(MathHelper.cos(f / 4.0F * 3.1415927F) * 0.1F);
        if (f1 >= 0.8F) {
            f2 = 0.0F;
        }
        GlStateManager.translate(0.0F, f2, 0.0F);
        float f3 = 1.0F - (float) Math.pow(f1, 27.0);
        GlStateManager.translate(f3 * 0.6F, f3 * -0.5F, f3 * 0.0F);
        GlStateManager.rotate(f3 * 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f3 * 30.0F, 0.0F, 0.0F, 1.0F);
    }

    private void doBowTransformations(ItemStack itemToRender, float partialTicks, AbstractClientPlayer player) {
        GlStateManager.rotate(-18.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-12.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-8.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-0.9F, 0.2F, 0.0F);
        float f = (float) itemToRender.getMaxItemUseDuration()
                - ((float) player.getItemInUseCount() - partialTicks + 1.0F);
        float f1 = f / 20.0F;
        f1 = (f1 * f1 + f1 * 2.0F) / 3.0F;
        if (f1 > 1.0F) {
            f1 = 1.0F;
        }
        if (f1 > 0.1F) {
            float f2 = MathHelper.sin((f - 0.1F) * 1.3F);
            float f3 = f1 - 0.1F;
            float f4 = f2 * f3;
            GlStateManager.translate(f4 * 0.0F, f4 * 0.01F, f4 * 0.0F);
        }
        GlStateManager.translate(f1 * 0.0F, f1 * 0.0F, f1 * 0.1F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F + f1 * 0.2F);
    }

    public int scaleArmSwingAnimationEnd(int animationEnd) {
        return animationEnd * (int) ((-this.getActiveSwingSpeed().getValue() / 100.0F) + 1.0F);
    }
}

