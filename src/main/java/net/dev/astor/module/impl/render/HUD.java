package net.dev.astor.module.impl.render;

import net.dev.astor.module.Category;
import net.dev.astor.Astor;
import net.dev.astor.enums.BlinkModules;
import net.dev.astor.enums.ChatColors;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.render.Render2DEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.mixin.gui.IAccessorGuiChat;
import net.dev.astor.module.Module;
import net.dev.astor.util.ColorUtil;
import net.dev.astor.util.RenderUtil;
import net.dev.astor.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private List<Module> activeModules = new ArrayList<>();
    public final ModeProperty colorMode = new ModeProperty(
            "Color", 3, new String[]{"Rainbow", "Chroma", "Astolfo", "Custom1", "Custom12", "Custom123"}
    );
    public final FloatProperty colorSpeed = new FloatProperty("ColorSpeed", 1.0F, 0.5F, 1.5F);
    public final PercentProperty colorSaturation = new PercentProperty("ColorSaturation", 50);
    public final PercentProperty colorBrightness = new PercentProperty("ColorBrightness", 100);
    public final ColorProperty custom1 = new ColorProperty("CustomColor1", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 3 || this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom2 = new ColorProperty("CustomColor2", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom3 = new ColorProperty("CustomColor3", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 5);
    public final ModeProperty posX = new ModeProperty("PositionX", 0, new String[]{"Left", "Right"});
    public final ModeProperty posY = new ModeProperty("PositionY", 0, new String[]{"Top", "Bottom"});
    public final IntProperty offsetX = new IntProperty("OffsetX", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("OffsetY", 2, 0, 255);
    public final FloatProperty scale = new FloatProperty("Scale", 1.0F, 0.5F, 1.5F);
    public final PercentProperty background = new PercentProperty("Background", 25);
    public final BooleanProperty showBar = new BooleanProperty("Bar", true);
    public final BooleanProperty shadow = new BooleanProperty("Shadow", true);
    public final BooleanProperty suffixes = new BooleanProperty("Suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("LowerCase", false);
    public final BooleanProperty chatOutline = new BooleanProperty("ChatOutline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("BlinkTimer", true);

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }
        return moduleName;
    }

    private String[] getModuleSuffix(Module module) {
        String[] moduleSuffix = module.getSuffix();
        if (this.lowerCase.getValue()) {
            for (int i = 0; i < moduleSuffix.length; i++) {
                moduleSuffix[i] = moduleSuffix[i].toLowerCase();
            }
        }
        return moduleSuffix;
    }

    private int getModuleWidth(Module module) {
        return this.calculateStringWidth(
                this.getModuleName(module), this.getModuleSuffix(module)
        );
    }

    private int calculateStringWidth(String string, String[] arr) {
        int width = mc.fontRendererObj.getStringWidth(string);
        if (this.suffixes.getValue()) {
            for (String str : arr) {
                width += 3 + mc.fontRendererObj.getStringWidth(str);
            }
        }
        return width;
    }

    private float getColorCycle(long long3, long long4) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0));
        return 1.0F - (float) (Math.abs(long3 - long4 * 300L) % speed) / (float) speed;
    }

    @Override
    public String getDescription() {
        return "Draws the module list and its settings on screen.";
    }

    public HUD() {
        super("HUD", Category.RENDER, true, true);
    }

    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }

    public Color getColor(long time, long offset) {
        Color color = Color.white;
        switch (this.colorMode.getValue()) {
            case 0:
                color = ColorUtil.fromHSB(this.getColorCycle(time, offset), 1.0F, 1.0F);
                break;
            case 1:
                color = ColorUtil.fromHSB(this.getColorCycle(time / 3L, 0L), 1.0F, 1.0F);
                break;
            case 2:
                float cycle = this.getColorCycle(time, offset);
                if (cycle % 1.0F < 0.5F) {
                    cycle = 1.0F - cycle % 1.0F;
                }
                color = ColorUtil.fromHSB(cycle, 1.0F, 1.0F);
                break;
            case 3:
                color = new Color(this.custom1.getValue(), true);
                break;
            case 4:
                double cycle1 = this.getColorCycle(time, offset);
                color = ColorUtil.interpolate(
                        (float) (2.0 * Math.abs(cycle1 - Math.floor(cycle1 + 0.5))),
                        new Color(this.custom1.getValue(), true),
                        new Color(this.custom2.getValue(), true)
                );
                break;
            case 5:
                double cycle2 = this.getColorCycle(time, offset);
                float floor = (float) (2.0 * Math.abs(cycle2 - Math.floor(cycle2 + 0.5)));
                if (floor <= 0.5F) {
                    color = ColorUtil.interpolate(floor * 2.0F, new Color(this.custom1.getValue(), true), new Color(this.custom2.getValue(), true));
                } else {
                    color = ColorUtil.interpolate((floor - 0.5F) * 2.0F, new Color(this.custom2.getValue(), true), new Color(this.custom3.getValue(), true));
                }
        }
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(
                hsb[0],
                hsb[1] * (this.colorSaturation.getValue().floatValue() / 100.0F),
                hsb[2] * (this.colorBrightness.getValue().floatValue() / 100.0F)
        );
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.activeModules = Astor.moduleManager.modules.values().stream().filter(module -> module.isEnabled() && !module.isHidden()).sorted(Comparator.comparingInt(this::getModuleWidth).reversed()).collect(Collectors.<Module>toList());
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.chatOutline.getValue() && mc.currentScreen instanceof GuiChat) {
            String text = ((IAccessorGuiChat) mc.currentScreen).getInputField().getText().trim();
            if (Astor.commandManager != null && Astor.commandManager.isTypingCommand(text)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawOutlineRect(
                        2.0F,
                        (float) (mc.currentScreen.height - 14),
                        (float) (mc.currentScreen.width - 2),
                        (float) (mc.currentScreen.height - 2),
                        1.5F,
                        0,
                        this.getColor(System.currentTimeMillis()).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }
        if (this.isEnabled() && !mc.gameSettings.showDebugInfo) {
            float height = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0F;
            float x = (float) this.offsetX.getValue()
                    + (1.0F + (this.showBar.getValue() ? (this.shadow.getValue() ? 2.0F : 1.0F) : 0.0F)) * this.scale.getValue();
            float y = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();
            if (this.posX.getValue() == 1) {
                x = (float) new ScaledResolution(mc).getScaledWidth() - x;
            }
            if (this.posY.getValue() == 1) {
                y = (float) new ScaledResolution(mc).getScaledHeight() - y - height * this.scale.getValue();
            }
            GlStateManager.pushMatrix();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
            long l = System.currentTimeMillis();
            long offset = 0L;
            for (Module module : this.activeModules) {
                String moduleName = this.getModuleName(module);
                String[] moduleSuffix = this.getModuleSuffix(module);
                float totalWidth = (float) (this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0 : 1));
                int color = this.getColor(l, offset).getRGB();
                RenderUtil.enableRenderState();
                if (this.background.getValue() > 0) {
                    RenderUtil.drawRect(
                            x / this.scale.getValue() - 1.0F - (this.posX.getValue() == 0 ? 0.0F : totalWidth),
                            y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F)),
                            x / this.scale.getValue() + 1.0F + (this.posX.getValue() == 0 ? totalWidth : 0.0F),
                            y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (offset == 0L ? 1.0F : 0.0F)),
                            new Color(0.0F, 0.0F, 0.0F, this.background.getValue().floatValue() / 100.0F).getRGB()
                    );
                }
                if (this.showBar.getValue()) {
                    if (this.shadow.getValue()) {
                        RenderUtil.drawRect(
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -3.0F : 1.0F),
                                y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                                y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                                color
                        );
                        RenderUtil.drawRect(
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                                y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -1.0F : 3.0F),
                                y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                                (color & 16579836) >> 2 | color & 0xFF000000
                        );
                    } else {
                        RenderUtil.drawRect(
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 1.0F),
                                y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 0.0F),
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -1.0F : 2.0F),
                                y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? 0.0F : (offset == 0L ? 1.0F : 0.0F)),
                                color
                        );
                    }
                }
                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();
                if (this.shadow.getValue()) {
                    mc.fontRendererObj
                            .drawStringWithShadow(moduleName, x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F), y / this.scale.getValue(), color);
                } else {
                    mc.fontRendererObj
                            .drawString(
                                    moduleName,
                                    x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F),
                                    y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                    color,
                                    false
                            );
                }
                if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                    float width = (float) mc.fontRendererObj.getStringWidth(moduleName) + 3.0F;
                    for (String string : moduleSuffix) {
                        if (this.shadow.getValue()) {
                            mc.fontRendererObj
                                    .drawStringWithShadow(
                                            string,
                                            x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                            y / this.scale.getValue(),
                                            ChatColors.GRAY.toAwtColor()
                                    );
                        } else {
                            mc.fontRendererObj
                                    .drawString(
                                            string,
                                            x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                            y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                            ChatColors.GRAY.toAwtColor(),
                                            false
                                    );
                        }
                        width += (float) mc.fontRendererObj.getStringWidth(string) + (this.shadow.getValue() ? 3.0F : 2.0F);
                    }
                }
                y += (height + (this.shadow.getValue() ? 1.0F : 0.0F)) * this.scale.getValue() * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
                offset++;
            }
            if (this.blinkTimer.getValue()) {
                BlinkModules blinkingModule = Astor.blinkManager.getBlinkingModule();
                if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                    long movementPacketSize = Astor.blinkManager.countMovement();
                    if (movementPacketSize > 0L) {
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        mc.fontRendererObj
                                .drawString(
                                        String.valueOf(movementPacketSize),
                                        (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue()
                                                - (float) mc.fontRendererObj.getStringWidth(String.valueOf(movementPacketSize)) / 2.0F,
                                        (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(),
                                        this.getColor(l, offset).getRGB() & 16777215 | -1090519040,
                                        this.shadow.getValue()
                                );
                        GlStateManager.disableBlend();
                    }
                }
            }
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }
}
