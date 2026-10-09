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

/**
 * Replaces the first-person block and swing animations.
 *
 * <p>Ported from the Animations module in {@code run/src/main/java}. The block animation matrix has 13
 * entries and the swing animation 5; both are reproduced case for case, including the one mode that is
 * listed but has no case - see {@link #blockAnimation}.</p>
 */
public class Animations extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int VANILLA = 0;

    /**
     * Where {@code ItemRenderer.transformFirstPersonItem} puts the hand - the first thing it does is
     * translate there - so this is the point the item is centred on and therefore the point a size
     * change has to pivot around.
     *
     * <p>Scaling about the camera instead looks like it does nothing, which is why this exists.
     * transformFirstPersonItem scales the item by 0.4, so a uniform scale s applied around the camera
     * multiplies both the item's size and its distance from the eye by s - and a perspective
     * projection's screen size is size/distance, which comes out unchanged. The only thing that
     * moves is the amount of foreshortening, which on most items is not worth seeing. Scaling about
     * this anchor instead leaves the item where it already sits and grows it in place.</p>
     */
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

    /** One rotation per axis, so the hand can be turned in all three directions independently. */
    public final FloatProperty xRotation = new FloatProperty("XRotation", 0.0F, -180.0F, 180.0F, 0);
    public final FloatProperty yRotation = new FloatProperty("YRotation", 0.0F, -180.0F, 180.0F, 0);
    public final FloatProperty zRotation = new FloatProperty("ZRotation", 0.0F, -180.0F, 180.0F, 0);

    /** Uniform on all three axes, so an item cannot be squashed on one side only. */
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

    /**
     * The two transform groups. Both are declared after their children on purpose: an instance
     * initialiser runs in source order, so building the group reads fields that have to be assigned
     * already.
     *
     * <p>The children keep the names the loose properties used to have, so a config written before
     * the split loads straight into the hand group. The anti-swing group's names are prefixed
     * because {@link net.dev.astor.config.Config} keys a module's properties by name - two properties
     * both called "X" would share one entry and the second would overwrite the first on every save.
     * </p>
     */
    public final ListProperty hand = new ListProperty(
            "Hand", null, this.x, this.y, this.z,
            this.xRotation, this.yRotation, this.zRotation, this.size, this.swingSpeed);

    /** Only read while {@link #isSwordBlocking()}, so its values sit idle the rest of the time. */
    public final ListProperty antiSwing = new ListProperty(
            "AntiSwing", null, this.aX, this.aY, this.aZ,
            this.aXRotation, this.aYRotation, this.aZRotation, this.aSize, this.aSwingSpeed);

    /**
     * Drops the hotbar-swap rise: the item appears at its resting place straight away instead of
     * climbing up into it. Applied by zeroing the equip progress before the hand render reads it,
     * which is the same value transformFirstPersonItem translates down by.
     */
    public final BooleanProperty noEquipAnimation = new BooleanProperty("NoEquipAnimation", false);

    /**
     * Puts the hand on the other side of the screen. A mirror about the plane through the camera
     * and nothing else - no packet, no handedness change, no inventory slot touched - so it only
     * affects what is drawn.
     */
    public final BooleanProperty leftHand = new BooleanProperty("LeftHand", false);

    /**
     * Counts down from 9 once a swing starts. Read by the 1.9+ swing animation to hold the item back for
     * the length of the swing.
     */
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

    /**
     * Suppresses the swing packet while an item is in use, which is what keeps the held item showing its
     * visual 1.7 pose instead of the 1.8 one. Server-side only: the local swing state is set before the
     * packet is queued, so this changes what others see and nothing on this screen.
     */
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

    /**
     * Cancels the vanilla hand transform and emits the selected animation instead.
     *
     * <p>The outer catch is deliberate: a bad matrix means broken rendering, not an error worth a stack
     * trace on every frame, and the next frame recomputes from scratch anyway.</p>
     */
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

            // One group or the other, never both: while a sword is blocking the anti-swing group is
            // the one in force and the hand group's values are simply not read. Picking here, before
            // the branch below, is what keeps them from being added together - applying the hand
            // group up front and the anti-swing one inside the branch would give x=1 plus x=1.
            if (this.isAntiSwingActive(event.isUseItem(), itemAction)) {
                this.applyTransform(this.aX, this.aY, this.aZ,
                        this.aXRotation, this.aYRotation, this.aZRotation, this.aSize);
            } else {
                this.applyTransform(this.x, this.y, this.z,
                        this.xRotation, this.yRotation, this.zRotation, this.size);
            }

            if (event.isUseItem()) {
                // Compared rather than switched on: javac compiles a switch over an enum into a lookup of
                // a synthetic Animations$1.$SwitchMap$... field, and that field is compiler-generated so it
                // is in no mapping table and cannot be renamed for the runtime jar. The EnumAction
                // constants do map, which is what makes this form safe.
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
                    // No transform of its own here: the anti-swing group was already applied above
                    // and the hand group's values are deliberately not folded in on top of it.
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
                                // Deliberately not a float: the source passes the long straight in, and the
                                // division has to truncate the same way.
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
                                // No transformFirstPersonItem here, unlike every other mode in this
                                // branch. That is what the source does, so the mode positions the item
                                // off a bare block transform.
                                GlStateManager.translate(0.41F, -0.25F, -0.5555557F);
                                GlStateManager.translate(0.0F, 0.0F, 0.0F);
                                GlStateManager.rotate(35.0F, 0.0F, 1.5F, 0.0F);
                                // swingProgress * swingProgress / 64 divides two floats by an int, so the
                                // quotient is truncated before the multiply. Kept as written.
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
                                // "Allah" is the thirteenth entry but has no case, so selecting it emits no
                                // transform at all. Left that way rather than quietly drawing something.
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

    /**
     * Translate, turn, then scale - the order that puts the rotations and the scaling around the item
     * rather than around the camera. The rotations and the scale are skipped at their neutral values
     * so a default config leaves the matrix exactly as vanilla would have it.
     */
    private void applyTransform(FloatProperty translateX, FloatProperty translateY, FloatProperty translateZ,
                                FloatProperty rotateX, FloatProperty rotateY, FloatProperty rotateZ,
                                FloatProperty scale) {
        GlStateManager.translate(translateX.getValue(), translateY.getValue(), translateZ.getValue());
        this.rotate(rotateX, 1.0F, 0.0F, 0.0F);
        this.rotate(rotateY, 0.0F, 1.0F, 0.0F);
        this.rotate(rotateZ, 0.0F, 0.0F, 1.0F);
        float scaleValue = scale.getValue();
        if (scaleValue != 1.0F) {
            // About the anchor, not the camera - see HAND_ANCHOR. Scaling about the camera is a no-op
            // that looks like one: it grows the item and pushes it away by the same factor, and the
            // projected size is size/distance, so the two cancel.
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

    /**
     * Whether the anti-swing group is the one in force. A sword that is in use, which is the same
     * condition the block animations key off: in this build {@code EnumAction.BLOCK} is only ever
     * returned by a sword, and the fake-blocking in MixinEntityRenderer is what puts the item in use.
     *
     * <p>The two groups are alternatives, never layers. Exactly one set of values is read, so a
     * hand x of 1 and an anti-swing x of 1 give an x of 1 while blocking, not 2.</p>
     */
    public boolean isAntiSwingActive(boolean itemInUse, EnumAction action) {
        return itemInUse && action == EnumAction.BLOCK;
    }

    /** The same question asked against the live player, for the code that has no event to read. */
    public boolean isSwordBlocking() {
        if (mc.thePlayer == null) {
            return false;
        }
        // Null is reachable: this is also called from a hook on EntityLivingBase, so it runs for mobs
        // and on the integrated server's thread, where the ticked entity is not the player and has no
        // hand stack at all.
        ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && this.isAntiSwingActive(mc.thePlayer.getItemInUseCount() > 0, held.getItemUseAction());
    }

    /** The anti-swing group's speed while a sword is blocking, the hand group's otherwise. */
    public FloatProperty getActiveSwingSpeed() {
        return this.isSwordBlocking() ? this.aSwingSpeed : this.swingSpeed;
    }

    /**
     * @see net.minecraft.client.renderer.ItemRenderer#doItemUsedTransformations(float)
     */
    private void doItemUsedTransformations(float swingProgress) {
        float f = -0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F);
        float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927F * 2.0F);
        float f2 = -0.2F * MathHelper.sin(swingProgress * 3.1415927F);
        GlStateManager.translate(f, f1, f2);
    }

    /**
     * @see net.minecraft.client.renderer.ItemRenderer#performDrinking(AbstractClientPlayer, float)
     */
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

    /**
     * @see net.minecraft.client.renderer.ItemRenderer#doBowTransformations(float, AbstractClientPlayer)
     */
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

    /**
     * Applies the active group's SwingSpeed to the swing length. Vanilla's 6 ticks become
     * 6 * (1 - SwingSpeed/100), and the truncation is the source's: anything above 0 and up to 50
     * lands on a multiplier of 0, which makes the swing length 0 and the swing progress a division
     * by zero. Kept as written rather than clamped.
     */
    public int scaleArmSwingAnimationEnd(int animationEnd) {
        return animationEnd * (int) ((-this.getActiveSwingSpeed().getValue() / 100.0F) + 1.0F);
    }
}
