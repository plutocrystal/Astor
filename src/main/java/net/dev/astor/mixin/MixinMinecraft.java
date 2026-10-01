package net.dev.astor.mixin;

import net.dev.astor.Astor;
import net.dev.astor.init.Initializer;
import net.dev.astor.event.EventManager;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.attack.HitBlockEvent;
import net.dev.astor.event.events.impl.input.KeyEvent;
import net.dev.astor.event.events.impl.input.LeftClickMouseEvent;
import net.dev.astor.event.events.impl.player.LoadWorldEvent;
import net.dev.astor.event.events.impl.render.ResizeEvent;
import net.dev.astor.event.events.impl.input.RightClickMouseEvent;
import net.dev.astor.event.events.impl.player.SwapItemEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.module.impl.combat.NoHitDelay;
import net.dev.astor.module.impl.exploit.MultiActions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {Minecraft.class}, priority = 9999)
public abstract class MixinMinecraft {
    @Shadow
    private int leftClickCounter;
    @Shadow
    public PlayerControllerMP playerController;
    @Shadow
    public WorldClient theWorld;
    @Shadow
    public EntityPlayerSP thePlayer;
    @Shadow
    public GuiScreen currentScreen;
    @Shadow
    public GameSettings gameSettings;

    @Inject(
            method = {"startGame"},
            at = {@At("HEAD")}
    )
    private void startGame(CallbackInfo callbackInfo) {
        new Initializer();
    }

    @Inject(
            method = {"startGame"},
            at = {@At("RETURN")}
    )
    private void postStartGame(CallbackInfo callbackInfo) {
        new Astor();
    }

    @Inject(
            method = {"runTick"},
            at = {@At("HEAD")}
    )
    private void runTick(CallbackInfo callbackInfo) {
        if (this.theWorld != null && this.thePlayer != null) {
            EventManager.call(new TickEvent(EventType.PRE));
        }
    }

    @Inject(
            method = {"runTick"},
            at = {@At("RETURN")}
    )
    private void postRunTick(CallbackInfo callbackInfo) {
        if (this.theWorld != null && this.thePlayer != null) {
            EventManager.call(new TickEvent(EventType.POST));
        }
    }

    @Inject(
            method = {"loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V"},
            at = {@At("HEAD")}
    )
    private void loadWorld(WorldClient worldClient, String string, CallbackInfo callbackInfo) {
        EventManager.call(new LoadWorldEvent());
    }

    @Inject(
            method = {"updateFramebufferSize"},
            at = {@At("RETURN")}
    )
    private void updateFramebufferSize(CallbackInfo callbackInfo) {
        EventManager.call(new ResizeEvent());
    }

    @Inject(
            method = {"clickMouse"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void clickMouse(CallbackInfo callbackInfo) {
        if (Astor.moduleManager != null && Astor.moduleManager.modules.get(NoHitDelay.class).isEnabled()) {
            this.leftClickCounter = 0;
        }
        LeftClickMouseEvent event = new LeftClickMouseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"rightClickMouse"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void rightClickMouse(CallbackInfo callbackInfo) {
        RightClickMouseEvent event = new RightClickMouseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = {"sendClickBlockToController"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void sendClickBlockToController(CallbackInfo callbackInfo) {
        HitBlockEvent event = new HitBlockEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    /**
     * Vanilla refuses to break a block while the player is using an item, which locks out mining
     * for as long as right click is held, sword blocking included. MultiActions lifts that gate.
     */
    @Redirect(
            method = {"sendClickBlockToController"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z"
            )
    )
    private boolean allowBlockBreakingWhileUsingItem(EntityPlayerSP entityPlayerSP) {
        return !MultiActions.isMultiActioning() && entityPlayerSP.isUsingItem();
    }

    @Redirect(
            method = {"runTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V"
            )
    )
    private void setKeyBindState(int integer, boolean boolean2) {
        KeyBinding.setKeyBindState(integer, boolean2);
        if (boolean2 && this.currentScreen == null) {
            EventManager.call(new KeyEvent(integer));
        }
    }

    /**
     * runTick branches on isUsingItem and, in that branch, spins on the attack key binding until
     * isPressed drains it. The press is thrown away there and clickMouse is never reached, so an
     * attack made while right click is held is silently dropped. MultiActions turns that press
     * into a real click. It only reacts to an actual left click press, never on its own, and the
     * press is still drained so the vanilla spin loop terminates as usual.
     */
    @Redirect(
            method = {"runTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/settings/KeyBinding;isPressed()Z"
            )
    )
    private boolean attackWhileUsingItem(KeyBinding keyBinding) {
        boolean pressed = keyBinding.isPressed();
        if (pressed && keyBinding == this.gameSettings.keyBindAttack && MultiActions.isMultiActioning()) {
            ((IAccessorMinecraft) (Object) this).callClickMouse();
        }
        return pressed;
    }

    @Redirect(
            method = {"runTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/InventoryPlayer;changeCurrentItem(I)V"
            )
    )
    private void changeCurrentItem(InventoryPlayer inventoryPlayer, int slot) {
        SwapItemEvent event = new SwapItemEvent(-1, slot);
        EventManager.call(event);
        if (!event.isCancelled()) {
            inventoryPlayer.changeCurrentItem(slot);
        }
    }
}
