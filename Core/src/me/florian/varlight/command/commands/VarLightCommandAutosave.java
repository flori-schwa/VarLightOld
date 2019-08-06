package me.florian.varlight.command.commands;

import me.florian.varlight.VarLightPlugin;
import me.florian.varlight.command.ArgumentIterator;
import me.florian.varlight.command.VarLightCommand;
import me.florian.varlight.command.VarLightSubCommand;
import me.florian.varlight.command.exception.VarLightCommandException;
import org.bukkit.command.CommandSender;

public class VarLightCommandAutosave extends VarLightSubCommand {

    private final VarLightPlugin plugin;

    public VarLightCommandAutosave(VarLightPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "autosave";
    }

    @Override
    public String getSyntax() {
        return " <new interval>";
    }

    @Override
    public String getDescription() {
        return "Sets the new autosave interval (0: disable, negative: save on world save)";
    }

    @Override
    public boolean execute(CommandSender sender, ArgumentIterator args) {
        VarLightCommand.assertPermission(sender, "varlight.admin.save");

        if (!args.hasNext()) {
            return false;
        }

        int newInterval;

        try {
            newInterval = args.parseNext(Integer::parseInt);
        } catch (NumberFormatException e) {
            throw new VarLightCommandException(String.format("Malformed input: %s", e.getMessage()), e);
        }

        plugin.getConfiguration().setAutosaveInterval(newInterval);
        plugin.initAutosave();

        if (newInterval > 0) {
            VarLightCommand.broadcastResult(sender, String.format("Updated Autosave interval to %d Minutes", newInterval), "varlight.admin.save");
        } else if (newInterval == 0) {
            VarLightCommand.broadcastResult(sender, "Disabled Autosave", "varlight.admin.save");
        } else {
            VarLightCommand.broadcastResult(sender, "Enabled Persist On Save", "varlight.admin.save");
        }

        return true;
    }
}
