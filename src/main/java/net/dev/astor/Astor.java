package net.dev.astor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dev.astor.command.CommandManager;
import net.dev.astor.command.commands.*;
import net.dev.astor.config.Config;
import net.dev.astor.event.EventManager;
import net.dev.astor.management.*;
import net.dev.astor.module.Module;
import net.dev.astor.module.ModuleManager;
import net.dev.astor.property.Property;
import net.dev.astor.property.PropertyManager;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class Astor {
    public static String clientName = "&7[&cA&6s&et&eo&ar&7]&r ";
    public static String version;
    public static RotationManager rotationManager;
    public static FloatManager floatManager;
    public static BlinkManager blinkManager;
    public static DelayManager delayManager;
    public static LagManager lagManager;
    public static PlayerStateManager playerStateManager;
    public static FriendManager friendManager;
    public static TargetManager targetManager;
    public static PropertyManager propertyManager;
    public static ModuleManager moduleManager;
    public static CommandManager commandManager;

    public Astor() {
        this.init();
    }

    public void init() {
        rotationManager = new RotationManager();
        floatManager = new FloatManager();
        blinkManager = new BlinkManager();
        delayManager = new DelayManager();
        lagManager = new LagManager();
        playerStateManager = new PlayerStateManager();
        friendManager = new FriendManager();
        targetManager = new TargetManager();
        propertyManager = new PropertyManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        EventManager.register(rotationManager);
        EventManager.register(floatManager);
        EventManager.register(blinkManager);
        EventManager.register(delayManager);
        EventManager.register(lagManager);
        EventManager.register(moduleManager);
        EventManager.register(commandManager);
        moduleManager.registerAll();
        commandManager.commands.add(new BindCommand());
        commandManager.commands.add(new ConfigCommand());
        commandManager.commands.add(new DenickCommand());
        commandManager.commands.add(new FriendCommand());
        commandManager.commands.add(new HelpCommand());
        commandManager.commands.add(new HideCommand());
        commandManager.commands.add(new IgnCommand());
        commandManager.commands.add(new ItemCommand());
        commandManager.commands.add(new ListCommand());
        commandManager.commands.add(new ModuleCommand());
        commandManager.commands.add(new PlayerCommand());
        commandManager.commands.add(new ShowCommand());
        commandManager.commands.add(new TargetCommand());
        commandManager.commands.add(new ToggleCommand());
        commandManager.commands.add(new VclipCommand());
        for (Module module : moduleManager.modules.values()) {
            // Walk the hierarchy from the base class down so Module's own properties (Hide) are
            // registered first and end up at the top of the module's row in the click gui.
            ArrayList<Class<?>> hierarchy = new ArrayList<>();
            for (Class<?> type = module.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                hierarchy.add(type);
            }
            Collections.reverse(hierarchy);
            ArrayList<Property<?>> properties = new ArrayList<>();
            for (Class<?> type : hierarchy) {
                for (final Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);
                    final Object obj;
                    try {
                        obj = field.get(module);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    if (obj instanceof Property<?> && !properties.contains(obj)) {
                        ((Property<?>) obj).setOwner(module);
                        properties.add((Property<?>) obj);
                    }
                }
            }
            propertyManager.properties.put(module.getClass(), properties);
            EventManager.register(module);
        }
        Config config = new Config("default", true);
        if (config.file.exists()) {
            config.load();
        }
        if (friendManager.file.exists()) {
            friendManager.load();
        }
        if (targetManager.file.exists()) {
            targetManager.load();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(config::save));

        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(Astor.class.getResourceAsStream("/version.json")), StandardCharsets.UTF_8)) {
            JsonObject modInfo = new JsonParser().parse(reader).getAsJsonObject();
            version = modInfo.get("version").getAsString();
        } catch (Exception e) {
            version = "dev";
        }
    }
}