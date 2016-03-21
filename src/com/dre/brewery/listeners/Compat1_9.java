package com.dre.brewery.listeners;

import com.dre.brewery.Brew;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

// Workaround to remove unwanted potion effects
public class Compat1_9 implements Listener {

    @EventHandler
    public void onPlayerDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Brew brew = Brew.get(item);
        if (brew == null) {
            return;
        }

        if (item.getType() == Material.POTION) {
            try {
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                meta.setMainEffect(null);
                item.setItemMeta(meta);
            } catch (Exception exception) {}
        }
    }

}
