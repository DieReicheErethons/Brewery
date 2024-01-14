package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BSealer;
import com.dre.brewery.P;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SealCommand implements SubCommand {
    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        if (!P.use1_13) {
            P.p.msg(sender, "Sealing requires minecraft 1.13 or higher");
            return;
        }
        Player player = (Player) sender;

        player.openInventory(new BSealer(player).getInventory());
    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.seal";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
