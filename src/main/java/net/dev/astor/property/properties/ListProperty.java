package net.dev.astor.property.properties;

import com.google.gson.JsonObject;
import net.dev.astor.property.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A property that groups child properties behind a single collapsible row.
 *
 * <p>Unlike every other property, this one owns children: they are registered as normal properties of
 * the owner module so config round-trips and {@code setValue} validation work unchanged, but the GUI
 * renders them nested under this row instead of as siblings.</p>
 */
public class ListProperty extends Property<List<Property<?>>> {
    private final List<Property<?>> children;

    public ListProperty(String name, BooleanSupplier visibleChecker, Property<?>... children) {
        // The value is the child list itself: getValue() is what the GUI walks to build the nested rows.
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
        // Header shows no value text at all - just the name and the triangle.
        return "";
    }

    @Override
    public boolean parseString(String string) {
        // A group cannot be typed into; toggling is the whole interaction.
        return false;
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        // Children carry their own entries in the same object, so the group itself stores nothing.
        return true;
    }

    @Override
    public void write(JsonObject jsonObject) {
        // Intentionally empty for the same reason as read().
    }
}