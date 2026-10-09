package net.dev.astor.module;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.render.ClientSetting;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.util.KeyBindUtil;

public abstract class Module {
    protected final String name;
    protected final Category category;
    protected final boolean defaultEnabled;
    protected final int defaultKey;
    protected boolean enabled;
    protected int key;
    
    public final BooleanProperty hide;

    public Module(String name, Category category, boolean enabled) {
        this(name, category, enabled, false);
    }

    public Module(String name, Category category, boolean enabled, boolean hidden) {
        this.name = name;
        this.category = category;
        this.enabled = this.defaultEnabled = enabled;
        this.key = this.defaultKey = 0;
        this.hide = new BooleanProperty("Hide", hidden);
    }

    public String getName() {
        return this.name;
    }

    public Category getCategory() {
        return this.category;
    }

    public String formatModule() {
        return String.format(
                "%s%s &r(%s&r)",
                this.key == 0 ? "" : String.format("&l[%s] &r", KeyBindUtil.getKeyName(this.key)),
                this.name,
                this.enabled ? "&a&lON" : "&c&lOFF"
        );
    }

    public String[] getSuffix() {
        return new String[0];
    }

    public String getDescription() {
        return "";
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                this.onEnabled();
            } else {
                this.onDisabled();
            }
        }
    }

    public boolean toggle() {
        boolean enabled = !this.enabled;
        this.setEnabled(enabled);
        if (this.enabled == enabled) {
            ClientSetting clientSetting = (ClientSetting) Astor.moduleManager.modules.get(ClientSetting.class);
            if (clientSetting != null && clientSetting.toggleSounds.getValue()) {
                Astor.moduleManager.playSound(enabled);
            }
            return true;
        } else {
            return false;
        }
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int integer) {
        if (integer != 0 && Astor.moduleManager != null) {
            for (Module module : Astor.moduleManager.modules.values()) {
                if (module != this && module.getKey() == integer) {
                    module.setKey(0);
                }
            }
        }
        this.key = integer;
    }

    public void setKeyShared(int integer) {
        this.key = integer;
    }

    public boolean isHidden() {
        return this.hide.getValue();
    }

    public void setHidden(boolean boolean1) {
        this.hide.setValue(boolean1);
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void verifyValue(String string) {
    }
}