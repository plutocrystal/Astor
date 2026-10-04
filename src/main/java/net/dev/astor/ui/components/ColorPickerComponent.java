package net.dev.astor.ui.components;

import net.dev.astor.property.properties.ColorProperty;
import net.dev.astor.ui.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

public class ColorPickerComponent implements Component {

    private static final int HEADER_HEIGHT = 12;
    private static final int PREVIEW_SIZE = 10;
    private static final int SQUARE_HEIGHT = 52;
    private static final int BAR_HEIGHT = 8;
    private static final int GAP = 2;

    private static final int OUTLINE = 0xFF101010;
    private static final int POINTER = 0xFFFFFFFF;
    private static final int POINTER_BORDER = 0xFF000000;

    private final ModuleComponent parentModule;
    private final ColorProperty property;
    private int offsetY;
    private boolean expanded;
    private boolean draggingSquare, draggingHue, draggingAlpha;
    private float hue, saturation, brightness;
    private int alpha;

    public ColorPickerComponent(ColorProperty property, ModuleComponent parentModule, int offsetY) {
        this.parentModule = parentModule;
        this.offsetY = offsetY;
        this.property = property;
        readFromProperty();
    }

    public void draw(AtomicInteger offset) {
        int x = parentModule.category.getX() + 4;
        int y = parentModule.category.getY() + offsetY;
        int width = parentModule.category.getWidth() - 8;

        if (!draggingSquare && !draggingHue && !draggingAlpha) {
            readFromProperty();
        }

        GlStateManager.enableBlend();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                property.getName().replace("-", " "), (float) (x * 2), (float) ((y + 4) * 2), -1);
        GL11.glPopMatrix();

        drawPreview(x + width - PREVIEW_SIZE, y + 1, PREVIEW_SIZE);

        if (expanded) {
            int squareTop = y + HEADER_HEIGHT;
            int hueTop = squareTop + SQUARE_HEIGHT + GAP;
            int alphaTop = hueTop + BAR_HEIGHT + GAP;

            drawSelectionArea(x, squareTop, width);
            drawHueBar(x, hueTop, width);
            drawAlphaBar(x, alphaTop, width);
        }

        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    private void drawPreview(int x, int y, int size) {
        fill(x, y, size, size, property.getValue());
        border(x, y, size, size);
    }

    private void drawSelectionArea(int x, int y, int width) {
        for (int i = 0; i < width; i++) {
            fill(x + i, y, 1, SQUARE_HEIGHT, Color.HSBtoRGB(0f, (float) i / (width - 1), 1f));
        }
        for (int j = 0; j < SQUARE_HEIGHT; j++) {
            int shade = Math.round((float) j / (SQUARE_HEIGHT - 1) * 255f) << 24;
            fill(x, y + j, width, 1, shade);
        }
        border(x, y, width, SQUARE_HEIGHT);

        int cursorX = x + (int) (saturation * (width - 1));
        int cursorY = y + (int) ((1f - brightness) * (SQUARE_HEIGHT - 1));
        fill(cursorX - 1, cursorY - 1, 3, 3, POINTER_BORDER);
        fill(cursorX, cursorY, 1, 1, POINTER);
    }

    private void drawHueBar(int x, int y, int width) {
        for (int i = 0; i < width; i++) {
            fill(x + i, y, 1, BAR_HEIGHT, Color.HSBtoRGB(hueAt(i, width), 1f, 1f));
        }
        drawBarCursor(x, y, hueColumn());
    }

    private void drawAlphaBar(int x, int y, int width) {
        int rgb = property.getValue() & 0x00FFFFFF;
        for (int i = 0; i < width; i++) {
            fill(x + i, y, 1, BAR_HEIGHT, alphaAt(i, width) << 24 | rgb);
        }
        drawBarCursor(x, y, alphaColumn());
    }

    private static float hueAt(int column, int width) {
        return (float) column / width;
    }

    private static int alphaAt(int column, int width) {
        return Math.round((float) column / (width - 1) * 255f);
    }

