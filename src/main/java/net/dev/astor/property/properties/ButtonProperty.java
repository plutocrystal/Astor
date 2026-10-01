package net.dev.astor.property.properties;

import com.google.gson.JsonObject;
import net.dev.astor.property.Property;

import java.util.function.BooleanSupplier;

public class ButtonProperty extends Property<Void> {
    private final Runnable action;

    public ButtonProperty(String name, Runnable action) {
        this(name, action, null);
    }

    public ButtonProperty(String name, Runnable action, BooleanSupplier booleanSupplier) {
        super(name, null, booleanSupplier);
        this.action = action;
    }

    public void click() {
        if (this.action != null) {
            this.action.run();
        }
    }

    @Override
    public String getValuePrompt() {
        return "click";
    }

    @Override
    public String formatValue() {
        return "&7Click";
    }

    @Override
    public boolean parseString(String string) {
        return false;
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return false;
    }

    @Override
    public void write(JsonObject jsonObject) {
    }
}