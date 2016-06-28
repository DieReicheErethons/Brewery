package com.dre.brewery.listeners;

import com.dre.brewery.BCauldron;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public class CauldronListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCauldronChange(CauldronLevelChangeEvent event) {
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
