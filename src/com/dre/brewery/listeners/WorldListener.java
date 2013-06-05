package com.dre.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.World;

import com.dre.brewery.P;
import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;

public class WorldListener implements Listener {

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		P.p.log("loading world with uuid " + event.getWorld().getUID().toString());
		World world = event.getWorld();

		if (world.getName().startsWith("DXL_")) {
			P.p.loadWorldData(P.p.getDxlName(world.getName()), world);
		} else {
			P.p.loadWorldData(event.getWorld().getUID().toString(), world);
		}
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
