package com.dre.brewery.listeners;

import com.dre.brewery.BCauldron;
import com.dre.brewery.utility.LegacyUtil;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public class CauldronListener implements Listener {

	/**
	 * Water in Cauldron gets filled up: remove BCauldron to disallow unlimited Brews
	 * Water in Cauldron gets removed: remove BCauldron to remove Brew data and stop particles
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCauldronChange(CauldronLevelChangeEvent event) {
		if (LegacyUtil.WATER_CAULDRON == null) {
			// < 1.17
			oldCauldronChange(event);
			return;
		}

		Material currentType = event.getBlock().getType();
		BlockState newState = event.getNewState();
		Material newType = newState.getType();

		if (currentType == Material.WATER_CAULDRON) {
			if (newType != Material.WATER_CAULDRON) {
				// Change from water to anything else
				if (event.getReason() != CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL) {
					BCauldron.remove(event.getBlock());
				}
			} else { // newType == Material.WATER_CAULDRON
				// Water level change
				Levelled oldCauldron = ((Levelled) event.getBlock().getBlockData());
				Levelled newCauldron = ((Levelled) newState.getBlockData());

				// Water Level increased somehow, might be Bucket, Bottle, Rain, etc.
				if (newCauldron.getLevel() > oldCauldron.getLevel()) {
					BCauldron.remove(event.getBlock());
				}
			}
		}
	}


	@SuppressWarnings("deprecation")
	private void oldCauldronChange(CauldronLevelChangeEvent event) {
		if (event.getNewLevel() == 0 && event.getOldLevel() != 0) {
			if (event.getReason() == CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL) {
				return;
			}
			BCauldron.remove(event.getBlock());
		} else if (event.getNewLevel() == 3 && event.getOldLevel() != 3) {
			BCauldron.remove(event.getBlock());
		}
	}
}
