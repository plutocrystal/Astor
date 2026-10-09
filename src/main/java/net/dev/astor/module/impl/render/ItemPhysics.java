package net.dev.astor.module.impl.render;

import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.render.Render3DEvent;
import net.dev.astor.mixin.render.IAccessorRenderManager;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

/**
 * Renders dropped item models nicer than vanilla does, and optionally labels them.
 *
 * <p>The {@code Item} option draws the stack's display name in world space above each dropped item.
 * Ported from 3arthh4ck's ESP module, where the equivalent setting is a plain boolean that draws
 * {@code EntityItem#getItem().getDisplayName()} as a nametag for every visible dropped item. Their
 * version leans on its own {@code Scale} setting for the text size and force-disables fancy graphics
 * plus gamma 100 around the loop; neither carries over usefully here - 1.8.9 display names do not
 * depend on fancy graphics, and toggling gamma mid-render risks leaking render state.</p>
 */
public class ItemPhysics extends Module {
    private static final String[] MODES = {"Default", "Physics", "1.7"};
    private static final int PHYSICS = 1;
    private static final int LEGACY = 2;
    /** Reference distance the nametag keeps when {@code NameAutoScale} is off, so text stays legible. */
    private static final double CONSTANT_SCALE_DISTANCE = 20.0;
    private static final double DISTANCE_SCALE_FACTOR = 0.0075;
    private static final double LABEL_HEIGHT = 0.25;

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, MODES);
    public final FloatProperty scale = new FloatProperty("Size", 1.0F, 0.1F, 3.0F);
    public final BooleanProperty showCount = new BooleanProperty("Count", true);
    public final BooleanProperty showDurability = new BooleanProperty("Durability", false);
    public final BooleanProperty item = new BooleanProperty("Item", false);
    public final BooleanProperty nameAutoScale =
            new BooleanProperty("NameAutoScale", false, this.item::getValue);
    // Hidden while auto scale is on, since that mode ignores this multiplier entirely.
    public final FloatProperty nameSize = new FloatProperty(
            "NameSize", 1.0F, 0.5F, 2.0F, () -> this.item.getValue() && !this.nameAutoScale.getValue());

    @Override
    public String getDescription() {
        return "Renders dropped items nicer than vanilla, and can label them with their name.";
    }

    public ItemPhysics() {
        super("ItemPhysics", Category.RENDER, false);
    }

    public boolean isPhysicsMode() {
        return this.isEnabled() && this.mode.getValue() == PHYSICS;
    }

    public boolean is17Mode() {
        return this.isEnabled() && this.mode.getValue() == LEGACY;
    }

    public float getScale() {
        return this.isEnabled() ? this.scale.getValue() : 1.0F;
    }

    public boolean isCountEnabled() {
        return this.isEnabled() && this.showCount.getValue();
    }

    public boolean isDurabilityEnabled() {
        return this.isEnabled() && this.showDurability.getValue();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || !this.item.getValue() || mc.theWorld == null || mc.getRenderViewEntity() == null) {
            return;
        }
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityItem) || entity.isDead) {
                continue;
            }
            if (!RenderUtil.isInViewFrustum(entity.getEntityBoundingBox(), 10.0)) {
                continue;
            }
            this.drawItemName((EntityItem) entity, event.getPartialTicks());
        }
    }

    /**
     * Draws the stack name as a camera-facing billboard. Render3DEvent fires after the world entities
     * have gone out, so the label lands on top of the item, and depth is off so it stays readable
     * through walls - the same behaviour the original ESP option had.
     */
    private void drawItemName(EntityItem itemEntity, float partialTicks) {
        ItemStack stack = itemEntity.getEntityItem();
        if (stack == null || stack.getItem() == null) {
            return;
        }
        String name = stack.getDisplayName();
        if (name == null || name.isEmpty()) {
            return;
        }
        int width = mc.fontRendererObj.getStringWidth(name);

        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        double x = RenderUtil.lerpDouble(itemEntity.posX, itemEntity.lastTickPosX, partialTicks) - renderManager.getRenderPosX();
        double y = RenderUtil.lerpDouble(itemEntity.posY, itemEntity.lastTickPosY, partialTicks) - renderManager.getRenderPosY() + LABEL_HEIGHT;
        double z = RenderUtil.lerpDouble(itemEntity.posZ, itemEntity.lastTickPosZ, partialTicks) - renderManager.getRenderPosZ();

        double scale = this.getNameScale(mc.getRenderViewEntity().getDistanceToEntity(itemEntity));

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        // Negated: the font texture is flipped, the same reason NameTags does it.
        GlStateManager.scale(-scale, -scale, 1.0);
        GlStateManager.disableDepth();
        mc.fontRendererObj.drawString(name, -width / 2.0F, -mc.fontRendererObj.FONT_HEIGHT, 0xFFFFFFFF, true);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private double getNameScale(double distance) {
        if (this.nameAutoScale.getValue()) {
            // Auto scale owns the size entirely - NameSize does not apply, it would fight the
            // distance curve and defeat the point of the option.
            return Math.pow(Math.min(Math.max(distance, 6.0), 128.0), 0.75) * DISTANCE_SCALE_FACTOR;
        }
        // Constant on-screen size, with NameSize as the multiplier.
        return Math.pow(CONSTANT_SCALE_DISTANCE, 0.75) * DISTANCE_SCALE_FACTOR * this.nameSize.getValue();
    }

    public static int get17BlockCount(ItemStack stack) {
        if (stack.stackSize > 40) {
            return 5;
        } else if (stack.stackSize > 20) {
            return 4;
        } else if (stack.stackSize > 5) {
            return 3;
        } else if (stack.stackSize > 1) {
            return 2;
        }
        return 1;
    }

    public static int get17FlatCount(ItemStack stack) {
        if (stack.stackSize < 2) {
            return 1;
        } else if (stack.stackSize < 16) {
            return 2;
        } else if (stack.stackSize < 32) {
            return 3;
        }
        return 4;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
