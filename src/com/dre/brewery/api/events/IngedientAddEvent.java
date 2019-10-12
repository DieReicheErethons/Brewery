package com.dre.brewery.api.events;

import com.dre.brewery.BCauldron;
import com.dre.brewery.LegacyUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Cauldron;
import org.bukkit.material.MaterialData;

/*
 * Player adding an ingredient to a cauldron
 * Always one item added at a time
 * If needed use the caudrons add method to manually add more Items
 */
public class IngedientAddEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Block block;
	private final BCauldron cauldron;
	private ItemStack ingredient;
	private boolean cancelled;
	private boolean takeItem = true;

	public IngedientAddEvent(Player who, Block block, BCauldron bCauldron, ItemStack ingredient) {
		super(who);
		this.block = block;
		cauldron = bCauldron;
		this.ingredient = ingredient.clone();
	}

	public Block getBlock() {
		return block;
	}

	public BCauldron getCauldron() {
		return cauldron;
	}

	// Get the item currently being added to the cauldron by the player
	// Can be changed directly (mutable) or with the setter Method
	// The amount is ignored and always one added
	public ItemStack getIngredient() {
		return ingredient;
	}

	// Set the ingredient added to the cauldron to something else
	// Will always be accepted, even when not in a recipe or the cooked list
	// The amount is ignored and always one added
	public void setIngredient(ItemStack ingredient) {
		this.ingredient = ingredient;
	}

	// If the amount of the item in the players hand should be decreased
	// Default true
	public boolean willTakeItem() {
		return takeItem;
	}

	// Set if the amount of the item in the players hand should be decreased
	public void setTakeItem(boolean takeItem) {
		this.takeItem = takeItem;
	}

	// Get the BlockData of the Cauldron
	// May be null if the Cauldron does not exist anymore
	public Levelled getCauldronData() {
		BlockData data = block.getBlockData();
		if (data instanceof Levelled) {
			return (Levelled) data;
		}
		return null;
	}

	// Get the Water Fill level of the Cauldron
	// 0 = empty, 1 = something in, 2 = full
	// Can use BCauldron.EMPTY, BCauldron.SOME, BCauldron.FULL
	public byte getFillLevel() {
		return LegacyUtil.getFillLevel(block);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
