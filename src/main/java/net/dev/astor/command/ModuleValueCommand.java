package net.dev.astor.command;

import net.dev.astor.Astor;
import net.dev.astor.module.Module;
import net.dev.astor.property.Property;
import net.dev.astor.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModuleValueCommand extends Command {
    public ModuleValueCommand() {
        super(new ArrayList<>(Arrays.asList("set")));
    }

    public boolean runOnUnknownCommand(List<String> args) {
        if (args.size() < 2) {
            return false;
        }
        Module module = Astor.moduleManager.getModule(args.get(0));
        if (module == null) {
            return false;
        }
        Property<?> property = Astor.propertyManager.getProperty(module, args.get(1));
        if (property == null) {
            ChatUtil.sendFormatted(
                    String.format("%s%s has no property &o%s&r", Astor.clientName, module.getName(), args.get(1))
            );
            return true;
        }
        if (args.size() < 3) {
            ChatUtil.sendFormatted(
                    String.format(
                            "%s%s: &o%s&r is set to %s&r (%s)&r",
                            Astor.clientName, module.getName(), property.getName(),
                            property.formatValue(), property.getValuePrompt()
                    )
            );
            return true;
        }
        String newValue = String.join(" ", args.subList(2, args.size()));
        try {
            if (property.parseString(newValue)) {
                ChatUtil.sendFormatted(
                        String.format(
                                "%s%s: &o%s&r has been set to %s&r",
                                Astor.clientName, module.getName(), property.getName(), property.formatValue()
                        )
                );
                return true;
            }
        } catch (Exception e) {
            ChatUtil.sendFormatted(
                    String.format(
                            "%sInvalid value for property &o%s&r (%s)&r",
                            Astor.clientName, property.getName(), property.getValuePrompt()
                    )
            );
            return true;
        }
        ChatUtil.sendFormatted(
                String.format(
                        "%sInvalid value for property &o%s&r (%s)&r",
                        Astor.clientName, property.getName(), property.getValuePrompt()
                )
        );
        return true;
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        this.runOnUnknownCommand(args.subList(1, args.size()));
    }

    }

