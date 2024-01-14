package com.dre.brewery.listeners;

import com.dre.brewery.*;
import com.dre.brewery.P;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.filedata.BData;
import com.dre.brewery.integration.barrel.BlocklockerBarrel;
import com.dre.brewery.utility.BUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

public class BlockListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		String[] lines = event.getLines();

		if (hasBarrelLine(lines)) {
			Player player = event.getPlayer();
			if (!player.hasPermission("brewery.createbarrel.small") && !player.hasPermission("brewery.createbarrel.big")) {
				P.p.msg(player, P.p.languageReader.get("Perms_NoBarrelCreate"));
				return;
			}
			if (BData.dataMutex.get() > 0) {
				P.p.msg(player, "§cCurrently loading Data");
				return;
			}
			if (Barrel.create(event.getBlock(), player)) {
				P.p.msg(player, P.p.languageReader.get("Player_BarrelCreated"));
			}
		}
	}

	public static boolean hasBarrelLine(String[] lines) {
		for (String line : lines) {
			if (line.equalsIgnoreCase("Barrel") || line.equalsIgnoreCase(P.p.languageReader.get("Etc_Barrel"))) {
				return true;
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onSignChangeLow(SignChangeEvent event) {
		if (DistortChat.doSigns) {
			if (BPlayer.hasPlayer(event.getPlayer())) {
				DistortChat.signWrite(event);
			}
		}
		if (BConfig.useBlocklocker) {
			String[] lines = event.getLines();
			if (hasBarrelLine(lines)) {
				BlocklockerBarrel.createdBarrelSign(event.getBlock());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!P.use1_14 || event.getBlock().getType() != Material.SMOKER) return;
		BSealer.blockPlace(event.getItemInHand(), event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!BUtil.blockDestroy(event.getBlock(), event.getPlayer(), BarrelDestroyEvent.Reason.PLAYER)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (!BUtil.blockDestroy(event.getBlock(), null, BarrelDestroyEvent.Reason.BURNED)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (event.isSticky()) {
			for (Block block : event.getBlocks()) {
				if (Barrel.get(block) != null) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		for (Block block : event.getBlocks()) {
			if (Barrel.get(block) != null) {
				event.setCancelled(true);
				return;
			}
		}
	}
}
