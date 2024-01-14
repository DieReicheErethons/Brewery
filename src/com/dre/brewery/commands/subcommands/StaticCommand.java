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

public class StaticCommand implements SubCommand {
    @Override
    public void execute(P p, CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        ItemStack hand = player.getItemInHand();
        if (hand.getType() != Material.AIR) {
            Brew brew = Brew.get(hand);
            if (brew != null) {
                if (brew.isStatic()) {
                    if (!brew.isStripped()) {
                        brew.setStatic(false, hand);
                        p.msg(sender, p.languageReader.get("CMD_NonStatic"));
                    } else {
                        p.msg(sender, p.languageReader.get("Error_SealedAlwaysStatic"));
                        return;
                    }
                } else {
                    brew.setStatic(true, hand);
                    p.msg(sender, p.languageReader.get("CMD_Static"));
                }
                brew.touch();
                ItemMeta meta = hand.getItemMeta();
                assert meta != null;
                BrewModifyEvent modifyEvent = new BrewModifyEvent(brew, meta, BrewModifyEvent.Type.STATIC);
                P.p.getServer().getPluginManager().callEvent(modifyEvent);
                if (modifyEvent.isCancelled()) {
                    return;
                }
                brew.save(meta);
                hand.setItemMeta(meta);
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
        return "brewery.cmd.static";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
