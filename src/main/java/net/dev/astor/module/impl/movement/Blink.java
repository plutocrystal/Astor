package net.dev.astor.module.impl.movement;

import net.dev.astor.module.Category;
import net.dev.astor.Astor;
import net.dev.astor.enums.BlinkModules;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.types.Priority;
import net.dev.astor.event.events.impl.player.LoadWorldEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.ModeProperty;

public class Blink extends Module {
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Default", "Pulse"});
    public final IntProperty ticks = new IntProperty("Ticks", 20, 0, 1200);

    public Blink() {
        super("Blink", Category.MOVEMENT, false);
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            if (!Astor.blinkManager.getBlinkingModule().equals(BlinkModules.BLINK)) {
                this.setEnabled(false);
            } else {
                if (this.ticks.getValue() > 0 && Astor.blinkManager.countMovement() > (long) this.ticks.getValue()) {
                    switch (this.mode.getValue()) {
                        case 0:
                            this.setEnabled(false);
                            break;
                        case 1:
                            Astor.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                            Astor.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.setEnabled(false);
    }

    @Override
    public void onEnabled() {
        Astor.blinkManager.setBlinkState(false, Astor.blinkManager.getBlinkingModule());
        Astor.blinkManager.setBlinkState(true, BlinkModules.BLINK);
    }

    @Override
    public void onDisabled() {
        Astor.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }
}