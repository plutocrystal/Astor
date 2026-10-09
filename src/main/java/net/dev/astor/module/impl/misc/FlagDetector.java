package net.dev.astor.module.impl.misc;

import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class FlagDetector extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private short counter;

    @Override
    public String getDescription() {
        return "Counts the server corrections you receive, so you can tell when you are being flagged or rubber-banded.";
    }

    public FlagDetector() {
        super("FlagDetector", Category.MISC, false, true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE
                && event.getPacket() instanceof S08PacketPlayerPosLook
                && mc.thePlayer != null && mc.thePlayer.ticksExisted > 40) {
            this.counter++;
            ChatUtil.sendFormatted(
                    String.format("%s&cFlag Detected: &7%d&r", Astor.clientName, this.counter)
            );
        }
    }
}
