package net.dev.astor.command;

import net.dev.astor.Astor;
import net.dev.astor.event.EventTarget;
import net.dev.astor.event.types.EventType;
import net.dev.astor.event.types.Priority;
import net.dev.astor.event.events.impl.network.PacketEvent;
import net.dev.astor.module.Module;
import net.dev.astor.util.ChatUtil;
import net.minecraft.network.play.client.C01PacketChatMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CommandManager {
    public ArrayList<Command> commands;
    private final ModuleValueCommand moduleValueCommand = new ModuleValueCommand();
    private boolean tabCompleting;
    private String lastBasePrefix = "";
    private String lastReturnedWord = "";
    private List<String> tabList = new ArrayList<>();
    private int tabIndex;

    public CommandManager() {
        this.commands = new ArrayList<>();
    }

    public void handleCommand(String string) {
        List<String> params = Arrays.asList(string.substring(1).trim().split("\\s+"));
        ArrayList<String> arrayList = new ArrayList<>(params);
        if (params.get(0).isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sUnknown command&r", Astor.clientName).replace("&", "§"));
        } else {
            Command command = this.findCommand(params.get(0));
            if (command != null) {
                command.runCommand(arrayList);
                return;
            }
            if (this.moduleValueCommand.runOnUnknownCommand(arrayList)) {
                return;
            }
            ChatUtil.sendFormatted(String.format("%sUnknown command (&o%s&r)&r", Astor.clientName, params.get(0)).replace("&", "§"));
        }
    }

    public Command findCommand(String name) {
        for (Command command : this.commands) {
            for (String commandName : command.names) {
                if (commandName.equalsIgnoreCase(name)) {
                    return command;
                }
            }
        }
        return null;
    }

    /**
     * The word Tab should complete the current token to, or null to leave the box alone.
     *
     * Only the token after the last space is returned, since that is all the caller has to swap in.
     * Repeated presses on an unchanged token walk the candidate list rather than handing back the
     * first one forever, which is what makes holding Tab cycle through them.
     */
    public String getTabComplete(String input) {
        if (input == null || !this.isTypingCommand(input)) {
            this.reset();
            return null;
        }
        int lastSpace = input.lastIndexOf(" ");
        String prefix = lastSpace == -1 ? "" : input.substring(0, lastSpace + 1);
        boolean commandName = lastSpace == -1;
        String current = commandName ? input.substring(1) : input.substring(lastSpace + 1);

        // Returning the whole line rather than just the token is what keeps the cycle alive. The caller
        // puts back exactly what was handed out, so the next press sees the same prefix and the same
        // completed token as last time, and the cached list is still valid. Handing back only the token
        // would leave the caller rebuilding the line itself, and the box text after a completion
        // narrows the candidate list to that one entry, throwing the cycle away on every press.
        //
        // The list can be empty here when the previous press found nothing: lastReturnedWord is set to
        // the input in that case, so a second Tab on the same text lands on this branch with nothing
        // to hand out. Staying quiet is the answer, indexing an empty list is not.
        if (this.tabCompleting && prefix.equals(this.lastBasePrefix) && input.equals(this.lastReturnedWord)) {
            return this.tabList.isEmpty() ? null : this.nextCompletion(prefix, commandName);
        }

        this.tabCompleting = true;
        this.lastBasePrefix = prefix;
        this.tabList = this.collectCompletions(input, current, commandName);
        this.tabIndex = 0;
        if (this.tabList.isEmpty()) {
            this.lastReturnedWord = input;
            return null;
        }
        return this.nextCompletion(prefix, commandName);
    }

    private void reset() {
        this.tabCompleting = false;
        this.tabList = new ArrayList<>();
        this.tabIndex = 0;
        this.lastBasePrefix = "";
        this.lastReturnedWord = "";
    }

    private String nextCompletion(String prefix, boolean commandName) {
        if (this.tabIndex >= this.tabList.size()) {
            this.tabIndex = 0;
        }
        String completed = this.tabList.get(this.tabIndex++);
        this.lastReturnedWord = prefix + (commandName ? "." + completed : completed);
        return this.lastReturnedWord;
    }

    private List<String> collectCompletions(String input, String current, boolean commandName) {
        List<String> candidates = new ArrayList<>();
        if (commandName) {
            for (Command command : this.commands) {
                candidates.addAll(command.names);
            }
            if (Astor.moduleManager != null) {
                for (Module module : Astor.moduleManager.modules.values()) {
                    candidates.add(module.getName());
                }
            }
        } else {
            String[] split = input.substring(1).split(" ", -1);
            String[] args = Arrays.copyOfRange(split, 1, split.length);
            Command command = this.findCommand(split[0]);
            if (command != null) {
                candidates.addAll(command.complete(args));
            } else {
                candidates.addAll(this.moduleValueCommand.complete(args));
            }
        }
        String lower = current.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    public boolean isTypingCommand(String string) {
        if (string == null || string.length() < 2) {
            return false;
        } else {
            return string.charAt(0) == '.' && Character.isLetterOrDigit(string.charAt(1));
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C01PacketChatMessage) {
            String msg = ((C01PacketChatMessage) event.getPacket()).getMessage();
            if (this.isTypingCommand(msg)) {
                event.setCancelled(true);
                this.handleCommand(msg);
            }
        }
    }
}
