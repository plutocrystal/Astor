package net.dev.astor.command;

import net.dev.astor.Astor;
import net.dev.astor.module.Module;
import net.dev.astor.property.Property;
import net.dev.astor.property.properties.BooleanProperty;
import net.dev.astor.property.properties.ModeProperty;
import net.dev.astor.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ".&lt;module&gt; &lt;property&gt; &lt;value&gt;", which works without a command name because the first
 * argument is matched against the module list. Typing a bare module name is the common case, so
 * requiring ".set AutoTool Delay 3" instead would be a step nobody asked for.
 *
 * When this is registered as the "set" command it keeps that name too; handleCommand only falls back
 * to it once no real command has claimed the first argument.
 */
public class ModuleValueCommand extends Command {
    private static final String[] BOOLEAN_VALUES = {"true", "false", "on", "off", "1", "0"};

    public ModuleValueCommand() {
        super(new ArrayList<>(Arrays.asList("set")));
    }

    /**
     * Handles the ".<module> <property> <value>" form. Returns false when the first argument is not a
     * module, so the caller can report an unknown command.
     */
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

    public List<String> moduleNames() {
        List<String> names = new ArrayList<>();
        for (Module module : Astor.moduleManager.modules.values()) {
            names.add(module.getName());
        }
        return names;
    }

    @Override
    public List<String> complete(String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            return moduleNames();
        } else if (args.length == 2) {
            Module module = Astor.moduleManager.getModule(args[0]);
            if (module != null) {
                List<Property<?>> properties = Astor.propertyManager.properties.get(module.getClass());
                if (properties != null) {
                    properties.stream()
                            .filter(Property::isVisible)
                            .map(Property::getName)
                            .forEach(completions::add);
                }
            }
        } else if (args.length == 3) {
            Module module = Astor.moduleManager.getModule(args[0]);
            if (module != null) {
                Property<?> property = Astor.propertyManager.getProperty(module, args[1]);
                if (property instanceof ModeProperty) {
                    completions.addAll(Arrays.asList(((ModeProperty) property).getModes()));
                } else if (property instanceof BooleanProperty) {
                    completions.addAll(Arrays.asList(BOOLEAN_VALUES));
                }
            }
        }
        return completions;
    }
}
