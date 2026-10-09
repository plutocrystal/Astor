package net.dev.astor.module.impl.render;

import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.render.Render3DEvent;
import net.dev.astor.mixin.render.IAccessorRenderManager;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.ColorProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class BlockHighlight extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String[] MODES = {"Both", "Fill"};
    private static final int BOTH = 0;
    private static final int FILL = 1;

    private static final double VANILLA_PADDING = 0.002D;

    public final ModeProperty mode = new ModeProperty("Mode", 0, MODES);
    public final ColorProperty outlineColor = new ColorProperty("OutlineColor", 0xFFFFFFFF);
    public final ColorProperty fillColor = new ColorProperty("FillColor", 0x66FFFFFF);

    @Override
    public String getDescription() {
        return "Outlines the block you are looking at, in several styles.";
    }

    public BlockHighlight() {
        super("BlockHighlight", Category.RENDER, false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }

    private AxisAlignedBB getSelectedBox() {
        BlockPos pos = mc.objectMouseOver.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block.getMaterial() == Material.air || !mc.theWorld.getWorldBorder().contains(pos)) {
            return null;
        }
        block.setBlockBoundsBasedOnState(mc.theWorld, pos);
        return block.getSelectedBoundingBox(mc.theWorld, pos)
                .expand(VANILLA_PADDING, VANILLA_PADDING, VANILLA_PADDING)
                .offset(
                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
                );
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {

        if (!this.isEnabled() || mc.theWorld == null || mc.objectMouseOver == null
                || mc.objectMouseOver.typeOfHit != MovingObjectType.BLOCK) {
            return;
        }
        AxisAlignedBB box = this.getSelectedBox();
        if (box == null) {
            return;
        }
        Color fill = new Color(this.fillColor.getValue(), true);
        Color outline = new Color(this.outlineColor.getValue(), true);
        boolean fillMode = this.mode.getValue() == FILL;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.enableDepth();
        if (fillMode) {

            GlStateManager.disableDepth();
            GlStateManager.disableCull();
        } else {
            GlStateManager.enableCull();
        }
        GlStateManager.depthMask(false);
        GL11.glLineWidth(2.0F);
        RenderUtil.drawFilledBox(box, fill.getRed(), fill.getGreen(), fill.getBlue(), fill.getAlpha());
        RenderUtil.drawBoundingBox(box, outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha(), 2.0F);
        GlStateManager.depthMask(true);
        if (fillMode) {
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
    }
}
