package com.dre.brewery.listeners;

import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.filedata.BData;
import com.dre.brewery.filedata.DataSave;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		World world = event.getWorld();

		if (world.getName().startsWith("DXL_")) {
			BData.loadWorldData(BUtil.getDxlName(world.getName()), world);
		} else {
			BData.loadWorldData(world.getUID().toString(), world);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		DataSave.save(true);
		Barrel.onUnload(event.getWorld().getName());
		BCauldron.onUnload(event.getWorld().getName());
	}

}
