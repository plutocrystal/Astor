package net.dev.astor.module;

import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.input.KeyEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.module.impl.render.ClientSetting;
import net.dev.astor.module.impl.render.GuiModule;
import net.dev.astor.util.ChatUtil;
import net.dev.astor.util.SoundUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;

public class ModuleManager {

    private Boolean pendingSound = null;
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

    public void registerAll() {
        List<Class<? extends Module>> found = ModuleScanner.findModules();
        for (Class<? extends Module> clazz : found) {
            Module module = instantiate(clazz);
            Category category = ModuleScanner.categoryOf(clazz);
            if (category != null && category != module.getCategory()) {
                System.out.println(String.format("%s is filed under %s but declares category %s",
                        clazz.getSimpleName(), category, module.getCategory()));
            }
            this.modules.put(clazz, module);
        }
        if (this.modules.isEmpty()) {
            throw new RuntimeException("No modules found below " + ModuleScanner.IMPL_PACKAGE);
        }
        System.out.println(String.format("Registered %d modules", this.modules.size()));
    }

    private static Module instantiate(Class<? extends Module> clazz) {
        try {
            Constructor<? extends Module> constructor = clazz.getDeclaredConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) {
                throw new NoSuchMethodException(clazz.getName() + " requires a public no-arg constructor");
            }
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to register module " + clazz.getName(), e);
        }
    }

    public Module getModule(String string) {
        return this.modules.values().stream().filter(mD -> mD.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
    }

    public Module getModule(Class<?> clazz){
        return this.modules.get(clazz);
    }

    public void playSound(boolean enabled) {
        this.pendingSound = enabled;
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        for (Module module : this.modules.values()) {
            if (module.getKey() == 0 || module.getKey() != event.getKey()) {
                continue;
            }
            boolean shouldNotify = module.toggle();
            ClientSetting clientSetting = (ClientSetting) this.modules.get(ClientSetting.class);
            if (clientSetting != null && shouldNotify) {
                shouldNotify = clientSetting.toggleAlerts.getValue();
            }
            if(module instanceof GuiModule){
                shouldNotify = false;
            }
            if (shouldNotify) {
                String status = module.isEnabled() ? "&a&lON" : "&c&lOFF";
                String message = String.format("%s%s: %s&r", Astor.clientName, module.getName(), status);
                ChatUtil.sendFormatted(message);
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE && this.pendingSound != null) {
            boolean enabled = this.pendingSound;
            this.pendingSound = null;
            ClientSetting clientSetting = (ClientSetting) this.modules.get(ClientSetting.class);
            if (clientSetting != null) {
                SoundUtil.playSound(clientSetting.getToggleSound(enabled));
            }
        }
    }
}