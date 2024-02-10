package com.dre.brewery.integration;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.integration.item.SlimefunPluginItem;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.LegacyUtil;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

public class SlimefunListener implements Listener {

	/**
	 * Catch the Slimefun Right Click event, to cancel it if the right click was on a Cauldron.
	 * This prevents item consumption while adding to the cauldron
	 */
	@EventHandler
	public void onCauldronClickSlimefun(PlayerRightClickEvent event) {
		try {
			if (event.getClickedBlock().isPresent() && event.getHand() == EquipmentSlot.HAND) {
				if (LegacyUtil.isWaterCauldron(event.getClickedBlock().get().getType())) {
					Optional<SlimefunItem> slimefunItem = event.getSlimefunItem();
					if (slimefunItem.isPresent()) {
						for (RecipeItem rItem : BCauldronRecipe.acceptedCustom) {
							if (rItem instanceof SlimefunPluginItem) {
								if (slimefunItem.get().getId().equalsIgnoreCase(((SlimefunPluginItem) rItem).getItemId())) {
									event.cancel();
									BreweryPlugin.getInstance().playerListener.onPlayerInteract(event.getInteractEvent());
									return;
								}
							}
						}
					}
				}
			}
		} catch (Throwable e) {
			HandlerList.unregisterAll(this);
			BreweryPlugin.getInstance().errorLog("Slimefun check failed");
			e.printStackTrace();
		}
	}
}
