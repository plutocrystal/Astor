package net.dev.astor.module.impl.misc;

import net.dev.astor.module.Category;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.render.Render2DEvent;
import net.dev.astor.module.Module;
import net.dev.astor.util.TimerUtil;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.TextProperty;
import net.minecraft.client.Minecraft;

public class Spammer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    private int charOffset = 19968;
    public final TextProperty text = new TextProperty("Text", "meow");
    public final FloatProperty delay = new FloatProperty("Delay", 3.5F, 0.0F, 3600.0F);
    public final IntProperty random = new IntProperty("Random", 0, 0, 10);

    public Spammer() {
        super("Spammer", Category.MISC, false);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled()) {
            if (this.timer.hasTimeElapsed((long) (this.delay.getValue() * 1000.0F))) {
                this.timer.reset();
                String text = this.text.getValue();
                if (this.random.getValue() > 0) {
                    text = String.format("%s ", text);
                    for (int i = 0; i < this.random.getValue(); i++) {
                        text = String.format("%s%s", text, (char) this.charOffset);
                        this.charOffset++;
                        if (this.charOffset > 40959) {
                            this.charOffset = 19968;
                        }
                    }
                }
                mc.thePlayer.sendChatMessage(text);
            }
        }
    }
}