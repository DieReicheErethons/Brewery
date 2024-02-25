package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BPlayer;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class InfoCommand implements SubCommand {

    private final BreweryPlugin breweryPlugin;

    public InfoCommand(BreweryPlugin breweryPlugin) {
        this.breweryPlugin = breweryPlugin;
    }

    @Override
    public void execute(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            if (sender.hasPermission("brewery.cmd.infoOther")) {
                cmdInfo(sender, args[1]);
            } else {
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Error_NoPermissions"));
            }
        } else {
            if (sender.hasPermission("brewery.cmd.info")) {
                cmdInfo(sender, null);
            } else {
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Error_NoPermissions"));
            }
        }
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.info";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    public void cmdInfo(CommandSender sender, String playerName) {

        boolean selfInfo = playerName == null;
        if (selfInfo) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                playerName = player.getName();
            } else {
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("Error_PlayerCommand"));
                return;
            }
        }

        Player player = BreweryPlugin.getInstance().getServer().getPlayerExact(playerName);
        BPlayer bPlayer;
        if (player == null) {
            bPlayer = BPlayer.getByName(playerName);
        } else {
            bPlayer = BPlayer.get(player);
        }
        if (bPlayer == null) {
            breweryPlugin.msg(sender, breweryPlugin.languageReader.get("CMD_Info_NotDrunk", playerName));
        } else {
            if (selfInfo) {
                bPlayer.showDrunkeness(player);
            } else {
                breweryPlugin.msg(sender, breweryPlugin.languageReader.get("CMD_Info_Drunk", playerName, "" + bPlayer.getDrunkeness(), "" + bPlayer.getQuality()));
            }
        }

    }
}
