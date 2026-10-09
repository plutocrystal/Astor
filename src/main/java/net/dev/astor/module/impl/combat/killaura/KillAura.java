package net.dev.astor.module.impl.combat.killaura;

import net.dev.astor.Astor;
import net.dev.astor.enums.BlinkModules;
import net.dev.astor.event.EventManager;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.attack.AttackEvent;
import net.dev.astor.event.events.impl.attack.HitBlockEvent;
import net.dev.astor.event.events.impl.input.LeftClickMouseEvent;
import net.dev.astor.event.events.impl.input.RightClickMouseEvent;
import net.dev.astor.event.events.impl.movement.MoveInputEvent;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.event.events.impl.player.CancelUseEvent;
import net.dev.astor.event.events.impl.player.TickEvent;
import net.dev.astor.event.events.impl.player.UpdateEvent;
import net.dev.astor.event.events.impl.render.Render3DEvent;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.types.Priority;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.combat.killaura.attack.Attacker;
import net.dev.astor.module.impl.combat.killaura.render.DotRenderer;
import net.dev.astor.module.impl.combat.killaura.rotation.RotationController;
import net.dev.astor.module.impl.combat.killaura.target.AttackData;
import net.dev.astor.module.impl.combat.killaura.target.TargetSelector;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.ColorProperty;
import net.dev.astor.property.properties.FloatProperty;
import net.dev.astor.property.properties.IntProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.property.properties.PercentProperty;
import net.dev.astor.util.ItemUtil;
import net.dev.astor.util.PlayerUtil;
import net.dev.astor.util.RenderUtil;
import net.dev.astor.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;

import java.awt.*;
import java.util.ArrayList;

/**
 * The module itself: the settings, the event handlers, and the state the parts in the subpackages
 * share. The behaviour lives in the pieces beside this file rather than here - {@link Attacker} for
 * packets and the rate limit, {@link TargetSelector} for finding a target, {@link RotationController}
 * for the aim, {@link DotRenderer} for the marker.
 *
 * <p>The state is left as package-visible rather than moved into the pieces because it is genuinely
 * shared: the attacker's rate limit is read by the tick handler, the selector's switch index is
 * written by its own pick, and the rotation's aim is read by the renderer. Gathering it here keeps
 * each piece holding one concern instead of a web of accessors back to the module.</p>
 */
