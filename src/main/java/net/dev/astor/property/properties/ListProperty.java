package net.dev.astor.property.properties;

import com.google.gson.JsonObject;
import net.dev.astor.property.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ListProperty extends Property<List<Property<?>>> {
    private final List<Property<?>> children;

    public ListProperty(String name, BooleanSupplier visibleChecker, Property<?>... children) {
        
        super(name, new ArrayList<Property<?>>(Arrays.asList(children)), visibleChecker);
        this.children = this.getValue();
    }

    public List<Property<?>> getChildren() {
        return this.children;
    }

    @Override
    public String getValuePrompt() {
        return "list";
    }

    @Override
    public String formatValue() {
        
        return "";
    }

    @Override
    public boolean parseString(String string) {
        
        return false;
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        
        return true;
    }

    @Override
    public void write(JsonObject jsonObject) {
        
    }
}