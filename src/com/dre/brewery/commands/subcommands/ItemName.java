package com.dre.brewery.commands.subcommands;

import com.dre.brewery.P;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public class ItemName implements SubCommand {
    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        @SuppressWarnings("deprecation")
        ItemStack hand = P.use1_9 ? player.getInventory().getItemInMainHand() : player.getItemInHand();
        if (hand != null) {
            p.msg(sender, p.languageReader.get("CMD_Configname", hand.getType().name().toLowerCase(Locale.ENGLISH)));
        } else {
            p.msg(sender, p.languageReader.get("CMD_Configname_Error"));
        }
    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.itemname";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
