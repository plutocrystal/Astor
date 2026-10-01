package net.dev.astor.command.commands;

import net.dev.astor.Astor;
import net.dev.astor.command.Command;
import net.dev.astor.enums.ChatColors;
import net.dev.astor.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.ArrayList;
import java.util.Arrays;

public class PlayerCommand extends Command {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public PlayerCommand() {
        super(new ArrayList<>(Arrays.asList("playerlist", "players")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ArrayList<String> players = new ArrayList<>();
        for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
            players.add(playerInfo.getGameProfile().getName().replace("§", "&"));
        }
        if (players.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sNo players&r", Astor.clientName));
        } else {
            ChatUtil.sendRaw(
                    String.format(
                            ChatColors.formatColor("%sPlayers:&r %s"),
                            ChatColors.formatColor(Astor.clientName),
                            String.join(", ", players)
                    )
            );
        }
    }
}
