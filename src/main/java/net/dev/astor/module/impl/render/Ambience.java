package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.player.LoadWorldEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.event.types.EventType;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;

/**
 * Pins the time of day and the weather.
 *
 * <p>Time is handled in {@code MixinNetHandlerPlayClient}: the server pushes an S03 every second or
 * so and we substitute our own value, which is the only way to actually hold a time rather than fight
 * the server for it tick by tick.</p>
 *
 * <p>Weather is applied per tick instead. The client never simulates weather - WorldClient's
 * updateWeather() is an empty override - so the rain and thunder strengths are simply whatever the
 * world was left holding. Writing both fields each tick pins them.</p>
 */
public class Ambience extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Vanilla mods a day by this. 24000 itself is the same instant as 0, so the range stops one short
     * rather than letting the slider sit on a value that is not a distinct time.
     */
    private static final long DAY_LENGTH = 24000L;

    /**
     * Stand-in temperature for the snow branch. 1.8.9 decides rain against snow with a bare
     * {@code >= 0.15F} on the biome temperature, so any value below that lands in vanilla's snow path;
     * this one sits among the temperatures real frozen biomes report rather than at an extreme.
     */
    public static final float SNOW_TEMPERATURE = -0.5F;

    private static final int VANILLA = 0;
    private static final int CLEAR = 1;
    private static final int RAIN = 2;
    private static final int THUNDER = 3;
    private static final int SNOW = 4;

    public final IntProperty time = new IntProperty("Time", 6000, 0, 23999);
    public final ModeProperty mode = new ModeProperty(
            "Mode", VANILLA, new String[]{"Vanilla", "Clear", "Rain", "Thunder", "Snow"}
    );

    /**
     * What the world looked like before we started overriding, so returning to Vanilla puts the sky
     * back instead of leaving it pinned at whatever we last wrote.
     */
    private float originalRain = 0.0F;
    private float originalThunder = 0.0F;
    private boolean captured = false;

    @Override
    public String getDescription() {
        return "Pins the time of day and the weather instead of letting the server change it.";
    }

    public Ambience() {
        super("Ambience", Category.RENDER, false);
    }

    public static Ambience get() {
        if (Astor.moduleManager == null) {
            return null;
        }
        return (Ambience) Astor.moduleManager.modules.get(Ambience.class);
    }

    /**
     * The time to show, or null when the world should keep its own - which is only ever the case while
     * the module is off. Modulo so a value arriving from anywhere cannot land outside a single day.
     */
    public Long getLockedTime() {
        if (!this.isEnabled()) {
            return null;
        }
        long value = this.time.getValue().longValue() % DAY_LENGTH;
        return value < 0L ? value + DAY_LENGTH : value;
    }

    public boolean isSnow() {
        return this.isEnabled() && this.mode.getValue() == SNOW;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE || mc.theWorld == null) {
            return;
        }
        if (!this.isEnabled()) {
            this.restore();
            return;
        }
        switch (this.mode.getValue()) {
            case CLEAR:
                this.apply(0.0F, 0.0F);
                break;
            case RAIN:
                this.apply(1.0F, 0.0F);
                break;
            case THUNDER:
                // getThunderStrength multiplies through getRainStrength, so rain has to be up as well
                // or the thunder strength is scaled straight back to nothing.
                this.apply(1.0F, 1.0F);
                break;
            case SNOW:
                // Snow still needs rain strength - it is the only switch that starts precipitation at
                // all. What falls is decided in MixinEntityRenderer, which hands the biome to vanilla's
                // cold branch.
                this.apply(1.0F, 0.0F);
                break;
            case VANILLA:
            default:
                this.restore();
                break;
        }
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        // A new world starts from whatever weather it was built with, so re-read it on the next tick.
        this.captured = false;
    }

    private void apply(float rain, float thunder) {
        if (!this.captured) {
            this.originalRain = mc.theWorld.getRainStrength(1.0F);
            this.originalThunder = mc.theWorld.getThunderStrength(1.0F);
            this.captured = true;
        }
        // setRainStrength and setThunderStrength write the previous and the current value together,
        // which is exactly what getRainStrength interpolates between - so a change lands immediately
        // rather than fading in over the next second.
        mc.theWorld.setRainStrength(rain);
        mc.theWorld.setThunderStrength(thunder);
    }

    private void restore() {
        if (!this.captured) {
            return;
        }
        // The world is gone by the time the module is switched off after a disconnect, and there is
        // nothing left to put back - but the capture still has to be dropped either way.
        this.captured = false;
        if (mc.theWorld == null) {
            return;
        }
        mc.theWorld.setRainStrength(this.originalRain);
        mc.theWorld.setThunderStrength(this.originalThunder);
    }

    @Override
    public void onEnabled() {
        this.captured = false;
    }

    @Override
    public void onDisabled() {
        this.restore();
    }

    @Override
    public String[] getSuffix() {
        Long locked = this.getLockedTime();
        return locked == null ? new String[0] : new String[]{String.format("%d", locked)};
    }
}
