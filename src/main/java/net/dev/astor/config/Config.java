package net.dev.astor.config;

import com.google.gson.*;
import net.dev.astor.Astor;
import net.dev.astor.mixin.IAccessorMinecraft;
import net.dev.astor.module.Module;
import net.dev.astor.util.ChatUtil;
import net.dev.astor.property.Property;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Config {
    public static Minecraft mc = Minecraft.getMinecraft();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public String name;
    public File file;

    public static String lastConfig;

    public Config(String name, boolean newConfig) {
        this.name = name;
        lastConfig = name;
        if (name.equals("!") || name.equals("default")) {
            this.name = "default";
        }
        this.file = new File("./config/Astor/", String.format("%s.json", this.name));
        try {
            file.getParentFile().mkdirs();
            if (newConfig) {
                ((IAccessorMinecraft) mc).getLogger().info(String.format("Created: %s", this.file.getName()));
            }
        } catch (Exception e) {
            ((IAccessorMinecraft) mc).getLogger().error(e.getMessage());
        }
    }

    public void load() {
        try {

            if (!file.exists()) {
                ChatUtil.sendFormatted(String.format("%sConfig file not found (&c&o%s&r). Creating default config...&r", Astor.clientName, file.getName()));
                save();
                return;
            }

            JsonElement parsed = new JsonParser().parse(new BufferedReader(new FileReader(file)));
            if (parsed == null || !parsed.isJsonObject()) {
                ChatUtil.sendFormatted(String.format("%sInvalid config format (&c&o%s&r)&r", Astor.clientName, file.getName()));
                return;
            }

            JsonObject jsonObject = parsed.getAsJsonObject();
            for (Module module : Astor.moduleManager.modules.values()) {
                JsonElement moduleObj = jsonObject.get(module.getName());
                if (moduleObj != null && moduleObj.isJsonObject()) {
                    JsonObject object = moduleObj.getAsJsonObject();

                    ArrayList<Property<?>> list = Astor.propertyManager.properties.get(module.getClass());
                    if (list != null) {
                        JsonObject values = withNormalizedKeys(object, list);
                        for (Property<?> property : list) {
                            if (values.has(property.getName())) {
                                try {
                                    property.read(values);
                                } catch (Exception e) {
                                    ((IAccessorMinecraft) mc).getLogger().warn(String.format("Failed to load property %s for module %s", property.getName(), module.getName()));
                                }
                            }
                        }
                    }

                    if (object.has("toggled")) {
                        JsonElement toggled = object.get("toggled");
                        if (toggled != null && toggled.isJsonPrimitive()) {
                            module.setEnabled(toggled.getAsBoolean());
                        }
                    }

                    if (object.has("key")) {
                        JsonElement key = object.get("key");
                        if (key != null && key.isJsonPrimitive()) {
                            module.setKey(key.getAsInt());
                        }
                    }

                    if (object.has("hidden")) {
                        JsonElement hidden = object.get("hidden");
                        if (hidden != null && hidden.isJsonPrimitive()) {
                            module.setHidden(hidden.getAsBoolean());
                        }
                    }
                }
            }
            ChatUtil.sendFormatted(String.format("%sConfig has been loaded (&a&o%s&r)&r", Astor.clientName, file.getName()));
        } catch (FileNotFoundException e) {
            ChatUtil.sendFormatted(String.format("%sConfig file not found (&c&o%s&r)&r", Astor.clientName, file.getName()));
        } catch (JsonSyntaxException e) {
            ChatUtil.sendFormatted(String.format("%sConfig has invalid JSON syntax (&c&o%s&r)&r", Astor.clientName, file.getName()));
            ((IAccessorMinecraft) mc).getLogger().error("JSON Syntax Error: " + e.getMessage());
        } catch (Exception e) {
            ((IAccessorMinecraft) mc).getLogger().error("Error loading config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be loaded (&c&o%s&r)&r", Astor.clientName, file.getName()));
        }
    }

    /**
     * Property names used to be written in kebab case, so a config saved before the rename to
     * UpperCamelCase still has to be understood.
     */
    private static String legacyName(String name) {
        StringBuilder legacy = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (i > 0 && (Character.isUpperCase(character) || Character.isDigit(character))) {
                legacy.append('-');
            }
            legacy.append(Character.toLowerCase(character));
        }
        return legacy.toString();
    }

    private static JsonObject withNormalizedKeys(JsonObject object, List<Property<?>> list) {
        JsonObject normalized = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            normalized.add(entry.getKey(), entry.getValue());
        }
        for (Property<?> property : list) {
            String legacy = legacyName(property.getName());
            if (!legacy.equals(property.getName()) && normalized.has(legacy)) {
                normalized.add(property.getName(), normalized.remove(legacy));
            }
        }
        return normalized;
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            JsonObject object = new JsonObject();
            for (Module module : Astor.moduleManager.modules.values()) {
                JsonObject moduleObject = new JsonObject();
                moduleObject.addProperty("toggled", module.isEnabled());
                moduleObject.addProperty("key", module.getKey());
                moduleObject.addProperty("hidden", module.isHidden());

                ArrayList<Property<?>> list = Astor.propertyManager.properties.get(module.getClass());
                if (list != null) {
                    for (Property<?> property : list) {
                        try {
                            property.write(moduleObject);
                        } catch (Exception e) {
                            ((IAccessorMinecraft) mc).getLogger().warn(String.format("Failed to save property %s for module %s", property.getName(), module.getName()));
                        }
                    }
                }
                object.add(module.getName(), moduleObject);
            }

            PrintWriter printWriter = new PrintWriter(new FileWriter(file));
            printWriter.println(gson.toJson(object));
            printWriter.close();
            ChatUtil.sendFormatted(String.format("%sConfig has been saved (&a&o%s&r)&r", Astor.clientName, file.getName()));
        } catch (IOException e) {
            ((IAccessorMinecraft) mc).getLogger().error("Error saving config: " + e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be saved (&c&o%s&r)&r", Astor.clientName, file.getName()));
        }
    }
}
