package net.dev.astor.command.commands;

import net.dev.astor.Astor;
import net.dev.astor.command.Command;
import net.dev.astor.module.Module;
import net.dev.astor.module.impl.render.HUD;
import net.dev.astor.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class HideCommand extends Command {
    public HideCommand() {
        super(new ArrayList<>(Arrays.asList("hide", "h")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(
                    String.format("%sUsage: .%s <&omodule&r>&r", Astor.clientName, args.get(0).toLowerCase(Locale.ROOT))
            );
        } else if (!args.get(1).equals("*")) {
            Module module = Astor.moduleManager.getModule(args.get(1));
            if (module == null) {
                ChatUtil.sendFormatted(String.format("%sModule &o%s&r not found&r", Astor.clientName, args.get(1)));
            } else if (module.isHidden()) {
                ChatUtil.sendFormatted(String.format("%s&o%s&r is already hidden in HUD&r", Astor.clientName, module.getName()));
            } else {
                module.setHidden(true);
                ChatUtil.sendFormatted(String.format("%s&o%s&r has been hidden in HUD&r", Astor.clientName, module.getName()));
            }
        } else {
            for (Module module : Astor.moduleManager.modules.values()) {
                module.setHidden(true);
            }
            ChatUtil.sendFormatted(String.format("%sAll modules have been hidden in HUD&r", Astor.clientName));
        }
    }
}