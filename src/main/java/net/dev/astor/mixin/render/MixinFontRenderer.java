package net.dev.astor.mixin.render;

import net.dev.astor.Astor;
import net.dev.astor.module.impl.misc.NickHider;
import net.dev.astor.module.impl.render.ClientSetting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(value = {FontRenderer.class}, priority = 9999)
public abstract class MixinFontRenderer {
    @ModifyVariable(
            method = {"renderString"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private String renderString(String string) {
        if (Astor.moduleManager == null) {
            return string;
        } else {
            NickHider nickHider = (NickHider) Astor.moduleManager.modules.get(NickHider.class);
            return nickHider.isEnabled() ? nickHider.replaceNick(string) : string;
        }
    }

    @ModifyVariable(
            method = {"getStringWidth"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private String getStringWidth(String string) {
        if (Astor.moduleManager == null) {
            return string;
        } else {
            NickHider nickHider = (NickHider) Astor.moduleManager.modules.get(NickHider.class);
            return nickHider.isEnabled() ? nickHider.replaceNick(string) : string;
        }
    }

    @Shadow
    protected abstract int renderString(String text, float x, float y, int color, boolean dropShadow);

    /**
     * 1.8.9 的 drawString 把阴影当成一次独立的 renderString 调用，硬编码画在 x+1 / y+1，
     * 所以只重定向 ordinal 0 这一层（阴影），正文层保持原样。
     * 收到的 x 已经是 origX+1，减去 0.4 让阴影落到 origX+0.6，文字看起来更粗更实。
     */
    @Redirect(
            method = {"drawString(Ljava/lang/String;FFIZ)I"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;renderString(Ljava/lang/String;FFIZ)I",
                    ordinal = 0
            )
    )
    private int drawStringShadow(FontRenderer fontRenderer, String text, float x, float y, int color, boolean dropShadow) {
        ClientSetting clientSetting = Astor.moduleManager == null
                ? null
                : (ClientSetting) Astor.moduleManager.modules.get(ClientSetting.class);
        return clientSetting != null && clientSetting.boldShadow.getValue()
                ? this.renderString(text, x - 0.4F, y - 0.4F, color, dropShadow)
                : this.renderString(text, x, y, color, dropShadow);
    }

    @Redirect(
            method = {"getStringWidth"},
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;charAt(I)C",
                    ordinal = 1
            )
    )
    private char getStringWidth(String string, int index) {
        char charAt = string.charAt(index);
        return charAt != '0'
                && charAt != '1'
                && charAt != '2'
                && charAt != '3'
                && charAt != '4'
                && charAt != '5'
                && charAt != '6'
                && charAt != '7'
                && charAt != '8'
                && charAt != '9'
                && charAt != 'a'
                && charAt != 'A'
                && charAt != 'b'
                && charAt != 'B'
                && charAt != 'c'
                && charAt != 'C'
                && charAt != 'd'
                && charAt != 'D'
                && charAt != 'e'
                && charAt != 'E'
                && charAt != 'f'
                && charAt != 'F'
                ? charAt
                : 'r';
    }
}