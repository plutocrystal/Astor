package net.dev.astor.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.ui.components.CategoryComponent;
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
    private static ClickGui instance;
    private final File configFile = new File("./config/Astor/", "clickgui.txt");
    private final ArrayList<CategoryComponent> categoryList;

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
    }

    public void drawScreen(int x, int y, float p) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());

        mc.fontRendererObj.drawStringWithShadow("Astor " + Astor.version, 4, this.height - 3 - mc.fontRendererObj.FONT_HEIGHT * 2, new Color(60, 162, 253).getRGB());
        mc.fontRendererObj.drawStringWithShadow("dev, PlutoCrystal_", 4, this.height - 3 - mc.fontRendererObj.FONT_HEIGHT, new Color(60, 162, 253).getRGB());

        for (CategoryComponent category : categoryList) {
            category.render(this.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int scrollDir = wheel > 0 ? 1 : -1;
            for (CategoryComponent category : categoryList) {
                category.onScroll(x, y, scrollDir);
            }
        }
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

    /**
     * While a key is being bound every click belongs to that bind, otherwise the click could
     * toggle the module that happens to sit underneath.
     */
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
        savePositions();
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
            pos.addProperty("y", cat.getY());
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
