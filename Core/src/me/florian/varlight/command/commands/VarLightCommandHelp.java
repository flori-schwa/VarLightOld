package me.florian.varlight.command.commands;

import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightCommand;
import me.florian.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

public class VarLightCommandHelp implements VarLightSubCommand {

    private final VarLightCommand baseCommand;

    public VarLightCommandHelp(VarLightCommand baseCommand) {
        this.baseCommand = baseCommand;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public void sendHelp(CommandSender sender) {
        sender.sendMessage("/varlight help: List all varlight commands");
        sender.sendMessage("/varlight help <command>: show more info on /varlight <command>");
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {

        if (args.hasNext()) {
            VarLightSubCommand subCommand = baseCommand.getRegisteredCommands().get(args.next());

            if (subCommand == null) {
                VarLightSubCommand.sendPrefixedMessage(sender, String.format("The subcommand \"/varlight %s\" does not exist", args.previous()));
                return true;
            }

            subCommand.sendHelp(sender);
            return true;
        }

        listAllSubCommands(sender);
        return true;
    }

    public void listAllSubCommands(CommandSender sender) {
        VarLightSubCommand.sendPrefixedMessage(sender, "VarLight command help:");

        for (VarLightSubCommand subCommand : baseCommand.getRegisteredCommands().values()) {
            subCommand.sendHelp(sender);
        }
    }
}
