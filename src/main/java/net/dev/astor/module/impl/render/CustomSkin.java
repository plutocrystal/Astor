package net.dev.astor.module.impl.render;

import net.dev.astor.mixin.client.IAccessorMinecraft;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.ButtonProperty;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.util.ChatUtil;
import net.dev.astor.util.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Replaces the skin on the local player, or on everyone when {@code AllPlayers} is on.
 *
 * <p>Skins come from two places: the ones shipped in the jar under {@code assets/astor/texture/skin},
 * and any dropped into the folder {@link #getCustomFolder()} points at. Both are read into the mode
 * list when the module is created, and {@code Load Skins} reads them again so a skin added while the
 * client was running can be picked up without a restart.</p>
 *
 * <p>Every skin is decoded into a {@link DynamicTexture} up front rather than being handed to the
 * renderer as a {@link ResourceLocation} to resolve per frame. {@code getLocationSkin} is called on
 * every render of every player, so anything that touches the disk there would be doing it sixty times
 * a second per entity.</p>
 */
public class CustomSkin extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String SKIN_FOLDER = "assets/astor/texture/skin";
    private static final String SKIN_EXTENSION = ".png";
    private static final String NONE = "None";

    public final ModeProperty skin = new ModeProperty("Skin", 0, new String[]{NONE});
    public final ModeProperty model = new ModeProperty("Model", 0, new String[]{"Steve", "Alex"});
    public final BooleanProperty allPlayers = new BooleanProperty("AllPlayers", false);
    public final ButtonProperty loadSkins = new ButtonProperty("Load Skins", this::loadSkins);
    public final ButtonProperty openFolder = new ButtonProperty("Open Folder", this::openFolder);

    private final Map<String, ResourceLocation> textures = new LinkedHashMap<>();
    private final List<String> names = new ArrayList<>();

    @Override
    public String getDescription() {
        return "Replaces the skin on the local player, or on everyone when AllPlayers is on.";
    }

    public CustomSkin() {
        super("CustomSkin", Category.RENDER, false);
        this.loadSkins();
    }

    public File getCustomFolder() {
        return new File(mc.mcDataDir, "astor" + File.separator + "skins");
    }

    private void openFolder() {
        File folder = this.getCustomFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            this.warn("Could not create " + folder.getPath());
            return;
        }
        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException | SecurityException | UnsupportedOperationException e) {
            this.warn("Could not open " + folder.getPath() + ", open it by hand");
        }
    }

    /**
     * Rebuilds the skin list, replacing the textures the previous run left registered.
     *
     * <p>The dynamic textures have to be released explicitly: they were handed out under generated
     * locations and nothing else holds a reference, so they would otherwise stay resident until the
     * client closed and eat into the texture budget for skins that are no longer selected.</p>
     */
    public void loadSkins() {
        for (ResourceLocation texture : this.textures.values()) {
            mc.renderEngine.deleteTexture(texture);
        }
        this.textures.clear();

        File folder = this.getCustomFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            this.warn("Could not create " + folder.getPath());
        }

        List<String> found = new ArrayList<>(skinsInAssets());
        found.addAll(skinsInFolder(folder));
        found.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> options = new ArrayList<>();
        options.add(NONE);
        Set<String> taken = new HashSet<>();
        taken.add(NONE.toLowerCase(Locale.ROOT));
        int skipped = 0;
        for (String name : found) {
            // A skin of the same name can arrive from both places, and the mode list is addressed by
            // name, so only the first of each survives.
            if (!taken.add(name.toLowerCase(Locale.ROOT))) {
                skipped++;
                continue;
            }
            BufferedImage image = readSkin(name, folder);
            if (image == null) {
                this.warn("Failed to load skin '&o" + name + "&r'");
                skipped++;
                continue;
            }
            this.textures.put(name, mc.renderEngine.getDynamicTextureLocation(name, new DynamicTexture(image)));
            options.add(name);
            ((IAccessorMinecraft) mc).getLogger().info(
                    String.format("Loaded skin %s (%dx%d)", name, image.getWidth(), image.getHeight()));
        }

        this.names.clear();
        this.names.addAll(options);
        this.skin.setModes(this.names.toArray(new String[0]));
        ((IAccessorMinecraft) mc).getLogger().info(
                String.format("Loaded %d skins, %d could not be used", options.size() - 1, skipped));
    }

    private List<String> skinsInAssets() {
        List<String> found = new ArrayList<>();
        for (String entry : ResourceUtil.list(SKIN_FOLDER)) {
            if (isSkin(entry) && !stripped(entry).isEmpty()) {
                found.add(stripped(entry));
            }
        }
        return found;
    }

    private List<String> skinsInFolder(File folder) {
        List<String> found = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) {
            return found;
        }
        Arrays.sort(files, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        for (File file : files) {
            if (file.isFile() && isSkin(file.getName()) && !stripped(file.getName()).isEmpty()) {
                found.add(stripped(file.getName()));
            }
        }
        return found;
    }

    private static boolean isSkin(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(SKIN_EXTENSION);
    }

    private static String stripped(String name) {
        return name.substring(0, name.length() - SKIN_EXTENSION.length());
    }

    /**
     * The bundled copy wins over the external one, so a broken drop-in file cannot make a skin that
     * ships with the client stop working.
     */
    private BufferedImage readSkin(String name, File folder) {
        InputStream assets = ResourceUtil.open(SKIN_FOLDER + "/" + name + SKIN_EXTENSION);
        if (assets != null) {
            try (InputStream image = assets) {
                return ImageIO.read(image);
            } catch (IOException e) {
                this.warn("Failed to load skin '&o" + name + "&r'");
                return null;
            }
        }
        File file = new File(folder, name + SKIN_EXTENSION);
        if (!file.isFile()) {
            return null;
        }
        try (InputStream image = new FileInputStream(file)) {
            return ImageIO.read(image);
        } catch (IOException e) {
            this.warn("Failed to load skin '&o" + name + "&r'");
            return null;
        }
    }

    private void warn(String message) {
        ((IAccessorMinecraft) mc).getLogger().warn(message);
        ChatUtil.sendFormatted("&c" + message);
    }

    /**
     * @return the texture for the selected skin, or null when None is selected or nothing is loaded
     */
    public ResourceLocation resolveSkin() {
        int index = this.skin.getValue();
        if (index <= 0 || index >= this.names.size()) {
            return null;
        }
        return this.textures.get(this.names.get(index));
    }

    /** Whether the selected skin should replace this player's own. */
    public boolean shouldApply(AbstractClientPlayer player) {
        if (this.skin.getValue() <= 0) {
            return false;
        }
        if (this.allPlayers.getValue()) {
            return true;
        }
        return player == mc.thePlayer;
    }

    /** The model the skin is laid out for, which is not derivable from the texture itself. */
    public String getSkinType() {
        return this.model.getValue() == 1 ? "slim" : "default";
    }

    @Override
    public String[] getSuffix() {
        String selected = this.skin.getModeString();
        return selected.isEmpty() || selected.equals(NONE) ? new String[0] : new String[]{selected};
    }
}
