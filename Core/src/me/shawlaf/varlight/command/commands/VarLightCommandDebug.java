package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class VarLightCommandDebug extends VarLightSubCommand {

    public VarLightCommandDebug(VarLightPlugin plugin) {
        super(plugin, "varlight-debug", false);
    }

    @Override
    public String getSubCommandName() {
        return "debug";
    }

    @Override
    public String getRequiredPermission() {
        return "varlight.admin.debug";
    }

    @Override
    public String getDescription() {
        return "Lists all custom Light sources in a region or chunk";
    }

    @Override
    public String getSyntax() {
        return " -r|-c [regionX|chunkX] [regionZ|chunkZ]";
    }

    @Override
    protected LiteralArgumentBuilder<CommandSender> buildCommand(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {

        literalArgumentBuilder.requires(sender -> sender instanceof Player);

        literalArgumentBuilder.then(
                LiteralArgumentBuilder.<CommandSender>literal("-r")
                    .executes(context -> {
                        return 0; // TODO
                    })
                .then(
                        RequiredArgumentBuilder.<CommandSender, Integer>argument("regionX", integer())
                        .then(
                                RequiredArgumentBuilder.<CommandSender, Integer>argument("regionZ", integer())
                                .executes(context -> {
                                    return 0;
                                })
                        )
                )
        );

        return null;
    }

    private void listLightSourcesInRegion(CommandSender commandSender, int regionX, int regionZ) {

    }
}
