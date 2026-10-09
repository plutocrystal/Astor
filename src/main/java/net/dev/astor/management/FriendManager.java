package net.dev.astor.management;

import net.dev.astor.config.Config;
import net.dev.astor.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class FriendManager extends PlayerFileManager {
    public FriendManager() {
        super(new File(Config.CONFIG_DIR, "friends.txt"), new Color(ChatColors.DARK_GREEN.toAwtColor()));
    }
}