package net.dev.astor.command.commands;

import net.dev.astor.Astor;
import net.dev.astor.command.Command;
import net.dev.astor.module.Module;
import net.dev.astor.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class ListCommand extends Command {
    public ListCommand() {
        super(new ArrayList<>(Arrays.asList("list", "l", "modules", "astor")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!Astor.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sModules:&r", Astor.clientName));
            for (Module module : Astor.moduleManager.modules.values()) {
                ChatUtil.sendFormatted(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
            }
        }
    }
}
