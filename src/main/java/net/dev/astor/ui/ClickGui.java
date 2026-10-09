package net.dev.astor.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dev.astor.Astor;
import net.dev.astor.config.Config;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.ui.components.CategoryComponent;
import net.dev.astor.ui.components.ModuleComponent;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClickGui extends GuiScreen {
    private static final int SCROLL_STEP = 12;

    private static ClickGui instance;
    private final File configFile = new File(Config.CONFIG_DIR, "clickgui.txt");
    private final ArrayList<CategoryComponent> categoryList;
    private final ToolTip toolTip = new ToolTip();
    private int scroll = 0;
    private double animScroll = 0;

    public ClickGui() {
        instance = this;

        Map<Category, List<Module>> modulesByCategory = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            modulesByCategory.put(category, new ArrayList<>());
        }
        for (Module module : Astor.moduleManager.modules.values()) {
            List<Module> modules = modulesByCategory.get(module.getCategory());
            if (modules != null) {
                modules.add(module);
            }
        }

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());

        this.categoryList = new ArrayList<>();
        int topOffset = 5;

        for (Map.Entry<Category, List<Module>> entry : modulesByCategory.entrySet()) {
            List<Module> modules = entry.getValue();
            modules.sort(comparator);

            CategoryComponent component = new CategoryComponent(entry.getKey().getDisplayName(), modules);
            component.setY(topOffset);
            categoryList.add(component);
            topOffset += 20;
        }

        loadPositions();
    }

    public static ClickGui getInstance() {
        return instance;
    }

    public void initGui() {
        super.initGui();
        // Deliberately not resetting scroll/animScroll: the module reuses this screen instance
        // across close/reopen, so leaving them alone is what makes the gui come back where it was
        // left instead of jumping back to the top.
        applyScrollOffset((int) this.animScroll);
    }

    /**
     * Consumes the wheel, eases towards the new position and hands the result to every category.
     * There is no clamp on purpose - the gui scrolls freely in both directions, so it can never be
     * stuck at an edge that depends on the window size.
     * Must run before the categories are rendered or hit-tested.
     */
    private void pollScroll() {
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            this.scroll -= (wheel > 0 ? 1 : -1) * SCROLL_STEP;
        }
        this.animScroll += (this.scroll - this.animScroll) * 0.2;
        applyScrollOffset((int) this.animScroll);
    }

    private void applyScrollOffset(int offset) {
        for (CategoryComponent category : categoryList) {
            category.setScrollOffset(offset);
        }
    }

    public void drawScreen(int x, int y, float p) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());

        pollScroll();

        for (CategoryComponent category : categoryList) {
            category.render(this.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }

        // After everything else, so the note lands on top of the gui rather than under it.
        long now = System.currentTimeMillis();
        this.toolTip.update(hoveredModule(x, y), now);
        if (this.toolTip.isVisible(now)) {
            this.toolTip.draw(this.fontRendererObj, x, y, this.width, this.height);
        }
    }

    /**
     * The module the cursor is resting on, hit-tested against the whole row rather than the name drawn
     * in the middle of it.
     *
     * <p>Only opened categories are considered: a collapsed one draws no module rows, so its modules
     * are not under the cursor in any sense the user could act on. The hit test itself comes from
     * {@link ModuleComponent#isHovered}, which already accounts for the gui scroll.</p>
     *
     * <p>The last match wins rather than the first. Categories are freely draggable and can sit on top
     * of each other, and they are painted in list order, so a later one is drawn over an earlier one -
     * the topmost row under the cursor is the one the user is actually pointing at. Taking the first
     * would report the row hidden underneath.</p>
     *
     * @return the hovered module, or null when the cursor is over nothing
     */
    private Module hoveredModule(int x, int y) {
        Module hovered = null;
        for (CategoryComponent category : categoryList) {
            if (!category.isOpened()) {
                continue;
            }
            for (Component component : category.getModules()) {
                ModuleComponent module = (ModuleComponent) component;
                if (module.isHovered(x, y)) {
                    hovered = module.mod;
                }
            }
        }
        return hovered;
    }

    public void mouseClicked(int x, int y, int mouseButton) {
        if (handleActiveBinding(x, y, mouseButton)) {
            return;
        }
        for (int i = categoryList.size() - 1; i >= 0; i--) {
            CategoryComponent category = categoryList.get(i);
            if (category.handleHeaderClick(x, y, mouseButton)) {
                return;
            }
        }
        for (int i = categoryList.size() - 1; i >= 0; i--) {
            CategoryComponent category = categoryList.get(i);
            if (!category.isInsideBody(x, y)) {
                continue;
            }
            List<Component> modules = category.getModules();
            for (int j = modules.size() - 1; j >= 0; j--) {
                if (modules.get(j).mouseDown(x, y, mouseButton)) {
                    return;
                }
            }
            return;
        }
    }

    private boolean handleActiveBinding(int x, int y, int button) {
        for (CategoryComponent category : categoryList) {
            if (!category.isOpened()) continue;
            for (Component component : category.getModules()) {
                if (component.isBinding()) {
                    return component.mouseDown(x, y, button);
                }
            }
        }
        return false;
    }

    public void mouseReleased(int x, int y, int mouseButton) {
        for (CategoryComponent categoryComponent : categoryList) {
            categoryComponent.setDragging(false);
        }

        for (CategoryComponent categoryComponent : categoryList) {
            if (!categoryComponent.isOpened()) continue;
            for (Component component : categoryComponent.getModules()) {
                component.mouseReleased(x, y, mouseButton);
            }
        }
    }

    public void keyTyped(char typedChar, int key) {
        for (int i = categoryList.size() - 1; i >= 0; i--) {
            CategoryComponent category = categoryList.get(i);
            if (!category.isOpened()) continue;
            List<Component> modules = category.getModules();
            for (int j = modules.size() - 1; j >= 0; j--) {
                if (modules.get(j).keyTyped(typedChar, key)) {
                    return;
                }
            }
        }
        if (key == 1) {
            this.mc.displayGuiScreen(null);
        }
    }

    public void onGuiClosed() {
        // The screen instance is reused across close and reopen, so a countdown left running would
        // otherwise still be counting when it comes back up.
        this.toolTip.reset();
        savePositions();
        if (Astor.moduleManager == null) {
            return;
        }
        net.dev.astor.module.impl.render.ClickGui module = (net.dev.astor.module.impl.render.ClickGui) Astor.moduleManager.modules.get(net.dev.astor.module.impl.render.ClickGui.class);
        if (module != null && module.isEnabled()) {
            module.setEnabled(false);
        }
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void savePositions() {
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : categoryList) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getUnscolledY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            for (CategoryComponent cat : categoryList) {
                if (json.has(cat.getName())) {
                    JsonObject pos = json.getAsJsonObject(cat.getName());
                    cat.setX(pos.get("x").getAsInt());
                    cat.setY(pos.get("y").getAsInt());
                    cat.setOpened(pos.get("open").getAsBoolean());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}