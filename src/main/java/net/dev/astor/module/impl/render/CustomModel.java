package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.ModeProperty;
import net.minecraft.util.ResourceLocation;

public class CustomModel extends Module {
    private static final int IMPOSTER = 0;
    private static final int RABBIT = 1;
    private static final int FREDDY = 2;

    private static final ResourceLocation IMPOSTER_TEXTURE = new ResourceLocation("astor", "texture/models/imposter.png");
    private static final ResourceLocation RABBIT_TEXTURE = new ResourceLocation("astor", "texture/models/rabbit.png");
    private static final ResourceLocation FREDDY_TEXTURE = new ResourceLocation("astor", "texture/models/freddy.png");
    
    private static final ResourceLocation PIG_TEXTURE = new ResourceLocation("textures/entity/pig/pig.png");

    private static final int PIG = 3;

    public final ModeProperty mode = new ModeProperty("Mode", IMPOSTER,
            new String[]{"Imposter", "Rabbit", "Freddy", "Pig"});

    public CustomModel() {
        super("CustomModel", Category.RENDER, false);
    }

    @Override
    public String getDescription() {
        return "Replaces the player model with an Among Us imposter, a rabbit, a Freddy or a pig.";
    }

    public static CustomModel get() {
        if (Astor.moduleManager != null) {
            CustomModel customModel = (CustomModel) Astor.moduleManager.modules.get(CustomModel.class);
            if (customModel != null) {
                return customModel;
            }
        }
        return null;
    }

    public ResourceLocation getTexture() {
        switch (this.mode.getValue()) {
            case RABBIT:
                return RABBIT_TEXTURE;
            case FREDDY:
                return FREDDY_TEXTURE;
            case PIG:
                return PIG_TEXTURE;
            default:
                return IMPOSTER_TEXTURE;
        }
    }
}