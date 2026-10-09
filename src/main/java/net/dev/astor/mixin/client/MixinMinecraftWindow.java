package net.dev.astor.mixin.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dev.astor.Astor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@SideOnly(Side.CLIENT)
@Mixin(value = {Minecraft.class}, priority = 9999)
public abstract class MixinMinecraftWindow {
    private static final String WINDOW_ICON = "/assets/astor/texture/pictures/Astor.png";
    private static final String TASKBAR_ICON = "/assets/astor/texture/pictures/AstorWindows.png";
    private static final int TITLE_BAR_SIZE = 32;
    private static final int TASKBAR_SIZE = 16;

    @Redirect(
            method = {"createDisplay"},
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V",
                    remap = false
            )
    )
    private void setWindowTitle(String vanillaTitle) {
        Display.setTitle("Astor " + readVersion());
    }

    @Inject(
            method = {"setWindowIcon"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void setWindowIcon(CallbackInfo callbackInfo) {
        ByteBuffer[] icons = buildIcons();
        if (icons == null) {
            
            return;
        }
        try {
            Display.setIcon(icons);
            callbackInfo.cancel();
        } catch (Throwable ignored) {
            
        }
    }

    private static ByteBuffer[] buildIcons() {
        BufferedImage taskbar = scale(load(TASKBAR_ICON), TASKBAR_SIZE);
        BufferedImage titleBar = scale(load(WINDOW_ICON), TITLE_BAR_SIZE);
        if (taskbar == null || titleBar == null) {
            return null;
        }
        return new ByteBuffer[]{toBuffer(taskbar), toBuffer(titleBar)};
    }

    private static BufferedImage load(String path) {
        try (InputStream stream = MixinMinecraftWindow.class.getResourceAsStream(path)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage scale(BufferedImage source, int size) {
        if (source == null) {
            return null;
        }
        int side = Math.min(source.getWidth(), source.getHeight());
        int originX = (source.getWidth() - side) / 2;
        int originY = (source.getHeight() - side) / 2;
        BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            int fromY = originY + y * side / size;
            int toY = originY + (y + 1) * side / size;
            for (int x = 0; x < size; x++) {
                int fromX = originX + x * side / size;
                int toX = originX + (x + 1) * side / size;
                target.setRGB(x, y, average(source, fromX, fromY, toX, toY));
            }
        }
        return target;
    }

    private static int average(BufferedImage source, int fromX, int fromY, int toX, int toY) {
        long alphaSum = 0L;
        long redSum = 0L;
        long greenSum = 0L;
        long blueSum = 0L;
        long weight = 0L;
        for (int y = fromY; y < toY; y++) {
            for (int x = fromX; x < toX; x++) {
                int argb = source.getRGB(x, y);
                int alpha = argb >>> 24 & 0xFF;
                alphaSum += alpha;
                redSum += (long) ((argb >> 16) & 0xFF) * alpha;
                greenSum += (long) ((argb >> 8) & 0xFF) * alpha;
                blueSum += (long) (argb & 0xFF) * alpha;
                weight += alpha;
            }
        }
        if (weight == 0L) {
            return 0;
        }
        int alphaOut = (int) (alphaSum / (((long) (toX - fromX)) * (toY - fromY)));
        int redOut = (int) (redSum / weight);
        int greenOut = (int) (greenSum / weight);
        int blueOut = (int) (blueSum / weight);
        return alphaOut << 24 | redOut << 16 | greenOut << 8 | blueOut;
    }

    private static ByteBuffer toBuffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        ByteBuffer buffer = ByteBuffer.allocate(4 * argb.length);
        for (int pixel : argb) {
            buffer.putInt(pixel << 8 | pixel >> 24 & 255);
        }
        buffer.flip();
        return buffer;
    }

    private static String readVersion() {
        try (InputStreamReader reader = new InputStreamReader(
                MixinMinecraftWindow.class.getResourceAsStream("/version.json"), StandardCharsets.UTF_8)) {
            JsonObject modInfo = new JsonParser().parse(reader).getAsJsonObject();
            return modInfo.get("version").getAsString();
        } catch (Exception e) {
            return "dev";
        }
    }
}