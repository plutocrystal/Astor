package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.minecraft.item.ItemStack;

public class ItemPhysics extends Module {
    private static final String[] MODES = {"Default", "Physics", "1.7"};
    private static final int PHYSICS = 1;
    private static final int LEGACY = 2;

    public final ModeProperty mode = new ModeProperty("Mode", 0, MODES);
    public final FloatProperty scale = new FloatProperty("Size", 1.0F, 0.1F, 3.0F);
    public final BooleanProperty showCount = new BooleanProperty("Count", false);
    public final BooleanProperty showDurability = new BooleanProperty("Durability", false);

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