package com.dre.brewery.commands.subcommands;

import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeleteCommand implements SubCommand {

    private final BreweryPlugin breweryPlugin;

    public DeleteCommand(BreweryPlugin breweryPlugin) {
        this.breweryPlugin = breweryPlugin;
    }

    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        ItemStack hand = player.getItemInHand();
        if (hand != null) {
            if (Brew.isBrew(hand)) {
                player.setItemInHand(new ItemStack(Material.AIR));
                return;
            }
        }
        breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Error_ItemNotPotion"));

    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.delete";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
