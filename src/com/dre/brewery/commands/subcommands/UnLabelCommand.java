package com.dre.brewery.commands.subcommands;

import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.api.events.brew.BrewModifyEvent;
import com.dre.brewery.commands.SubCommand;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class UnLabelCommand implements SubCommand {
    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.AIR) {
            Brew brew = Brew.get(hand);
            if (brew != null) {
                if (!brew.isUnlabeled()) {
                    ItemMeta origMeta = hand.getItemMeta();
                    brew.unLabel(hand);
                    brew.touch();
                    ItemMeta meta = hand.getItemMeta();
                    assert meta != null;
                    BrewModifyEvent modifyEvent = new BrewModifyEvent(brew, meta, BrewModifyEvent.Type.UNLABEL);
                    P.p.getServer().getPluginManager().callEvent(modifyEvent);
                    if (modifyEvent.isCancelled()) {
                        hand.setItemMeta(origMeta);
                        return;
                    }
                    brew.save(meta);
                    hand.setItemMeta(meta);
                    p.msg(sender, p.languageReader.get("CMD_UnLabel"));
                    return;
                } else {
                    p.msg(sender, p.languageReader.get("Error_AlreadyUnlabeled"));
                    return;
                }
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
        return "brewery.cmd.unlabel";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
