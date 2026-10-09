package net.dev.astor.mixin.render;

import net.dev.astor.Astor;
import net.dev.astor.data.Box;
import net.dev.astor.event.EventManager;
import net.dev.astor.event.events.impl.render.PickEvent;
import net.dev.astor.event.events.impl.render.RaytraceEvent;
import net.dev.astor.event.events.impl.render.Render3DEvent;
import net.dev.astor.mixin.player.IAccessorEntityLivingBase;
import net.dev.astor.mixin.player.IAccessorEntityPlayer;
import net.dev.astor.module.impl.combat.killaura.KillAura;
import net.dev.astor.module.impl.player.AntiDebuff;
import net.dev.astor.module.impl.player.AutoBlockIn;
import net.dev.astor.module.impl.player.AutoTool;
import net.dev.astor.module.impl.player.GhostHand;
import net.dev.astor.module.impl.player.Scaffold;
import net.dev.astor.module.impl.render.Ambience;
import net.dev.astor.module.impl.render.AspectRatio;
import net.dev.astor.module.impl.render.NoHurtCam;
import net.dev.astor.module.impl.render.NoRender;
import net.dev.astor.module.impl.render.ViewClip;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(value = {EntityRenderer.class}, priority = 9999)
public abstract class MixinEntityRenderer {
    @Unique
    private List<Integer> itemSpoofStack = null;
    @Unique
    private Box<ItemStack> using = null;
    @Unique
    private Box<Integer> useCount = null;
    @Shadow
    private Minecraft mc;
    @Shadow
    private float thirdPersonDistance;

    /**
     * Only here so the ViewBobbing redirect below can put the call back when the option is off.
     * setupViewBobbing is private in EntityRenderer, so there is no other way to reach it - and
     * calling the shadow inside a merged mixin is just a call to the target's own method.
     */
    @Shadow
    private void setupViewBobbing(float partialTicks) {
        throw new AssertionError();
    }

