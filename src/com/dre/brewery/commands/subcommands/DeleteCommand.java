package com.dre.brewery.commands.subcommands;

import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeleteCommand implements SubCommand {

    private final P p;

    public DeleteCommand(P p) {
        this.p = p;
    }

    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        ItemStack hand = player.getItemInHand();
        if (hand != null) {
            if (Brew.isBrew(hand)) {
                player.setItemInHand(new ItemStack(Material.AIR));
                return;
            }
        }
        p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));

    }

    @Override
    public List<String> tabComplete(P p, CommandSender sender, String label, String[] args) {
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
