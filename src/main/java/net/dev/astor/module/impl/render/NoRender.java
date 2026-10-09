package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.minecraft.block.material.Material;

public class NoRender extends Module {

    public static final int SLOT_BOOTS = 1;
    public static final int SLOT_LEGGINGS = 2;
    public static final int SLOT_CHESTPLATE = 3;
    public static final int SLOT_HELMET = 4;

    public final BooleanProperty waterFog = new BooleanProperty("WaterFog", true);
    public final BooleanProperty lavaFog = new BooleanProperty("LavaFog", true);
    public final BooleanProperty pumpkin = new BooleanProperty("Pumpkin", true);
    public final BooleanProperty headBlock = new BooleanProperty("HeadBlock", true);
    public final BooleanProperty fire = new BooleanProperty("Fire", true);
    public final BooleanProperty helmet = new BooleanProperty("Helmet", true);
    public final BooleanProperty chestplate = new BooleanProperty("Chestplate", true);
    public final BooleanProperty leggings = new BooleanProperty("Leggings", true);
    public final BooleanProperty boots = new BooleanProperty("Boots", true);

    public final BooleanProperty viewBobbing = new BooleanProperty("ViewBobbing", false);

    @Override
    public String getDescription() {
        return "Hides assorted vanilla overlays: fog, fire, the pumpkin overlay and armour pieces.";
    }

    public NoRender() {
        super("NoRender", Category.RENDER, false);
    }

    public static NoRender get() {
        if (Astor.moduleManager == null) {
            return null;
        }
        NoRender noRender = (NoRender) Astor.moduleManager.modules.get(NoRender.class);
        return noRender != null && noRender.isEnabled() ? noRender : null;
    }

    public static boolean removesFluidFog(Material material) {
        NoRender noRender = get();
        if (noRender == null) {
            return false;
        }
        if (material == Material.water) {
            return noRender.waterFog.getValue();
        }
        if (material == Material.lava) {
            return noRender.lavaFog.getValue();
        }
        return false;
    }

    public static boolean hidesArmorSlot(int armorSlot) {
        NoRender noRender = get();
        if (noRender == null) {
            return false;
        }
        switch (armorSlot) {
            case SLOT_HELMET:
                return noRender.helmet.getValue();
            case SLOT_CHESTPLATE:
                return noRender.chestplate.getValue();
            case SLOT_LEGGINGS:
                return noRender.leggings.getValue();
            case SLOT_BOOTS:
                return noRender.boots.getValue();
            default:
                return false;
        }
    }
}

