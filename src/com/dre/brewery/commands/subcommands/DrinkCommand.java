package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BPlayer;
import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.utility.Tuple;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DrinkCommand implements SubCommand {
    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            p.msg(sender, p.languageReader.get("Etc_Usage"));
            p.msg(sender, p.languageReader.get("Help_Drink"));
            return;
        }

        Tuple<Brew, Player> brewForPlayer = CommandUtil.getFromCommand(sender, args);
        if (brewForPlayer != null) {
            Player player = brewForPlayer.b();
            Brew brew = brewForPlayer.a();
            String brewName = brew.getCurrentRecipe().getName(brew.getQuality());
            BPlayer.drink(brew, null, player);

            p.msg(player, p.languageReader.get("CMD_Drink", brewName));
            if (!sender.equals(player)) {
                p.msg(sender, p.languageReader.get("CMD_DrinkOther", player.getDisplayName(), brewName));
            }
        }
    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
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
