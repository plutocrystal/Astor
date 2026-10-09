package net.dev.astor.module.impl.render;

import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

public class AntiBlind extends Module {

    /** Channel the server uses to ask the client to open the book a player is holding. */
    private static final String OPEN_BOOK_CHANNEL = "MC|BOpen";

    public final BooleanProperty bossHealth = new BooleanProperty("BossHealth", true);
    public final BooleanProperty bookPage = new BooleanProperty("BookPage", true);
    public final BooleanProperty achievements = new BooleanProperty("Achievements", true);

    @Override
    public String getDescription() {
        return "Removes the overlays that get in the way: the boss health bar, the page a written book "
                + "opens into, and achievement popups.";
    }

    public AntiBlind() {
        super("AntiBlind", Category.RENDER, false);
    }

    public static AntiBlind get() {
        if (Astor.moduleManager == null) {
            return null;
        }
        AntiBlind antiBlind = (AntiBlind) Astor.moduleManager.modules.get(AntiBlind.class);
        return antiBlind != null && antiBlind.isEnabled() ? antiBlind : null;
    }

    /**
     * Drops the packet that pops a written book's page open.
     *
     * <p>The server sends {@code MC|BOpen} when a player starts reading, and vanilla answers by opening
     * the book's page on top of the screen. Dropping the packet is what stops that, rather than
     * cancelling the render, because the popup is opened from the packet handler.</p>
     *
     * <p>The enabled check is explicit here even though upstream does not need one. LiquidBounce's event
     * manager only hands an event to a module that is on, while Astor's calls every registered handler
     * regardless, so without it the book page would stay suppressed with the module switched off.</p>
     */
    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || !this.bookPage.getValue() || event.isCancelled()
                || !(event.getPacket() instanceof S3FPacketCustomPayload)) {
            return;
        }
        if (OPEN_BOOK_CHANNEL.equals(((S3FPacketCustomPayload) event.getPacket()).getChannelName())) {
            event.setCancelled(true);
        }
    }
}