package net.dev.astor.mixin.client;

import net.dev.astor.Astor;
import net.dev.astor.init.Initializer;
import net.dev.astor.event.EventManager;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.events.impl.attack.HitBlockEvent;
import net.dev.astor.event.events.impl.input.KeyEvent;
import net.dev.astor.event.events.impl.input.LeftClickMouseEvent;
import net.dev.astor.event.events.impl.player.LoadWorldEvent;
import net.dev.astor.event.events.impl.render.GameLoopEvent;
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
    @Shadow
    public net.minecraft.util.Timer timer;

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

    @Inject(
            method = {"runGameLoop"},
            at = {@At("TAIL")}
    )
    private void frameInput(CallbackInfo callbackInfo) {
        this.pumpGuiInput();
        this.pumpKeyEvents();
    }

    /**
     * HEAD rather than TAIL so a speed written here is picked up by this frame's timer.updateTimer()
     * instead of the next one. Dispatching before the tick loop is also what lets a listener run at all
     * when a previous listener has already pinned the tick counter at zero.
     */
    @Inject(
            method = {"runGameLoop"},
            at = {@At("HEAD")}
    )
    private void gameLoop(CallbackInfo callbackInfo) {
        GameLoopEvent event = new GameLoopEvent(this.timer.timerSpeed);
        EventManager.call(event);
        this.timer.timerSpeed = event.getTimerSpeed();
    }

    @Redirect(
            method = {"runGameLoop"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;isEntityInsideOpaqueBlock()Z"
            )
    )
    private boolean keepPerspectiveInsideBlock(EntityPlayerSP entityPlayerSP) {
        // Vanilla reads this and, when true, runs `gameSettings.thirdPersonView = 0` - it overwrites
        // the player's own perspective every frame the camera clips into terrain and never puts it
        // back, so suffocating or being shoved into a wall silently forces first person for good.
        // Always reporting "not inside a block" makes that branch never execute.
        return false;
    }

    private void pumpGuiInput() {
        if (this.currentScreen == null) {
            return;
        }
        try {
            this.currentScreen.handleInput();
        } catch (IOException e) {

        }
    }

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