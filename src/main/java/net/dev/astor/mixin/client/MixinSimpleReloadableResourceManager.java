package net.dev.astor.mixin.client;

import net.dev.astor.util.AstorResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(value = {SimpleReloadableResourceManager.class})
public abstract class MixinSimpleReloadableResourceManager {
    @Inject(
            method = {"reloadResources"},
            at = {@At("HEAD")}
    )
    private void astor$addAssetPack(List<IResourcePack> packs, CallbackInfo callbackInfo) {
        packs.add(AstorResourcePack.getInstance());
    }
}