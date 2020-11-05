package com.dre.brewery;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BSink {
	private final Block block;
	public static Map<Block, BSink> bsinks = new HashMap<>();
	
	public BSink(Block block) {
		this.block = block;
	}
	
	public static void add(Block block, BSink sink) {
		
	}
	
	public static void clickSink(PlayerInteractEvent event) {
		Material materialInHand = event.getMaterial();
		ItemStack item = event.getItem();
		Block clickedBlock = event.getClickedBlock();
		
		if (materialInHand == Material.POTION) {
			BSink sink = bsinks.get(clickedBlock);
			
			if (sink == null) {
				sink = new BSink(clickedBlock);
				bsinks.put(clickedBlock, sink);
			}
			
			item.setAmount(0);
			BCauldron.setItemInHand(event, Material.GLASS_BOTTLE, false);
			event.setCancelled(true);
		}
	}
}
