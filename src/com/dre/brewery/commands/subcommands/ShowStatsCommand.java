package com.dre.brewery.commands.subcommands;

import com.dre.brewery.*;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.recipe.BRecipe;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ShowStatsCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        //if (sender instanceof ConsoleCommandSender && !sender.isOp()) return;

        BreweryPlugin.getInstance().msg(sender, "Drunk Players: " + BPlayer.numDrunkPlayers());
        BreweryPlugin.getInstance().msg(sender, "Brews created: " + BreweryPlugin.getInstance().stats.brewsCreated);
        BreweryPlugin.getInstance().msg(sender, "Barrels built: " + Barrel.barrels.size());
        BreweryPlugin.getInstance().msg(sender, "Cauldrons boiling: " + BCauldron.bcauldrons.size());
        BreweryPlugin.getInstance().msg(sender, "Number of Recipes: " + BRecipe.getAllRecipes().size());
        BreweryPlugin.getInstance().msg(sender, "Wakeups: " + Wakeup.wakeups.size());
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.showstats";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
