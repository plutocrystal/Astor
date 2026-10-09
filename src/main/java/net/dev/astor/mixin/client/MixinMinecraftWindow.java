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

/**
 * Replaces the window title and the window/taskbar icons with the client's own.
 *
 * <p>Both vanilla call sites sit inside startGame() before {@code new Astor()} runs at its RETURN, so
 * {@link Astor#version} is still null at this point and the version has to be read from version.json
 * directly rather than off the Astor instance.</p>
 *
 * <p>Windows keeps three icon slots on the one window handle - ICON_SMALL (16x16, used by the taskbar
 * button and alt-tab), ICON_BIG (32x32, the title bar) and ICON_BIG2 (48x48, high DPI) - and LWJGL
 * picks the slot from each image's dimensions. The client's two sources are fed in at the sizes that
 * land them where they belong: the taskbar wants AstraWindows.png, the title bar wants Astra.png.</p>
 */
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
            // Anything went wrong reading or scaling the art, so leave the display alone rather than
            // cancel: vanilla will still install its own icons and the game still opens.
            return;
        }
        try {
            Display.setIcon(icons);
            callbackInfo.cancel();
        } catch (Throwable ignored) {
            // Same reasoning: a display that rejects the icon set should still start.
        }
    }

    /**
     * Ascending sizes, matching vanilla's own ordering: the 16x16 entry lands in ICON_SMALL, which is
     * what the taskbar button and alt-tab draw, and the 32x32 entry lands in ICON_BIG, the title bar.
     * Returns null unless both came out of a real image, since a partially built set would leave one
     * slot stale.
     */
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

    /**
     * Centre-crops to a square, then box-filters it down to the requested size. The taskbar art is
     * 500x550, and scaling that straight to 16x16 would squash it, so the middle square is taken first.
     *
     * <p>The resize is done pixel by pixel rather than through
     * {@code Graphics2D.drawImage(int,int,int,int,int,int,int,int)}. That overload drops the alpha
     * channel outright here - a 64x64 source with 2433 opaque pixels came out with none - and a
     * Graphics2D path is not something that can be verified on a headless box. Averaging the whole
     * source box that maps to each destination pixel also keeps a 500x500 logo from aliasing into
     * nothing at 16x16 the way nearest-neighbour sampling would.</p>
     */
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

    /**
     * Area average over one destination pixel's worth of source. Colour is weighted by alpha and the
     * alpha itself is averaged separately, otherwise fully transparent pixels - which still carry
     * whatever RGB was left in the file - bleed their colour into the edges and halo the result.
     */
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

    /**
     * Same packing as Minecraft.readImageToBuffer: getRGB hands back 0xAARRGGBB and the expression
     * rotates it into 0xRRGGBBAA, which a big-endian putInt lays down as R,G,B,A - the order LWJGL's
     * CreateIcon expects.
     */
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