public class KillAura extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();

    private final Attacker attacker = new Attacker(this);
    private final TargetSelector targetSelector = new TargetSelector(this);
    private final RotationController rotationController = new RotationController(this);
    private final DotRenderer dotRenderer = new DotRenderer(this);

    private AttackData target = null;
    private int switchTick = 0;
    private boolean hitRegistered = false;
    private boolean blockingState = false;
    private boolean isBlocking = false;
    private boolean fakeBlockState = false;
    private long attackDelayMS = 0L;

    public final ModeProperty mode;
    public final ModeProperty sort;
    public final ModeProperty autoBlock;
    public final FloatProperty autoBlockRange;
    public final FloatProperty swingRange;
    public final FloatProperty attackRange;
    public final FloatProperty aimRange;
    public final IntProperty fov;
    public final IntProperty minCPS;
    public final IntProperty maxCPS;
    public final IntProperty switchDelay;
    public final ModeProperty rotations;
    public final ModeProperty rotationMode;
    public final ModeProperty moveFix;
    public final PercentProperty smoothing;
    public final BooleanProperty throughWalls;
    public final BooleanProperty requirePress;
    public final BooleanProperty allowMining;
    public final BooleanProperty weaponsOnly;
    public final BooleanProperty allowTools;
    public final BooleanProperty inventoryCheck;
    public final BooleanProperty preferEnemies;

    /**
     * Holds the swing back so hits land as crits: waits for the target to be ready to take a crit hit,
     * and for an airborne swing to be on the way down rather than up. Off by default, as in Astra.
     */
    public final BooleanProperty smartAttack = new BooleanProperty("SmartAttack", false);
    public final BooleanProperty dot = new BooleanProperty("Dot", false);
    public final FloatProperty dotSize = new FloatProperty("DotSize", 0.08F, 0.01F, 1.0F, 2, this.dot::getValue);
    public final ColorProperty outlineColor = new ColorProperty("OutlineColor", new Color(0, 0, 0).getRGB(), this.dot::getValue);
    public final ColorProperty fillColor = new ColorProperty("FillColor", new Color(255, 40, 40).getRGB(), this.dot::getValue);

    @Override
    public String getDescription() {
        return "Finds a target, turns to it and attacks on its own, with rotation smoothing and an optional target dot.";
    }

    public KillAura() {
        super("KillAura", Category.COMBAT, false);
        this.mode = new ModeProperty("Mode", 0, new String[]{"Single", "Switch"});
        this.sort = new ModeProperty("Sort", 0, new String[]{"Distance", "Health", "HurtTime", "Fov"});
        this.autoBlock = new ModeProperty("AutoBlock", 0, new String[]{"None", "Fake"});
        this.autoBlockRange = new FloatProperty("AutoBlockRange", 6.0F, 3.0F, 8.0F);
        this.swingRange = new FloatProperty("SwingRange", 3.5F, 3.0F, 6.0F);
        this.attackRange = new FloatProperty("AttackRange", 3.0F, 3.0F, 6.0F);
        this.aimRange = new FloatProperty("AimRange", 6.0F, 3.0F, 12.0F);
        this.fov = new IntProperty("Fov", 360, 30, 360);
        this.minCPS = new IntProperty("MinAps", 14, 1, 20);
        this.maxCPS = new IntProperty("MaxAps", 14, 1, 20);
        this.switchDelay = new IntProperty("SwitchDelay", 150, 0, 1000);
        this.rotations = new ModeProperty("Rotations", 1, new String[]{"None", "Silent", "LockView"});
        this.rotationMode = new ModeProperty("RotationMode", 0, new String[]{"Basic", "Physical"});
        this.moveFix = new ModeProperty("MoveFix", 1, new String[]{"None", "Silent", "Strict"});
        this.smoothing = new PercentProperty("Smoothing", 0);
        this.throughWalls = new BooleanProperty("ThroughWalls", true);
        this.requirePress = new BooleanProperty("RequirePress", false);
        this.allowMining = new BooleanProperty("AllowMining", true);
        this.weaponsOnly = new BooleanProperty("WeaponsOnly", true);
        this.allowTools = new BooleanProperty("AllowTools", false, this.weaponsOnly::getValue);
        this.inventoryCheck = new BooleanProperty("InventoryCheck", true);
        this.preferEnemies = new BooleanProperty("PreferEnemies", true);
    }

    // ---- shared state ----

    public Attacker getAttacker() {
        return this.attacker;
    }

    public TargetSelector getTargetSelector() {
        return this.targetSelector;
    }

    public RotationController getRotationController() {
        return this.rotationController;
    }

    public AttackData getTargetData() {
        return this.target;
    }

    public long getAttackDelayMS() {
        return this.attackDelayMS;
    }

    public void addAttackDelay(long amount) {
        this.attackDelayMS = this.attackDelayMS + amount;
    }

    public int getSwitchTick() {
        return this.switchTick;
    }

    public void advanceSwitchTick() {
        this.switchTick++;
    }

    public void resetSwitchTick() {
        this.switchTick = 0;
    }

    public boolean isHitRegistered() {
        return this.hitRegistered;
    }

    public void setHitRegistered(boolean hitRegistered) {
        this.hitRegistered = hitRegistered;
    }

    /** Clears the flag the switch mode reads, so the next pick advances exactly once per hit. */
    public void consumeHit() {
        this.hitRegistered = false;
    }

    public void setBlockingState(boolean blockingState) {
        this.blockingState = blockingState;
    }

    public void callAttackEvent(EntityLivingBase entity) {
        EventManager.call(new AttackEvent(entity));
    }

    // ---- aim state, forwarded to the rotation controller ----

    public boolean isAiming() {
        return this.rotationController.isAiming();
    }

    public float getAimYaw() {
        return this.rotationController.getAimYaw();
    }

    public float getAimPitch() {
        return this.rotationController.getAimPitch();
    }

    public float getPrevAimYaw() {
        return this.rotationController.getPrevAimYaw();
    }

    public float getPrevAimPitch() {
        return this.rotationController.getPrevAimPitch();
    }

    // ---- public API other modules use ----

    public EntityLivingBase getTarget() {
        return this.target != null ? this.target.getEntity() : null;
    }

    public boolean isAttackAllowed() {
        return this.attacker.isAttackAllowed();
    }

    public boolean shouldAutoBlock() {
        return false;
    }

    public boolean isBlocking() {
        return this.fakeBlockState && ItemUtil.isHoldingSword();
    }

    public boolean isPlayerBlocking() {
        return (mc.thePlayer.isUsingItem() || this.blockingState) && ItemUtil.isHoldingSword();
    }

    // ---- events ----

    @EventTarget(Priority.LOW)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.attacker.decayAttackDelay();
            boolean attack = this.target != null && this.targetSelector.canAttack();
            boolean block = attack && this.attacker.canAutoBlock();
            if (!block) {
                Astor.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                this.isBlocking = false;
                this.fakeBlockState = false;
            }
            if (attack) {
                boolean swap = false;
                if (block) {
                    switch (this.autoBlock.getValue()) {
                        case 0:
                            if (PlayerUtil.isUsingItem()) {
                                this.isBlocking = true;
                                if (!this.isPlayerBlocking() && !Astor.playerStateManager.digging && !Astor.playerStateManager.placing) {
                                    swap = true;
                                }
                            } else {
                                this.isBlocking = false;
                                if (this.isPlayerBlocking() && !Astor.playerStateManager.digging && !Astor.playerStateManager.placing) {
                                    this.attacker.stopBlock();
                                }
                            }
                            Astor.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                            this.fakeBlockState = false;
                            break;
                        case 1:
                            Astor.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                            this.isBlocking = false;
                            // Fake block is decided by whether a target could exist at all, not by
                            // whether one does: the point is the server seeing the block start before
                            // the swing, so it has to be armed the tick the target is picked.
                            this.fakeBlockState = this.targetSelector.hasValidTarget();
                            if (PlayerUtil.isUsingItem()
                                    && !this.isPlayerBlocking()
                                    && !Astor.playerStateManager.digging
                                    && !Astor.playerStateManager.placing) {
                                swap = true;
                            }
                            break;
                    }
                }
                boolean attacked = false;
                if (this.targetSelector.isBoxInSwingRange(this.target.getBox())) {
                    this.rotationController.applyAim(event, this.target);
                    if (attack) {
                        attacked = this.attacker.performAttack(this.target, event.getNewYaw(), event.getNewPitch());
                    }
                } else {
                    this.rotationController.applyAim(event, this.target);
                }
                if (swap) {
                    if (attacked) {
                        this.attacker.interactAttack(this.target, event.getNewYaw(), event.getNewPitch());
                    } else {
                        this.attacker.sendUseItem();
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        this.dotRenderer.draw(event.getPartialTicks());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    if (this.targetSelector.needsRetarget(this.target)
                            || this.timer.hasTimeElapsed(this.switchDelay.getValue().longValue())) {
                        this.timer.reset();
                        ArrayList<EntityLivingBase> targets = this.targetSelector.collectTargets();
                        if (targets.isEmpty()) {
                            this.target = null;
                        } else {
                            this.target = new AttackData(targets.get(this.targetSelector.pickTarget(targets, this.hitRegistered)));
                        }
                    }
                    if (this.target != null) {
                        this.target = new AttackData(this.target.getEntity());
                    }
                    break;
                case POST:
                    this.attacker.holdBlock();
                    break;
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && !event.isCancelled() && mc.thePlayer != null && mc.theWorld != null) {
            if (event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.getPacket();
                if (packet.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                    this.attacker.onUseItemReleased();
                }
            }
            if (event.getPacket() instanceof C09PacketHeldItemChange) {
                this.attacker.onHeldItemChanged();
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled()) {
            this.rotationController.applyMoveFix(event);
            if (this.shouldAutoBlock()) {
                mc.thePlayer.movementInput.jump = false;
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        this.cancelIfAiming(event);
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        this.cancelIfAiming(event);
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        this.cancelIfAiming(event);
    }

    /**
     * Stops the click reaching vanilla while the module is driving. Without it the attack is sent
     * twice - once here and once by the client when the mouse button is handled.
     */
    private void cancelIfAiming(net.dev.astor.event.events.callables.EventCancellable event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else if (this.isEnabled() && this.target != null && this.targetSelector.canAttack()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onCancelUse(CancelUseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnabled() {
        this.target = null;
        this.resetSwitchTick();
        this.hitRegistered = false;
        this.attackDelayMS = 0L;
        this.rotationController.reset();
        this.attacker.reset();
    }

    @Override
    public void onDisabled() {
        Astor.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
        this.blockingState = false;
        this.isBlocking = false;
        this.fakeBlockState = false;
        this.rotationController.reset();
        this.attacker.reset();
    }

    /**
     * Keeps the dependent settings inside their own ranges. SwingRange below AttackRange or MinAps
     * above MaxAps would both leave the module unable to act - the first with no window where it can
     * swing and still reach, the second with a reversed random range.
     */
    @Override
    public void verifyValue(String value) {
        if (this.swingRange.getName().equals(value)) {
            if (this.swingRange.getValue() < this.attackRange.getValue()) {
                this.attackRange.setValue(this.swingRange.getValue());
            }
        } else if (this.attackRange.getName().equals(value)) {
            if (this.swingRange.getValue() < this.attackRange.getValue()) {
                this.swingRange.setValue(this.attackRange.getValue());
            }
        } else if (this.minCPS.getName().equals(value)) {
            if (this.minCPS.getValue() > this.maxCPS.getValue()) {
                this.maxCPS.setValue(this.minCPS.getValue());
            }
        } else {
            if (this.maxCPS.getName().equals(value) && this.minCPS.getValue() > this.maxCPS.getValue()) {
                this.minCPS.setValue(this.maxCPS.getValue());
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.mode.getModeString()};
    }
}
