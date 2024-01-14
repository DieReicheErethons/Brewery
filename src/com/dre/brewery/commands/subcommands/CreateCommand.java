package com.dre.brewery.commands.subcommands;

import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.utility.Tuple;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CreateCommand implements SubCommand {

    private final P p;

    public CreateCommand(P p) {
        this.p = p;
    }

    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            p.msg(sender, p.languageReader.get("Etc_Usage"));
            p.msg(sender, p.languageReader.get("Help_Create"));
            return;
        }
        // Is this just a map?
        Tuple<Brew, Player> brewForPlayer = CommandUtil.getFromCommand(sender, args);

        if (brewForPlayer != null) {
            if (brewForPlayer.b().getInventory().firstEmpty() == -1) {
                p.msg(sender, p.languageReader.get("CMD_Copy_Error", "1"));
                return;
            }

            ItemStack item = brewForPlayer.a().createItem(null);
            if (item != null) {
                brewForPlayer.b().getInventory().addItem(item);
                p.msg(sender, p.languageReader.get("CMD_Created"));
            }
        }
    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
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
