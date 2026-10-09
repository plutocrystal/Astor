package net.dev.astor.module.impl.player;

import net.dev.astor.module.Category;
import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.player.SwapItemEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.module.impl.misc.Target;
import net.dev.astor.util.ItemUtil;
import net.dev.astor.util.KeyBindUtil;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class AutoTool extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int currentToolSlot = -1;
    private int previousSlot = -1;
    // Render-only slot: the hotbar key the player pressed. The real slot is switched to the tool so the
    // server counts the right tool for speed/drops, while this drives what our own screen shows.
    private int spoofSlot = -1;
    private int tickDelayCounter = 0;

    /**
     * Milliseconds to wait before switching the held tool.
     *
     * <p>{@link #tickDelayCounter} counts ticks, so the value is divided by 50 where it is compared.
     * Anything under 50ms floors to zero, meaning no wait at all.</p>
     */
    public final IntProperty switchDelay = new IntProperty("Delay", 0, 0, 250);
    public final BooleanProperty switchBack = new BooleanProperty("SwitchBack", true);
    public final BooleanProperty sneakOnly = new BooleanProperty("SneakOnly", true);
    public final BooleanProperty itemSpoof = new BooleanProperty("ItemSpoof", false, () -> (Boolean) this.switchBack.getValue());

    @Override
    public String getDescription() {
        return "Switches to the right tool for the block you are mining, and can switch back afterwards.";
    }

    public AutoTool() {
        super("AutoTool", Category.PLAYER, false);
    }

    /**
     * Render-only slot backing ItemSpoof. Deliberately not the real tool slot: {@link #currentToolSlot}
     * is what the server sees and must stay real so mining speed and drops are correct.
     */
    public int getSlot() {
        return this.spoofSlot;
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled()) {
            this.spoofSlot = event.setSlot(this.spoofSlot);
        }
    }

    public boolean isKillAura() {
        KillAura killAura = (KillAura) Astor.moduleManager.modules.get(KillAura.class);
        if (!killAura.isEnabled()) return false;
        // Re-check the target rather than trust KillAura, so the block-breaking switch never stays
        // suppressed by a friend or teammate.
        return Target.get().isValidTarget(killAura.getTarget()) && killAura.isAttackAllowed();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.currentToolSlot != -1 && this.currentToolSlot != mc.thePlayer.inventory.currentItem) {
                this.currentToolSlot = -1;
                this.previousSlot = -1;
            }
            if (mc.objectMouseOver != null
                    && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK
                    && mc.gameSettings.keyBindAttack.isKeyDown()
                    && !mc.thePlayer.isUsingItem()
                    && !isKillAura()) {
                if (this.tickDelayCounter >= this.switchDelay.getValue() / 50
                        && (!(Boolean) this.sneakOnly.getValue() || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()))) {
                    int slot = ItemUtil.findInventorySlot(
                            mc.thePlayer.inventory.currentItem, mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock()
                    );
                    if (mc.thePlayer.inventory.currentItem != slot) {
                        if (this.previousSlot == -1) {
                            this.previousSlot = mc.thePlayer.inventory.currentItem;
                        }
                        mc.thePlayer.inventory.currentItem = this.currentToolSlot = slot;
                    }
                }
                this.tickDelayCounter++;
            } else {
                if (this.switchBack.getValue() && this.previousSlot != -1) {
                    mc.thePlayer.inventory.currentItem = this.previousSlot;
                }
                this.currentToolSlot = -1;
                this.previousSlot = -1;
                this.tickDelayCounter = 0;
            }
        }
    }

    @Override
    public void onEnabled() {
        // Seed with the slot already held so an untouched AutoTool renders identically to vanilla
        // until the player actually presses a hotbar key.
        this.spoofSlot = mc.thePlayer != null ? mc.thePlayer.inventory.currentItem : -1;
    }

    @Override
    public void onDisabled() {
        this.currentToolSlot = -1;
        this.previousSlot = -1;
        this.spoofSlot = -1;
        this.tickDelayCounter = 0;
    }
}