    private int hueColumn() {

        return Math.max(0, Math.min((int) Math.round(hue * barWidth()), barWidth() - 1));
    }

    private int alphaColumn() {
        return Math.round(alpha / 255f * (barWidth() - 1));
    }

    private int barWidth() {
        return parentModule.category.getWidth() - 8;
    }

    private static int columnAt(int mouseX, int start, int width) {
        double clamped = Math.min(width - 1, Math.max(0, mouseX - start));
        return (int) Math.round(clamped);
    }

    private void drawBarCursor(int x, int y, int column) {
        int cursorX = x + column;
        fill(cursorX - 1, y, 3, BAR_HEIGHT, POINTER_BORDER);
        fill(cursorX, y, 1, BAR_HEIGHT, POINTER);
    }

    private static void fill(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }

    private static void border(int x, int y, int width, int height) {
        fill(x, y, width, 1, OUTLINE);
        fill(x, y + height - 1, width, 1, OUTLINE);
        fill(x, y, 1, height, OUTLINE);
        fill(x + width - 1, y, 1, height, OUTLINE);
    }

    @Override
    public void update(int mouseX, int mouseY) {
        if (!draggingSquare && !draggingHue && !draggingAlpha) {
            return;
        }
        int x = parentModule.category.getX() + 4;
        int y = parentModule.category.getY() + offsetY;
        int width = parentModule.category.getWidth() - 8;

        if (draggingSquare) {
            saturation = ratio(mouseX, x, width);
            brightness = 1f - ratio(mouseY, y + HEADER_HEIGHT, SQUARE_HEIGHT);
        } else if (draggingHue) {
            hue = hueAt(columnAt(mouseX, x, width), width);
        } else if (draggingAlpha) {
            alpha = alphaAt(columnAt(mouseX, x, width), width);
        }

        property.setValue((Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF) | alpha << 24);
    }

    @Override
    public boolean mouseDown(int mouseX, int mouseY, int button) {
        if (button != 0 || !parentModule.panelExpand) return false;

        int x = parentModule.category.getX() + 4;
        int y = parentModule.category.getY() + offsetY;
        int width = parentModule.category.getWidth() - 8;

        if (isInside(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) {
            expanded = !expanded;
            return true;
        }
        if (!expanded) {
            return false;
        }

        int squareTop = y + HEADER_HEIGHT;
        int hueTop = squareTop + SQUARE_HEIGHT + GAP;
        int alphaTop = hueTop + BAR_HEIGHT + GAP;

        if (isInside(mouseX, mouseY, x, squareTop, width, SQUARE_HEIGHT)) {
            draggingSquare = true;
        } else if (isInside(mouseX, mouseY, x, hueTop, width, BAR_HEIGHT)) {
            draggingHue = true;
        } else if (isInside(mouseX, mouseY, x, alphaTop, width, BAR_HEIGHT)) {
            draggingAlpha = true;
        } else {
            return false;
        }
        update(mouseX, mouseY);
        return true;
    }

    @Override
    public void mouseReleased(int x, int y, int button) {
        draggingSquare = draggingHue = draggingAlpha = false;
    }

    @Override
    public boolean keyTyped(char chatTyped, int keyCode) {
        return false;
    }

    @Override
    public void setComponentStartAt(int newOffsetY) {
        offsetY = newOffsetY;
    }

    @Override
    public int getHeight() {

        return expanded ? HEADER_HEIGHT + SQUARE_HEIGHT + BAR_HEIGHT * 2 + GAP * 2 : HEADER_HEIGHT;
    }

    @Override
    public boolean isVisible() {
        return property.isVisible();
    }

    private void readFromProperty() {

        Color color = new Color(property.getValue(), true);
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = color.getAlpha();
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static float ratio(int mouse, int start, int length) {
        double clamped = Math.min(length, Math.max(0, mouse - start));
        return (float) roundToPrecision(clamped / length, 3);
    }

    private static double roundToPrecision(double v, int precision) {
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}