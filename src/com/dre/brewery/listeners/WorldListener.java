package com.dre.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.dre.brewery.P;
import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;

public class WorldListener implements Listener {

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		P.p.log("loading world with uuid " + event.getWorld().getUID().toString());
		P.p.loadWorldData(event.getWorld().getUID().toString());
	}

	@EventHandler
	public void onWorldUnload(WorldUnloadEvent event) {
		P.p.log("Unloading world with uuid " + event.getWorld().getUID().toString());
		if (!event.isCancelled()) {
			P.p.saveData();
			Barrel.onUnload(event.getWorld().getName());
			BCauldron.onUnload(event.getWorld().getName());
		}
	}

}
