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
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.combat.NoHitDelay;
import net.dev.astor.module.impl.exploit.ExploitFixer;
import net.dev.astor.module.impl.exploit.MultiActions;
import net.dev.astor.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@SideOnly(Side.CLIENT)
@Mixin(value = {Minecraft.class}, priority = 9999)
public abstract class MixinMinecraft {
    @Unique
    private final Set<Integer> frameDownKeys = new HashSet<>();
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

    /**
     * ExploitFixer swallows the crash when it is asked to, which is what keeps a client alive on a
     * server that is deliberately sending packets the handler cannot take. The call is already
     * inside a crash handler, so nothing it does may throw.
     */
    @Inject(
            method = {"crashed"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void crashed(CrashReport crashReport, CallbackInfo callbackInfo) {
        if (ExploitFixer.onCrash(crashReport)) {
            callbackInfo.cancel();
        }
    }

    /**
     * runTick only runs while the world is ticking, and the tick loop is what Timer drives, so
     * anything fired from inside runTick dies with a slow or frozen game. Input is pulled here
     * instead, once per frame: Display.update polls the keyboard and mouse into LWJGL's buffers
     * from runGameLoop as well, so the raw key state is as fresh here as it is inside a tick.
     */
    @Inject(
            method = {"runGameLoop"},
            at = {@At("TAIL")}
    )
    private void frameInput(CallbackInfo callbackInfo) {
        this.pumpGuiInput();
        this.pumpKeyEvents();
    }

    /**
     * GuiScreen.handleInput drains the keyboard and mouse event queues, so on a frame where runTick
     * already handled the screen this finds nothing left to do and changes nothing. Only on a frame
     * where the world did not tick at all does it become the one that feeds the screen, which is
     * what keeps the ClickGui alive while the game is frozen or slowed down.
     */
    private void pumpGuiInput() {
        if (this.currentScreen == null) {
            return;
        }
        try {
            this.currentScreen.handleInput();
        } catch (IOException e) {
            // A screen throwing on input must not take the frame down with it.
        }
    }

    /**
     * Dispatched on the rising edge of a key instead of on the tick the press arrived in, so a
     * module bind behaves the same at every game speed. The candidates are the vanilla key binds
     * plus whatever the user bound modules to; the previous state is kept so a held key only ever
     * reports once. A press made while a screen is open updates that state without firing, which is
     * what the tick bound version did as well.
     */
    private void pumpKeyEvents() {
        if (Astor.moduleManager == null || this.gameSettings == null) {
            return;
        }
        for (KeyBinding keyBinding : this.gameSettings.keyBindings) {
            this.pollKey(keyBinding.getKeyCode());
        }
        for (Module module : Astor.moduleManager.modules.values()) {
            this.pollKey(module.getKey());
        }
    }

    private void pollKey(int keyCode) {
        if (keyCode == 0) {
            return;
        }
        if (!KeyBindUtil.isKeyDown(keyCode)) {
            this.frameDownKeys.remove(keyCode);
            return;
        }
        if (this.frameDownKeys.add(keyCode) && this.currentScreen == null) {
            EventManager.call(new KeyEvent(keyCode));
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
