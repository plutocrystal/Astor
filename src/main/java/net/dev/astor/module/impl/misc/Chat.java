package net.dev.astor.module.impl.misc;

import net.dev.astor.Astor;
import net.dev.astor.module.Category;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;

public class Chat extends Module {
    public final BooleanProperty noBackground = new BooleanProperty("NoBackground", true);
    public final BooleanProperty unlimitedChat = new BooleanProperty("UnlimitedChat", true);

    @Override
    public String getDescription() {
        return "Tweaks the vanilla chat log: drops the box behind the lines and lifts the 100 line scrollback cap.";
    }

    public Chat() {
        super("Chat", Category.MISC, false);
    }

    public static Chat get() {
        if (Astor.moduleManager == null) {
            return null;
        }
        Chat chat = (Chat) Astor.moduleManager.modules.get(Chat.class);
        return chat != null && chat.isEnabled() ? chat : null;
    }
}

