package com.dre.brewery.commands.subcommands;

import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CopyCommand implements SubCommand {

    private final P p;

    public CopyCommand(P p) {
        this.p = p;
    }

    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            cmdCopy(sender, p.parseInt(args[1]));
        } else {
            cmdCopy(sender, 1);
        }
    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.copy";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    //@Deprecated but still used?
    public void cmdCopy(CommandSender sender, int count) {
        if (count < 1 || count > 36) {
            p.msg(sender, p.languageReader.get("Etc_Usage"));
            p.msg(sender, p.languageReader.get("Help_Copy"));
            return;
        }
        Player player = (Player) sender;
        ItemStack hand = player.getItemInHand();
        if (hand != null) {
            if (Brew.isBrew(hand)) {
                while (count > 0) {
                    ItemStack item = hand.clone();
                    if (!(player.getInventory().addItem(item)).isEmpty()) {
                        p.msg(sender, p.languageReader.get("CMD_Copy_Error", "" + count));
                        return;
                    }
                    count--;
                }
                return;
            }
        }

        p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));

    }
}
