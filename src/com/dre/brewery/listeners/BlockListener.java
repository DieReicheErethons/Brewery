package com.dre.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;
import com.dre.brewery.P;

public class BlockListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(SignChangeEvent event) {
		String[] lines = event.getLines();

		if (lines[0].equalsIgnoreCase(P.p.languageReader.get("Etc_Barrel"))) {
			if (Barrel.create(event.getBlock())) {
				P.p.msg(event.getPlayer(), P.p.languageReader.get("Player_BarrelCreated"));
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		// remove cauldron
		if (block.getType() == Material.CAULDRON) {
			if (block.getData() != 0) {
				// will only remove when existing
				BCauldron.remove(block);
			}
			// remove barrel and throw potions on the ground
		} else if (block.getType() == Material.FENCE || block.getType() == Material.NETHER_FENCE) {
			Barrel barrel = Barrel.get(block);
			if (barrel != null) {
				barrel.remove(null);
			}
			// remove small Barrels
		} else if (block.getType() == Material.SIGN || block.getType() == Material.WALL_SIGN) {
			Barrel barrel = Barrel.get(block);
			if (barrel != null) {
				if (!barrel.isLarge()) {
					barrel.remove(null);
				}
			}
		}
	}
}
