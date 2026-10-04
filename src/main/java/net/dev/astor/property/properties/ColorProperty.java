package net.dev.astor.property.properties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dev.astor.property.Property;

import java.util.function.BooleanSupplier;

public class ColorProperty extends Property<Integer> {
    public ColorProperty(String name, Integer color) {
        this(name, color, null);
    }

    public ColorProperty(String string, Integer color, BooleanSupplier check) {
        super(string, color, rgb -> true, check);
    }

    @Override
    public String getValuePrompt() {
        return "ARGB";
    }

    @Override
    public String formatValue() {
        int argb = this.getValue();
        String hex = String.format("%06X", argb & 0xFFFFFF);
        int alpha = argb >>> 24 & 0xFF;
        String alphaCode = alpha == 0xFF ? "" : String.format("&7%02X", alpha);
        return String.format("&c%s&a%s&9%s%s", hex.substring(0, 2), hex.substring(2, 4), hex.substring(4, 6), alphaCode);
    }

    @Override
    public boolean parseString(String string) {
        if (string == null) {
            return false;
        }
        String hex = string.trim().replace("#", "");
        if (hex.isEmpty() || hex.length() > 8) {
            return false;
        }
        for (int i = 0; i < hex.length(); i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                return false;
            }
        }

        return this.setValue((int) Long.parseLong(hex, 16));
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        JsonElement element = jsonObject.get(this.getName());
        if (element == null || !element.isJsonPrimitive()) {
            return false;
        }
        return this.parseString(element.getAsString());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), String.format("%08X", this.getValue()));
    }
}