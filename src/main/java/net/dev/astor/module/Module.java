package net.dev.astor.module;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.render.HUD;
import net.dev.astor.util.KeyBindUtil;

public abstract class Module {
    protected final String name;
    protected final Category category;
    protected final boolean defaultEnabled;
    protected final int defaultKey;
    protected final boolean defaultHidden;
    protected boolean enabled;
    protected int key;
    protected boolean hidden;

    public Module(String name, Category category, boolean enabled) {
        this(name, category, enabled, false);
    }

    public Module(String name, Category category, boolean enabled, boolean hidden) {
        this.name = name;
        this.category = category;
        this.enabled = this.defaultEnabled = enabled;
        this.key = this.defaultKey = 0;
        this.hidden = this.defaultHidden = hidden;
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
            if (((HUD) Astor.moduleManager.modules.get(HUD.class)).toggleSound.getValue()) {
                Astor.moduleManager.playSound();
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

    /**
     * Binds without taking the key away from other modules, used by ".bind *".
     */
    public void setKeyShared(int integer) {
        this.key = integer;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean boolean1) {
        this.hidden = boolean1;
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void verifyValue(String string) {
    }
}
