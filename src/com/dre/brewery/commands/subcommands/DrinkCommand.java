package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BPlayer;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.utility.Tuple;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DrinkCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Etc_Usage"));
            breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Help_Drink"));
            return;
        }

        Tuple<Brew, Player> brewForPlayer = CommandUtil.getFromCommand(sender, args);
        if (brewForPlayer != null) {
            Player player = brewForPlayer.b();
            Brew brew = brewForPlayer.a();
            String brewName = brew.getCurrentRecipe().getName(brew.getQuality());
            BPlayer.drink(brew, null, player);

            breweryPlugin.msg(player, breweryPlugin.languageReader.get("CMD_Drink", brewName));
            if (!sender.equals(player)) {
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("CMD_DrinkOther", player.getDisplayName(), brewName));
            }
        }
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return CommandUtil.tabCreateAndDrink(args);
    }

    @Override
    public String permission() {
        return "brewery.cmd.drink";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
