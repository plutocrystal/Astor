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

public class Ambience extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final long DAY_LENGTH = 24000L;

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
                
                this.apply(1.0F, 1.0F);
                break;
            case SNOW:
                
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
        
        this.captured = false;
    }

    private void apply(float rain, float thunder) {
        if (!this.captured) {
            this.originalRain = mc.theWorld.getRainStrength(1.0F);
            this.originalThunder = mc.theWorld.getThunderStrength(1.0F);
            this.captured = true;
        }
        
        mc.theWorld.setRainStrength(rain);
        mc.theWorld.setThunderStrength(thunder);
    }

    private void restore() {
        if (!this.captured) {
            return;
        }
        
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

