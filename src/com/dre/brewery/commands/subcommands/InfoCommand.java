package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BPlayer;
import com.dre.brewery.P;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class InfoCommand implements SubCommand {

    private final P p;

    public InfoCommand(P p) {
        this.p = p;
    }

    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            if (sender.hasPermission("brewery.cmd.infoOther")) {
                cmdInfo(sender, args[1]);
            } else {
                p.msg(sender, p.languageReader.get("Error_NoPermissions"));
            }
        } else {
            if (sender.hasPermission("brewery.cmd.info")) {
                cmdInfo(sender, null);
            } else {
                p.msg(sender, p.languageReader.get("Error_NoPermissions"));
            }
        }
    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
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
                p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
                return;
            }
        }

        Player player = P.p.getServer().getPlayerExact(playerName);
        BPlayer bPlayer;
        if (player == null) {
            bPlayer = BPlayer.getByName(playerName);
        } else {
            bPlayer = BPlayer.get(player);
        }
        if (bPlayer == null) {
            p.msg(sender, p.languageReader.get("CMD_Info_NotDrunk", playerName));
        } else {
            if (selfInfo) {
                bPlayer.showDrunkeness(player);
            } else {
                p.msg(sender, p.languageReader.get("CMD_Info_Drunk", playerName, "" + bPlayer.getDrunkeness(), "" + bPlayer.getQuality()));
            }
        }

    }
}
