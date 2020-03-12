package com.dre.brewery.listeners;

import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;
import com.dre.brewery.P;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.filedata.BData;
import com.dre.brewery.filedata.DataSave;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {

	private final P p;
	private FileConfiguration worldData;

	public WorldListener(P p) {
		this.p = p;
	}

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		World world = event.getWorld();

		if (worldData == null) {
			worldData = BData.loadWorldData(world.getName().startsWith("DXL_") ? BUtil.getDxlName(world.getName()) : world.getUID().toString(), world, null);
		} else {
			Bukkit.getScheduler().runTaskAsynchronously(p,
				() -> BData.loadWorldData(world.getName().startsWith("DXL_") ? BUtil.getDxlName(world.getName()) : world.getUID().toString(), world, worldData));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		DataSave.save(true);
		Barrel.onUnload(event.getWorld().getName());
		BCauldron.onUnload(event.getWorld().getName());
	}

}
