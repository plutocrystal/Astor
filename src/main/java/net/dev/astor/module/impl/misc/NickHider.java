package net.dev.astor.module.impl.misc;

import net.dev.astor.module.Category;
import net.dev.astor.enums.ChatColors;
import net.dev.astor.module.Module;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.TextProperty;
import net.minecraft.client.Minecraft;

import java.util.regex.Matcher;

public class NickHider extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final TextProperty protectName = new TextProperty("Name", "You");
    public final BooleanProperty scoreboard = new BooleanProperty("Scoreboard", true);
    public final BooleanProperty level = new BooleanProperty("Level", true);

    public NickHider() {
        super("NickHider", Category.MISC, false, true);
    }

    public String replaceNick(String input) {
        if (input != null && mc.thePlayer != null) {
            if (this.scoreboard.getValue() && input.matches("§7\\d{2}/\\d{2}/\\d{2}(?:\\d{2})?  ?§8.*")) {
                input = input.replaceAll("§8", "§8§k").replaceAll("[^\\x00-\\x7F§]", "?");
            }
            return input.replaceAll(
                    mc.thePlayer.getName(), Matcher.quoteReplacement(ChatColors.formatColor(this.protectName.getValue()))
            );
        } else {
            return input;
        }
    }
}
