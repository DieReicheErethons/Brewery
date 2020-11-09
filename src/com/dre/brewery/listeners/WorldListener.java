package com.dre.brewery.listeners;

import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;
import com.dre.brewery.P;
import com.dre.brewery.Wakeup;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.filedata.BData;
import com.dre.brewery.filedata.DataSave;
import com.dre.brewery.utility.BUtil;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldListener implements Listener {

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		final World world = event.getWorld();
		if (BConfig.loadDataAsync) {
			P.p.getServer().getScheduler().runTaskAsynchronously(P.p, () -> lwDataTask(world));
		} else {
			lwDataTask(world);
		}
	}

	private void lwDataTask(World world) {
		if (!BData.acquireDataLoadMutex()) return;  // Tries for 60 sec

		try {
			if (world.getName().startsWith("DXL_")) {
				BData.loadWorldData(BUtil.getDxlName(world.getName()), world);
			} else {
				BData.loadWorldData(world.getUID().toString(), world);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			BData.releaseDataLoadMutex();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		String worldName = event.getWorld().getName();
		if (Barrel.hasDataInWorld(worldName) || BCauldron.hasDataInWorld(worldName)) {
			DataSave.save(true);
			Barrel.onUnload(worldName);
			BCauldron.onUnload(worldName);
		}
		Wakeup.onUnload(worldName);
	}

}
