package net.dev.astor.property.properties;

import com.google.gson.JsonObject;
import net.dev.astor.property.Property;

import java.util.function.BooleanSupplier;

public class FloatProperty extends Property<Float> {
    
    private static final int DEFAULT_PRECISION = 1;

    private final Float minimum;
    private final Float maximum;
    private final int precision;

    public FloatProperty(String name, Float value, Float minimum, Float maximum) {
        this(name, value, minimum, maximum, null);
    }

    public FloatProperty(String name, Float value, Float minimum, Float maximum, BooleanSupplier check) {
        this(name, value, minimum, maximum, DEFAULT_PRECISION, check);
    }

    public FloatProperty(String name, Float value, Float minimum, Float maximum, int precision) {
        this(name, value, minimum, maximum, precision, null);
    }

    public FloatProperty(
            String name, Float value, Float minimum, Float maximum, int precision, BooleanSupplier check
    ) {
        
        super(name, value, floatV -> floatV >= minimum && floatV <= maximum, check);
        this.minimum = minimum;
        this.maximum = maximum;
        this.precision = Math.max(0, precision);
    }

    @Override
    public String getValuePrompt() {
        return String.format("%s-%s", this.minimum, this.maximum);
    }
    @Override
    public String formatValue() {
        return String.format("&6%s", this.getValue());
    }

    @Override
    public boolean parseString(String string) {
        return this.setValue(Float.parseFloat(string));
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        return this.setValue(jsonObject.get(this.getName()).getAsNumber().floatValue());
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), this.getValue());
    }

    public Float getMinimum() {
        return minimum;
    }

    public Float getMaximum() {
        return maximum;
    }

    public int getPrecision() {
        return precision;
    }

    public double getStep() {
        return Math.pow(10.0D, -this.precision);
    }
}