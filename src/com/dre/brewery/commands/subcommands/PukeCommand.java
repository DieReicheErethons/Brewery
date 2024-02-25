package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BPlayer;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PukeCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        Player player = null;
        if (args.length > 1) {
            player = breweryPlugin.getServer().getPlayer(args[1]);
            if (player == null) {
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Error_NoPlayer", args[1]));
                return;
            }
        }

        if (!(sender instanceof Player) && player == null) {
            breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Error_PlayerCommand"));
            return;
        }
        if (player == null) {
            player = ((Player) sender);
        } else {
            if (!sender.hasPermission("brewery.cmd.pukeOther") && !player.equals(sender)) {
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Error_NoPermissions"));
                return;
            }
        }
        int count = 0;
        if (args.length > 2) {
            count = BreweryPlugin.getInstance().parseInt(args[2]);
        }
        if (count <= 0) {
            count = 20 + (int) (Math.random() * 40);
        }
        BPlayer.addPuke(player, count);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.puke";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}
