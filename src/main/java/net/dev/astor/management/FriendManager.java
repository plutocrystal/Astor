package net.dev.astor.management;

import net.dev.astor.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class FriendManager extends PlayerFileManager {
    public FriendManager() {
        super(new File("./config/Astor/", "friends.txt"), new Color(ChatColors.DARK_GREEN.toAwtColor()));
    }
}