package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.ModeProperty;
import net.minecraft.util.ResourceLocation;

/**
 * Replaces the player model with an Among Us imposter, a rabbit or a Freddy.
 *
 * <p>Ported from NightX's CustomModel. The shapes themselves are built out of {@code ModelRenderer}
 * boxes in {@link net.dev.astor.mixin.render.MixinModelPlayer}, which cancels the vanilla render;
 * this module only carries the mode and the texture that goes with it.</p>
 *
 * <p>Upstream pulls its textures from a remote API at launch. They ship here instead - the three files
 * were fetched from that API and are the same artwork, so the client works offline and needs no HTTP
 * stack or certificate-trusting downloader.</p>
 */
public class CustomModel extends Module {
    private static final int IMPOSTER = 0;
    private static final int RABBIT = 1;
    private static final int FREDDY = 2;

    private static final ResourceLocation IMPOSTER_TEXTURE = new ResourceLocation("astor", "texture/models/imposter.png");
    private static final ResourceLocation RABBIT_TEXTURE = new ResourceLocation("astor", "texture/models/rabbit.png");
    private static final ResourceLocation FREDDY_TEXTURE = new ResourceLocation("astor", "texture/models/freddy.png");
    /**
     * Vanilla's own sheet, so the pig shape needs nothing shipped with this client. It is a plain
     * single-arg ResourceLocation rather than the custom-skin form: no domain means "minecraft", which
     * is where the game jar keeps it.
     */
    private static final ResourceLocation PIG_TEXTURE = new ResourceLocation("textures/entity/pig/pig.png");

    /** Appended rather than inserted, since ModeProperty persists by name and the existing order has to stand. */
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

    /** @return the texture the selected shape is drawn from */
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