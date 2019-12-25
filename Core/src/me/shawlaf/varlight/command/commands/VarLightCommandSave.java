package me.shawlaf.varlight.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.varlight.VarLightPlugin;
import me.shawlaf.varlight.command.VarLightSubCommand;
import me.shawlaf.varlight.persistence.WorldLightSourceManager;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static me.shawlaf.command.brigadier.argument.WorldArgumentType.world;
import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.varlight.command.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.command.VarLightCommand.SUCCESS;

public class VarLightCommandSave extends VarLightSubCommand {

    private static final RequiredArgumentBuilder<CommandSender, World> ARG_WORLD = argument("world", world());

    public VarLightCommandSave(VarLightPlugin plugin) {
        super(plugin, "save");
    }

    @NotNull
    @Override
    public String getSyntax() {
        return " [all|<world>]";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Save all custom light sources in the current world, the specified world or all worlds";
    }

    @Override
    public @NotNull String getRequiredPermission() {
        return "varlight.admin.save";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> node) {

        node.executes(this::saveImplicit);
        node.then(LiteralArgumentBuilder.<CommandSender>literal("all").executes(this::saveAll));
        node.then(ARG_WORLD.executes(this::saveExplicit));

        return node;
    }

    private int saveImplicit(CommandContext<CommandSender> context) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "Only players may use this command!");

            return FAILURE;
        }

        Player player = (Player) context.getSource();

        WorldLightSourceManager manager = plugin.getManager(player.getWorld());

        if (manager != null) {
            manager.save(player);

            return SUCCESS;
        } else {
            failure(this, player, String.format("Varlight is not active in world \"%s\"", player.getWorld().getName()));

            return FAILURE;
        }
    }

    private int saveAll(CommandContext<CommandSender> context) {
        for (WorldLightSourceManager manager : plugin.getAllManagers()) {
            manager.save(context.getSource());
        }

        return SUCCESS;
    }

    private int saveExplicit(CommandContext<CommandSender> context) {
        World world = context.getArgument(ARG_WORLD.getName(), World.class);
        WorldLightSourceManager manager = plugin.getManager(world);

        if (manager == null) {
            failure(this, context.getSource(), String.format("Varlight is not active in world \"%s\"", world.getName()));

            return FAILURE;
        }

        manager.save(context.getSource());

        return SUCCESS;
    }

}
