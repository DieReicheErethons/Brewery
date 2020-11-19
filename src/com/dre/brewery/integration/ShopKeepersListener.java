package com.dre.brewery.integration;

import com.dre.brewery.Brew;
import com.dre.brewery.P;
import com.dre.brewery.filedata.BConfig;
import com.nisovin.shopkeepers.api.events.PlayerOpenUIEvent;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class ShopKeepersListener implements Listener {
	Set<HumanEntity> openedEditors = new HashSet<>();

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShopCreated(PlayerOpenUIEvent event) {
		try {
			if (event.getUIType() == DefaultUITypes.EDITOR() || event.getUIType() == DefaultUITypes.TRADING()) {
				openedEditors.add(event.getPlayer());
			}
		} catch (Throwable e) {
			failed(e);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClickShopKeeper(InventoryClickEvent event) {
		if (openedEditors.isEmpty() || !openedEditors.contains(event.getWhoClicked())) {
			return;
		}
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		ItemStack item = event.getCursor();
		if (item != null && item.getType() == Material.POTION && event.getClickedInventory() == event.getView().getTopInventory()) {
			Brew brew = Brew.get(item);
			if (brew != null && !brew.isSealed()) {
				P.p.msg(event.getWhoClicked(), P.p.languageReader.get("Player_ShopSealBrew"));
			}
		}
	}


	@EventHandler
	public void onCloseInventoryShopKeeper(InventoryCloseEvent event) {
		openedEditors.remove(event.getPlayer());
	}

	private void failed(Throwable e) {
		HandlerList.unregisterAll(this);
		BConfig.hasShopKeepers = false;
		e.printStackTrace();
		P.p.errorLog("Failed to notify Player using 'ShopKeepers'. Disabling 'ShopKeepers' support");
		openedEditors.clear();
	}

}
