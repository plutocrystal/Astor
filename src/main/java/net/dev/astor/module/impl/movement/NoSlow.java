package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.Astor;
import net.dev.astor.enums.FloatModules;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.Priority;
import net.dev.astor.event.events.impl.player.LivingUpdateEvent;
import net.dev.astor.event.events.impl.player.PlayerUpdateEvent;
import net.dev.astor.event.events.impl.input.RightClickMouseEvent;
import net.dev.astor.module.Module;
import net.dev.astor.util.BlockUtil;
import net.dev.astor.util.ItemUtil;
import net.dev.astor.util.PlayerUtil;
import net.dev.astor.util.TeamUtil;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.PercentProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.BlockPos;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int lastSlot = -1;
    public final ModeProperty swordMode = new ModeProperty("SwordMode", 1, new String[]{"None", "Vanilla"});
    public final PercentProperty swordMotion = new PercentProperty("SwordMotion", 100, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty swordSprint = new BooleanProperty("SwordSprint", true, () -> this.swordMode.getValue() != 0);
    public final ModeProperty foodMode = new ModeProperty("FoodMode", 0, new String[]{"None", "Vanilla", "Float"});
    public final PercentProperty foodMotion = new PercentProperty("FoodMotion", 100, () -> this.foodMode.getValue() != 0);
    public final BooleanProperty foodSprint = new BooleanProperty("FoodSprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeProperty bowMode = new ModeProperty("BowMode", 0, new String[]{"None", "Vanilla", "Float"});
    public final PercentProperty bowMotion = new PercentProperty("BowMotion", 100, () -> this.bowMode.getValue() != 0);
    public final BooleanProperty bowSprint = new BooleanProperty("BowSprint", true, () -> this.bowMode.getValue() != 0);

    public NoSlow() {
        super("NoSlow", Category.MOVEMENT, false);
    }

    public boolean isSwordActive() {
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isFloatMode() {
        return this.foodMode.getValue() == 2 && ItemUtil.isEating()
                || this.bowMode.getValue() == 2 && ItemUtil.isUsingBow();
    }

    public boolean isAnyActive() {
        return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return this.isSwordActive() && this.swordSprint.getValue()
                || this.isFoodActive() && this.foodSprint.getValue()
                || this.isBowActive() && this.bowSprint.getValue();
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) {
            return this.swordMotion.getValue();
        } else if (ItemUtil.isEating()) {
            return this.foodMotion.getValue();
        } else {
            return ItemUtil.isUsingBow() ? this.bowMotion.getValue() : 100;
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.isAnyActive()) {
            float multiplier = (float) this.getMotionMultiplier() / 100.0F;
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;
            if (!this.canSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled() && this.isFloatMode()) {
            int item = mc.thePlayer.inventory.currentItem;
            if (this.lastSlot != item && PlayerUtil.isUsingItem()) {
                this.lastSlot = item;
                Astor.floatManager.setFloatState(true, FloatModules.NO_SLOW);
            }
        } else {
            this.lastSlot = -1;
            Astor.floatManager.setFloatState(false, FloatModules.NO_SLOW);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.objectMouseOver != null) {
                switch (mc.objectMouseOver.typeOfHit) {
                    case BLOCK:
                        BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                        if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) {
                            return;
                        }
                        break;
                    case ENTITY:
                        Entity entityHit = mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) {
                            return;
                        }
                        if (entityHit instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase) entityHit)) {
                            return;
                        }
                }
            }
            if (this.isFloatMode() && !Astor.floatManager.isPredicted() && mc.thePlayer.onGround) {
                event.setCancelled(true);
                mc.thePlayer.motionY = 0.42F;
            }
        }
    }
}