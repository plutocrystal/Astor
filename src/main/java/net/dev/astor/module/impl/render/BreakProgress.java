package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.event.events.impl.render.Render3DEvent;
import net.dev.astor.event.types.EventType;
import net.dev.astor.mixin.attack.IAccessorPlayerControllerMP;
import net.dev.astor.mixin.render.IAccessorRenderManager;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.misc.BedNuker;
import net.dev.astor.property.properties.ModeProperty;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import java.util.Locale;

public class BreakProgress extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String[] MODES = {"Percentage", "Second", "Decimal"};
    private static final int PERCENTAGE = 0;
    private static final int SECOND = 1;
    private static final double TEXT_SCALE = 0.02266667D;
    /**
     * 剩余 tick 数换算成显示用的秒数。原版这里是 20.0（tick/秒），数值偏大，
     * 除以 10 之后才对应真实世界时间（例如 35 -> 3.5）。
     */
    private static final double TICKS_PER_SECOND = 10.0D;

    public final ModeProperty mode = new ModeProperty("Mode", 0, MODES);

    private double progress;
    private BlockPos block;
    private String progressStr = "";

    @Override
    public String getDescription() {
        return "Adds a progress indicator to blocks you are breaking.";
    }

    public BreakProgress() {
        super("BreakProgress", Category.RENDER, false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }

    private static BedNuker getBedNuker() {
        return (BedNuker) Astor.moduleManager.modules.get(BedNuker.class);
    }

    private static Block getBlock(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock();
    }

    private void resetVariables() {
        this.progress = 0.0D;
        this.block = null;
        this.progressStr = "";
    }

    /**
     * 每 tick 的挖掘进度，移植自 keystrokesmod 的 BlockUtils.getBlockHardness。
     * 返回 0 表示这个方块挖不动（硬度为负，例如基岩）。
     */
    private float getBreakSpeed() {
        Block block = BreakProgress.getBlock(this.block);
        float hardness = block.getBlockHardness(mc.theWorld, this.block);
        if (hardness < 0.0F) {
            return 0.0F;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        float boost = (block.getMaterial().isToolNotRequired() || (held != null && held.canHarvestBlock(block))) ? 30.0F : 100.0F;
        return this.getToolDigEfficiency(held, block) / hardness / boost;
    }

    private float getToolDigEfficiency(ItemStack itemStack, Block block) {
        float efficiency = itemStack == null ? 1.0F : itemStack.getItem().getStrVsBlock(itemStack, block);
        if (efficiency > 1.0F) {
            int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
            if (level > 0) {
                efficiency += level * level + 1;
            }
        }
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            efficiency *= 1.0F + (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2F;
        }
        if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
                case 0:
                    efficiency *= 0.3F;
                    break;
                case 1:
                    efficiency *= 0.09F;
                    break;
                case 2:
                    efficiency *= 0.0027F;
                    break;
                default:
                    efficiency *= 8.1E-4F;
            }
        }
        if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
            efficiency /= 5.0F;
        }
        if (!mc.thePlayer.onGround) {
            efficiency /= 5.0F;
        }
        return efficiency;
    }

    private void setProgress() {
        switch (this.mode.getValue()) {
            case PERCENTAGE:
                this.progressStr = (int) (100.0D * this.progress) + "%";
                break;
            case SECOND: {
                double speed = this.getBreakSpeed();
                double timeLeft = speed <= 0.0D ? 0.0D : (1.0D - this.progress) / speed / BreakProgress.TICKS_PER_SECOND;
                this.progressStr = timeLeft == 0.0D ? "0" : String.format(Locale.ROOT, "%.1fs", timeLeft);
                break;
            }
            default:
                this.progressStr = String.format(Locale.ROOT, "%.2f", this.progress);
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null || mc.playerController == null) {
            this.resetVariables();
            return;
        }
        if (mc.thePlayer.capabilities.isCreativeMode || !mc.thePlayer.capabilities.allowEdit) {
            this.resetVariables();
            return;
        }
        BedNuker bedNukerModule = BreakProgress.getBedNuker();
        if (bedNukerModule != null && bedNukerModule.isEnabled()) {
            BlockPos currentBlock = bedNukerModule.getTargetBed();
            if (currentBlock != null && !(BreakProgress.getBlock(currentBlock) instanceof BlockBed)) {
                this.progress = Math.min(1.0D, bedNukerModule.getBreakProgress());
                this.block = currentBlock;
                if (this.block == null) {
                    return;
                }
                this.setProgress();
                return;
            }
        }
        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectType.BLOCK) {
            this.resetVariables();
            return;
        }
        this.progress = ((IAccessorPlayerControllerMP) mc.playerController).getCurBlockDamageMP();
        if (this.progress == 0.0F) {
            this.resetVariables();
            return;
        }
        this.block = mc.objectMouseOver.getBlockPos();
        this.setProgress();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || this.block == null || this.progress == 0.0D
                || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        IAccessorRenderManager renderManager = (IAccessorRenderManager) mc.getRenderManager();
        double x = this.block.getX() + 0.5D - renderManager.getRenderPosX();
        double y = this.block.getY() + 0.5D - renderManager.getRenderPosY();
        double z = this.block.getZ() + 0.5D - renderManager.getRenderPosZ();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-BreakProgress.TEXT_SCALE, -BreakProgress.TEXT_SCALE, -BreakProgress.TEXT_SCALE);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        mc.fontRendererObj.drawString(this.progressStr, -mc.fontRendererObj.getStringWidth(this.progressStr) / 2.0F, -3.0F, -1, true);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    @Override
    public void onDisabled() {
        this.resetVariables();
    }
}
