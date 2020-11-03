package com.dre.brewery.api.events.barrel;

import com.dre.brewery.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A Player opens a Barrel by rightclicking it.
 * <p>The PlayerInteractEvent on the Barrel may be cancelled. In that case this never gets called
 * <p>Can be cancelled to silently deny opening the Barrel
 */
public class BarrelAccessEvent extends BarrelEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private final Block clickedBlock;
	private final BlockFace clickedBlockFace;
	private boolean isCancelled;

	public BarrelAccessEvent(Barrel barrel, Player player, Block clickedBlock) {
		this(barrel, player, clickedBlock, BlockFace.UP);
	}

	public BarrelAccessEvent(Barrel barrel, Player player, Block clickedBlock, BlockFace clickedBlockFace) {
		super(barrel);
		this.player = player;
		this.clickedBlock = clickedBlock;
		this.clickedBlockFace = clickedBlockFace;
	}

	/**
	 * Gets the Block that was actually clicked.
	 * <p>For access Permissions getSpigot() should be used
	 */
	public Block getClickedBlock() {
		return clickedBlock;
	}

	/**
	 * Get the clicked Block Face when clicking on the Barrel Block
	 */
	public BlockFace getClickedBlockFace() {
		return clickedBlockFace;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	public Player getPlayer() {
		return player;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	// Required by Bukkit
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
