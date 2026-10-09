package net.dev.astor.property.properties;

import com.google.gson.JsonObject;
import net.dev.astor.property.Property;

import java.util.function.BooleanSupplier;

public class FloatProperty extends Property<Float> {
    /**
     * One decimal place, which is what every float slider did before precision became configurable.
     */
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

    /**
     * @param precision 小数位数，同时决定滑块拖动和滚轮每次走的距离（10^-precision）。范围远小于 1.0 的
     *                  属性需要比默认更细的步长，否则整个范围都跨不到 —— 比如 DotSize 最小 0.01 最大 1.0，
     *                  用默认的一位小数就只能选到 0.1、0.2 这些。
     */
    public FloatProperty(
            String name, Float value, Float minimum, Float maximum, int precision, BooleanSupplier check
    ) {
        // Range-checked against the declared bounds rather than a hardcoded >= 0: a property whose range
        // starts below zero (Animations' SwingSpeed is -200..50) could not otherwise be moved into its own
        // negative half at all, since every negative value was rejected. A NaN fails the comparison and is
        // rejected here too.
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

    /**
     * How far one slider drag or scroll wheel tick moves the value. Kept as a double rather than routed
     * through {@code float}: {@code (float) 0.1} widens to 0.10000000149011612, which is then the
     * divisor every dragged value is snapped against.
     */
    public double getStep() {
        return Math.pow(10.0D, -this.precision);
    }
}