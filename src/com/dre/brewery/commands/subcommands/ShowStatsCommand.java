package com.dre.brewery.commands.subcommands;

import com.dre.brewery.*;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.recipe.BRecipe;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ShowStatsCommand implements SubCommand {
    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        //if (sender instanceof ConsoleCommandSender && !sender.isOp()) return;

        P.p.msg(sender, "Drunk Players: " + BPlayer.numDrunkPlayers());
        P.p.msg(sender, "Brews created: " + P.p.stats.brewsCreated);
        P.p.msg(sender, "Barrels built: " + Barrel.barrels.size());
        P.p.msg(sender, "Cauldrons boiling: " + BCauldron.bcauldrons.size());
        P.p.msg(sender, "Number of Recipes: " + BRecipe.getAllRecipes().size());
        P.p.msg(sender, "Wakeups: " + Wakeup.wakeups.size());
    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
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
