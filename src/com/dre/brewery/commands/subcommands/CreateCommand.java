package com.dre.brewery.commands.subcommands;

import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.utility.Tuple;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CreateCommand implements SubCommand {

    private final BreweryPlugin breweryPlugin;

    public CreateCommand(BreweryPlugin breweryPlugin) {
        this.breweryPlugin = breweryPlugin;
    }

    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Etc_Usage"));
            breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Help_Create"));
            return;
        }
        // Is this just a map?
        Tuple<Brew, Player> brewForPlayer = CommandUtil.getFromCommand(sender, args);

        if (brewForPlayer != null) {
            if (brewForPlayer.b().getInventory().firstEmpty() == -1) {
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("CMD_Copy_Error", "1"));
                return;
            }

            ItemStack item = brewForPlayer.a().createItem(null);
            if (item != null) {
                brewForPlayer.b().getInventory().addItem(item);
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("CMD_Created"));
            }
        }
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return CommandUtil.tabCreateAndDrink(args);
    }

    @Override
    public String permission() {
        return "brewery.cmd.create";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }


}
