package net.dev.astor.module.impl.render;

import net.dev.astor.mixin.client.IAccessorMinecraft;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.ButtonProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.util.ChatUtil;
import net.dev.astor.util.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.MathHelper;
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

public class CustomCape extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String CAPE_FOLDER = "assets/astor/texture/cape";
    private static final String CAPE_EXTENSION = ".png";
    private static final String NONE = "None";

    public final ModeProperty cape = new ModeProperty("Cape", 0, new String[]{NONE});
    public final ButtonProperty loadCapes = new ButtonProperty("Load Capes", this::loadCapes);
    public final ButtonProperty openFolder = new ButtonProperty("Open Folder", this::openFolder);

    private final Map<String, ResourceLocation> textures = new LinkedHashMap<>();
    private final List<String> names = new ArrayList<>();

    @Override
    public String getDescription() {
        return "Replaces your cape with one loaded from the game folder, and optionally applies it to other players.";
    }

    public CustomCape() {
        super("CustomCape", Category.RENDER, false);
        this.loadCapes();
    }

    public File getCustomFolder() {
        return new File(mc.mcDataDir, "astor" + File.separator + "capes");
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

    public void loadCapes() {
        for (ResourceLocation texture : this.textures.values()) {
            mc.renderEngine.deleteTexture(texture);
        }
        this.textures.clear();

        File folder = this.getCustomFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            this.warn("Could not create " + folder.getPath());
        }

        List<String> found = new ArrayList<>(capesInAssets());
        found.addAll(capesInFolder(folder));
        found.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> options = new ArrayList<>();
        options.add(NONE);
        Set<String> taken = new HashSet<>();
        taken.add(NONE.toLowerCase(Locale.ROOT));
        int skipped = 0;
        for (String name : found) {
            if (!taken.add(name.toLowerCase(Locale.ROOT))) {
                skipped++;
                continue;
            }
            BufferedImage image = readCape(name, folder);
            if (image == null) {
                this.warn("Failed to load cape '&o" + name + "&r'");
                skipped++;
                continue;
            }
            this.textures.put(name, mc.renderEngine.getDynamicTextureLocation(name, new DynamicTexture(image)));
            options.add(name);
            ((IAccessorMinecraft) mc).getLogger().info(
                    String.format("Loaded cape %s (%dx%d)", name, image.getWidth(), image.getHeight()));
        }

        this.names.clear();
        this.names.addAll(options);
        this.cape.setModes(this.names.toArray(new String[0]));
        ((IAccessorMinecraft) mc).getLogger().info(
                String.format("Loaded %d capes, %d could not be used", options.size() - 1, skipped));
    }

    private List<String> capesInAssets() {
        List<String> found = new ArrayList<>();
        for (String entry : ResourceUtil.list(CAPE_FOLDER)) {
            if (isCape(entry) && !stripped(entry).isEmpty()) {
                found.add(stripped(entry));
            }
        }
        return found;
    }

    private List<String> capesInFolder(File folder) {
        List<String> found = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) {
            return found;
        }
        Arrays.sort(files, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        for (File file : files) {
            if (file.isFile() && isCape(file.getName()) && !stripped(file.getName()).isEmpty()) {
                found.add(stripped(file.getName()));
            }
        }
        return found;
    }

    private static boolean isCape(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(CAPE_EXTENSION);
    }

    private static String stripped(String name) {
        return name.substring(0, name.length() - CAPE_EXTENSION.length());
    }

    private BufferedImage readCape(String name, File folder) {
        InputStream assets = ResourceUtil.open(CAPE_FOLDER + "/" + name + CAPE_EXTENSION);
        if (assets != null) {
            try (InputStream image = assets) {
                return ImageIO.read(image);
            } catch (IOException e) {
                this.warn("Failed to load cape '&o" + name + "&r'");
                return null;
            }
        }
        File file = new File(folder, name + CAPE_EXTENSION);
        if (!file.isFile()) {
            return null;
        }
        try (InputStream image = new FileInputStream(file)) {
            return ImageIO.read(image);
        } catch (IOException e) {
            this.warn("Failed to load cape '&o" + name + "&r'");
            return null;
        }
    }

    private void warn(String message) {
        ((IAccessorMinecraft) mc).getLogger().warn(message);
        ChatUtil.sendFormatted("&c" + message);
    }

    public boolean renderCape(AbstractClientPlayer player, RenderPlayer playerRenderer, float partialTicks) {
        int index = this.cape.getValue();
        if (index <= 0 || index >= this.names.size()) {
            return false;
        }
        ResourceLocation texture = this.textures.get(this.names.get(index));
        if (texture == null) {
            return false;
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        playerRenderer.bindTexture(texture);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 0.125F);

        double x = player.prevChasingPosX + (player.chasingPosX - player.prevChasingPosX) * partialTicks
                - (player.prevPosX + (player.posX - player.prevPosX) * partialTicks);
        double y = player.prevChasingPosY + (player.chasingPosY - player.prevChasingPosY) * partialTicks
                - (player.prevPosY + (player.posY - player.prevPosY) * partialTicks);
        double z = player.prevChasingPosZ + (player.chasingPosZ - player.prevChasingPosZ) * partialTicks
                - (player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks);

        float yaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        double sin = MathHelper.sin(yaw * 3.1415927F / 180.0F);
        double cos = -MathHelper.cos(yaw * 3.1415927F / 180.0F);

        float lean = (float) y * 10.0F;
        lean = MathHelper.clamp_float(lean, -6.0F, 32.0F);
        float swing = (float) (x * sin + z * cos) * 100.0F;
        float sway = (float) (x * cos - z * sin) * 100.0F;
        if (swing < 0.0F) {
            swing = 0.0F;
        }

        float cameraYaw = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * partialTicks;
        lean += MathHelper.sin((player.prevDistanceWalkedModified
                + (player.distanceWalkedModified - player.prevDistanceWalkedModified) * partialTicks) * 6.0F)
                * 32.0F * cameraYaw;
        if (player.isSneaking()) {
            lean += 25.0F;
        }

        GlStateManager.rotate(6.0F + swing / 2.0F + lean, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(sway / 2.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-sway / 2.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        playerRenderer.getMainModel().renderCape(0.0625F);
        GlStateManager.popMatrix();
        return true;
    }

    @Override
    public String[] getSuffix() {
        String selected = this.cape.getModeString();
        return selected.isEmpty() || selected.equals(NONE) ? new String[0] : new String[]{selected};
    }
}