    @Inject(
            method = {"updateCameraAndRender"},
            at = {@At("HEAD")}
    )
    private void updateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        if (this.mc.thePlayer != null) {
            Scaffold scaffold = (Scaffold) Astor.moduleManager.modules.get(Scaffold.class);
            if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
                this.spoofItem(scaffold.getSlot());
            }
            AutoTool autoTool = (AutoTool) Astor.moduleManager.modules.get(AutoTool.class);
            if (autoTool.isEnabled() && autoTool.itemSpoof.getValue()) {
                this.spoofItem(autoTool.getSlot());
            }
            KillAura killAura = (KillAura) Astor.moduleManager.modules.get(KillAura.class);
            if (killAura.isEnabled() && killAura.isBlocking()) {
                this.using = new Box<>(((IAccessorEntityPlayer) this.mc.thePlayer).getItemInUse());
                ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUse(this.mc.thePlayer.inventory.getCurrentItem());
                this.useCount = new Box<>(((IAccessorEntityPlayer) this.mc.thePlayer).getItemInUseCount());
                ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUseCount(69000);
            }
        }
    }

    @Inject(
            method = {"updateCameraAndRender"},
            at = {@At("RETURN")}
    )
    private void postUpdateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        this.restoreSpoofedItem();
        if (this.using != null) {
            ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUse(this.using.value);
            this.using = null;
        }
        if (this.useCount != null) {
            ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUseCount(this.useCount.value);
            this.useCount = null;
        }
    }

    @Inject(
            method = {"updateRenderer"},
            at = {@At("HEAD")}
    )
    private void updateRenderer(CallbackInfo callbackInfo) {
        Scaffold scaffold = (Scaffold) Astor.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
            this.spoofItem(scaffold.getSlot());
        }

        AutoBlockIn autoBlockIn = (AutoBlockIn) Astor.moduleManager.modules.get(AutoBlockIn.class);
        if (autoBlockIn.isEnabled() && autoBlockIn.itemSpoof.getValue()) {
            this.spoofItem(autoBlockIn.getSlot());
        }

        AutoTool autoTool = (AutoTool) Astor.moduleManager.modules.get(AutoTool.class);
        if (autoTool.isEnabled() && autoTool.itemSpoof.getValue()) {
            this.spoofItem(autoTool.getSlot());
        }
    }

    private void spoofItem(int slot) {
        if (slot < 0 || slot > 8 || slot == this.mc.thePlayer.inventory.currentItem) {
            return;
        }
        if (this.itemSpoofStack == null) {
            this.itemSpoofStack = new ArrayList<>();
        }
        this.itemSpoofStack.add(this.mc.thePlayer.inventory.currentItem);
        this.mc.thePlayer.inventory.currentItem = slot;
    }

    private void restoreSpoofedItem() {
        if (this.itemSpoofStack != null && !this.itemSpoofStack.isEmpty()) {
            this.mc.thePlayer.inventory.currentItem = this.itemSpoofStack.get(0);
            this.itemSpoofStack.clear();
        }
    }

    @Inject(
            method = {"updateRenderer"},
            at = {@At("RETURN")}
    )
    private void postUpdateRenderer(CallbackInfo callbackInfo) {
        this.restoreSpoofedItem();
    }

    @Inject(
            method = {"renderWorldPass"},
            at = {@At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z",
                    shift = At.Shift.BEFORE
            )}
    )
    private void renderWorldPass(int integer, float float2, long long3, CallbackInfo callbackInfo) {
        EventManager.call(new Render3DEvent(float2));
    }

    @ModifyConstant(
            method = {"hurtCameraEffect"},
            constant = {@Constant(
                    floatValue = 14.0F,
                    ordinal = 0
            )}
    )
    private float hurtCameraEffect(float float1) {
        if (Astor.moduleManager == null) {
            return float1;
        } else {
            NoHurtCam noHurtCam = (NoHurtCam) Astor.moduleManager.modules.get(NoHurtCam.class);
            return noHurtCam.isEnabled() ? float1 * (float) noHurtCam.multiplier.getValue().intValue() / 100.0F : float1;
        }
    }

    @ModifyConstant(
            method = {"getMouseOver"},
            constant = {@Constant(
                    doubleValue = 3.0,
                    ordinal = 1
            )}
    )
    private double getMouseOver(double range) {
        PickEvent event = new PickEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    @ModifyVariable(
            method = {"getMouseOver"},
            at = @At("STORE"),
            name = {"d0"}
    )
    private double storeMouseOver(double range) {
        RaytraceEvent event = new RaytraceEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    @Inject(
            method = {"getMouseOver"},
            at = {@At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I",
                    ordinal = 0
            )},
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void a(
            float float1,
            CallbackInfo callbackInfo,
            Entity entity,
            double double4,
            double double5,
            Vec3 vec36,
            boolean boolean7,
            int integer8,
            Vec3 vec39,
            Vec3 vec310,
            Vec3 vec311,
            float float12,
            List<Entity> list,
            double double14,
            int integer15
    ) {
        if (Astor.moduleManager != null) {
            GhostHand event = (GhostHand) Astor.moduleManager.modules.get(GhostHand.class);
            if (event.isEnabled()) {
                list.removeIf(event::shouldSkip);
            }
        }
    }

    /**
     * NoRender's ViewBobbing. Scoped to setupCameraTransform on purpose: setupViewBobbing is called
     * from renderHand as well, once around the hand itself and once around the overlays, and those
     * two are left alone so the hand keeps bobbing while the view stops.
     */
    @Redirect(
            method = {"setupCameraTransform"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;setupViewBobbing(F)V"
            )
    )
    private void setupViewBobbing(EntityRenderer entityRenderer, float partialTicks) {
        NoRender noRender = NoRender.get();
        if (noRender == null || !noRender.viewBobbing.getValue()) {
            this.setupViewBobbing(partialTicks);
        }
    }

    @Redirect(
            method = {"orientCamera"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"
            )
    )
    private double v(Vec3 vec31, Vec3 vec32) {
        if (Astor.moduleManager == null) {
            return vec31.distanceTo(vec32);
        } else {
            return Astor.moduleManager.modules.get(ViewClip.class).isEnabled() ? (double) this.thirdPersonDistance : vec31.distanceTo(vec32);
        }
    }

    @Redirect(
            method = {"setupFog"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;"
            )
    )
    private Material x(Block block) {
        if (Astor.moduleManager == null) {
            return block.getMaterial();
        } else {
            if (Astor.moduleManager.modules.get(ViewClip.class).isEnabled()) {
                return Material.air;
            }
            return NoRender.removesFluidFog(block.getMaterial()) ? Material.air : block.getMaterial();
        }
    }

    @Redirect(
            method = {"updateFogColor"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;"
            )
    )
    private Material updateFogColor(Block block) {
        if (NoRender.removesFluidFog(block.getMaterial())) {
            return Material.air;
        }
        return block.getMaterial();
    }

    @Redirect(
            method = {"updateFogColor"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean y(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.blindness && Astor.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Astor.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityLivingBase).getActivePotionsMap().containsKey(potion.id);
    }

    /**
     * Hands every biome to vanilla's cold branch while Ambience is set to Snow.
     *
     * <p>1.8.9 has no snow field. renderRainSnow() reads the biome temperature and splits on a bare
     * {@code >= 0.15F} - at or above is rain, below is snow - and getRainStrength() only decides
     * whether precipitation is drawn at all. Overriding this one temperature lookup therefore puts the
     * rendering on the real snow path, texture, UV scroll, alpha and lightmap included, instead of
     * swapping the texture on a path that was shaped for rain.</p>
     */
    @Redirect(
            method = {"renderRainSnow"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/biome/WorldChunkManager;getTemperatureAtHeight(FI)F"
            )
    )
    private float snowTemperature(WorldChunkManager manager, float biomeTemperature, int height) {
        Ambience ambience = Ambience.get();
        if (ambience != null && ambience.isSnow()) {
            return Ambience.SNOW_TEMPERATURE;
        }
        return manager.getTemperatureAtHeight(biomeTemperature, height);
    }

    /**
     * Snow gets neither splashes nor the rain sound, so the whole splash pass is dropped while it is
     * snowing.
     *
     * <p>Cancelling addRainParticles() is equivalent to turning the rain-splash option off, because
     * splashes and the rain sound are the only things the method does. Going through the option itself
     * would have meant returning its value from a redirect, which is not possible here - Config lives
     * outside the compile classpath - and answering unconditionally would quietly override whatever
     * the player has that option set to.</p>
     */
    @Inject(method = {"addRainParticles"}, at = {@At("HEAD")}, cancellable = true)
    private void snowSkipsSplashes(CallbackInfo callbackInfo) {
        Ambience ambience = Ambience.get();
        if (ambience != null && ambience.isSnow()) {
            callbackInfo.cancel();
        }
    }

    @Redirect(
            method = {"setupFog"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean q(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.blindness && Astor.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Astor.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityLivingBase).getActivePotionsMap().containsKey(potion.id);
    }

    @Redirect(
            method = {"setupCameraTransform"},
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",
                    remap = false
            )
    )
    private void perspectiveCameraTransform(float fov, float aspect, float near, float far) {
        this.perspective(fov, aspect, near, far);
    }

    @Redirect(
            method = {"renderWorldPass"},
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",
                    remap = false
            )
    )
    private void perspectiveWorldPass(float fov, float aspect, float near, float far) {
        this.perspective(fov, aspect, near, far);
    }

    @Redirect(
            method = {"renderCloudsCheck"},
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",
                    remap = false
            )
    )
    private void perspectiveClouds(float fov, float aspect, float near, float far) {
        this.perspective(fov, aspect, near, far);
    }

    @Redirect(
            method = {"renderHand"},
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",
                    remap = false
            )
    )
    private void perspectiveHand(float fov, float aspect, float near, float far) {
        this.perspective(fov, aspect, near, far);
    }

    private void perspective(float fov, float aspect, float near, float far) {
        float resolved = aspect;
        if (Astor.moduleManager != null) {
            AspectRatio aspectRatio = (AspectRatio) Astor.moduleManager.modules.get(AspectRatio.class);
            if (aspectRatio != null && aspectRatio.isEnabled()) {
                resolved = aspectRatio.getAspect(aspect);
            }
        }
        Project.gluPerspective(fov, resolved, near, far);
    }

    @Redirect(
            method = {"setupCameraTransform"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean c(EntityPlayerSP entityPlayerSP, Potion potion) {
        if (potion == Potion.confusion && Astor.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Astor.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.nausea.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityPlayerSP).getActivePotionsMap().containsKey(potion.id);
    }
}