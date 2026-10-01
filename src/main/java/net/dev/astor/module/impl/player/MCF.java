package net.dev.astor.module.impl.player;

import net.dev.astor.module.Category;
import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.events.impl.input.KeyEvent;
import net.dev.astor.module.Module;
import net.dev.astor.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class MCF extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public MCF() {
        super("MCF", Category.PLAYER, false, true);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (this.isEnabled() && event.getKey() == -98) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
                String hitName = mc.objectMouseOver.entityHit.getName();
                if (!Astor.friendManager.isFriend(hitName)) {
                    Astor.friendManager.add(hitName);
                    ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your friend list&r", Astor.clientName, hitName));
                } else {
                    Astor.friendManager.remove(hitName);
                    ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your friend list&r", Astor.clientName, hitName));
                }
            }
        }
    }
}
