package com.dre.brewery.listeners;

import com.dre.brewery.BPlayer;
import com.dre.brewery.BUtil;
import com.dre.brewery.Barrel;
import com.dre.brewery.P;
import com.dre.brewery.DistortChat;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;

public class BlockListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		String[] lines = event.getLines();

		if (lines[0].equalsIgnoreCase("Barrel") || lines[0].equalsIgnoreCase(P.p.languageReader.get("Etc_Barrel"))) {
			Player player = event.getPlayer();
			if (!player.hasPermission("brewery.createbarrel.small") && !player.hasPermission("brewery.createbarrel.big")) {
				P.p.msg(player, P.p.languageReader.get("Perms_NoBarrelCreate"));
				return;
			}
			if (Barrel.create(event.getBlock(), player)) {
				P.p.msg(player, P.p.languageReader.get("Player_BarrelCreated"));
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onSignChangeLow(SignChangeEvent event) {
		if (DistortChat.doSigns) {
			if (BPlayer.hasPlayer(event.getPlayer())) {
				DistortChat.signWrite(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!BUtil.blockDestroy(event.getBlock(), event.getPlayer(), BarrelDestroyEvent.Reason.PLAYER)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		BUtil.blockDestroy(event.getBlock(), null, BarrelDestroyEvent.Reason.BURNED);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (event.isSticky()) {
			Block block = event.getRetractLocation().getBlock();

			if (Barrel.get(block) != null) {
				event.setCancelled(true);
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
