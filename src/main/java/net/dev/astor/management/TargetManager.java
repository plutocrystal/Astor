package net.dev.astor.management;

import net.dev.astor.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class TargetManager extends PlayerFileManager {
    public TargetManager() {
        super(new File("./config/Astor/", "enemies.txt"), new Color(ChatColors.DARK_RED.toAwtColor()));
    }
}
