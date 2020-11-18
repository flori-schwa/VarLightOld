package me.shawlaf.varlight.spigot.command.commands.config;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shawlaf.varlight.spigot.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;

import static me.shawlaf.command.result.CommandResult.info;
import static me.shawlaf.command.result.CommandResult.successBroadcast;
import static me.shawlaf.varlight.spigot.command.VarLightCommand.SUCCESS;

public class PermissionExecutor extends SubCommandExecutor {

    public PermissionExecutor(VarLightSubCommand command) {
        super(command);
    }

    int executeGet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        boolean doCheckPermission = plugin.getConfiguration().isCheckingPermission();

        if (doCheckPermission) {
            info(command, context.getSource(), String.format("Only players with the \"varlight.use\" permission node may %s to update Light sources", plugin.getKey(plugin.getLightUpdateItem()).getKey()));
        } else {
            info(command, context.getSource(), "There is currently no permisison requirement to use plugin features.");
        }

        return SUCCESS;
    }

    int executeSet(CommandContext<CommandSender> context) throws CommandSyntaxException {

        boolean newValue = context.getArgument("value", boolean.class);

        if (newValue == plugin.getConfiguration().isCheckingPermission()) {
            info(command, context.getSource(), "Nothing changed.");
        } else {
            plugin.getConfiguration().setCheckPermission(newValue);

            if (newValue) {
                successBroadcast(command, context.getSource(), String.format("Enabled permission checking, only players with the \"varlight.use\" permission node may use %s to update Light sources", plugin.getKey(plugin.getLightUpdateItem()).getKey()));
            } else {
                successBroadcast(command, context.getSource(), String.format("Disabled permission checking, all players may use %s to update Light sources", plugin.getKey(plugin.getLightUpdateItem()).getKey()));
            }
        }


        return SUCCESS;
    }
}
