package me.shawlaf.varlight.command;

import me.shawlaf.varlight.VarLightPlugin;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CommandSuggestions {

    private final VarLightPlugin plugin;
    private final CommandSender commandSender;
    private final String[] args;

    private final List<String> suggestions = new ArrayList<>();

    public CommandSuggestions(VarLightPlugin plugin, CommandSender commandSender, String[] args) {
        this.plugin = plugin;
        this.commandSender = commandSender;
        this.args = args;
    }

    public CommandSender getCommandSender() {
        return commandSender;
    }

    public String[] getArgs() {
        return args;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public int getArgumentCount() {
        return args.length;
    }

    public String getCurrentArgument() {
        return args[args.length - 1];
    }

    public CommandSuggestions addSuggestion(String suggestion) {
        return addSuggestion(suggestion, true);
    }

    public CommandSuggestions addSuggestion(String suggestion, boolean checkArgument) {
        if (checkArgument && !suggestion.startsWith(getCurrentArgument())) {
            return this;
        }

        suggestions.add(suggestion);
        return this;
    }

    public CommandSuggestions suggestBlockPosition(int completedCoordinates) {
        return suggestBlockPosition(completedCoordinates, true);
    }

    public CommandSuggestions suggestChoices(Collection<String> choices) {
        for (String choice : choices) {
            addSuggestion(choice);
        }

        return this;
    }

    public CommandSuggestions suggestChoices(String... choices) {
        for (String choice : choices) {
            addSuggestion(choice);
        }

        return this;
    }

    public CommandSuggestions suggestBlockPosition(int completedCoordinates, boolean checkArgument) {
        if (!(commandSender instanceof Player)) {
            return this;
        }

        Player player = (Player) commandSender;

        final int[] coords = getCoordinatesLookingAt(player);

        if (coords.length == 0) {
            return this;
        }

        final int[] toSuggest = new int[3 - completedCoordinates];

        System.arraycopy(coords, completedCoordinates, toSuggest, 0, toSuggest.length);

        for (int i = 0; i < toSuggest.length; i++) {
            StringBuilder builder = new StringBuilder();

            for (int j = 0; j <= i; j++) {
                builder.append(toSuggest[j]);
                builder.append(" ");
            }

            addSuggestion(builder.toString().trim(), checkArgument);
        }

        return this;
    }

    private int[] getCoordinatesLookingAt(Player player) {
        Block targetBlock = plugin.getNmsAdapter().getTargetBlockExact(player, 10);

        if (targetBlock == null) {
            return new int[0];
        }

        return new int[]{
                targetBlock.getX(),
                targetBlock.getY(),
                targetBlock.getZ()
        };
    }
}
