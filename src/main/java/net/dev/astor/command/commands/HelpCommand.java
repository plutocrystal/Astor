package net.dev.astor.command.commands;

import net.dev.astor.Astor;
import net.dev.astor.command.Command;
import net.dev.astor.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class HelpCommand extends Command {
    public HelpCommand() {
        super(new ArrayList<>(Arrays.asList("help", "commands")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!Astor.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sCommands:&r", Astor.clientName));
            for (Command command : Astor.commandManager.commands) {
                if (!(command instanceof ModuleCommand)) {
                    ChatUtil.sendFormatted(String.format("&7»&r .%s&r", String.join(" &7/&r .", command.names)));
                }
            }
        }
    }
}
