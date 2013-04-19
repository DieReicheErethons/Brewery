package com.dre.brewery.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BrewingStand;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BIngredients;

import com.dre.brewery.P;

public class PlayerListener implements Listener{
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerInteract(PlayerInteractEvent event){
		Block clickedBlock = event.getClickedBlock();
		
		if(clickedBlock!=null){
			if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
				if(clickedBlock.getType() == Material.CAULDRON){
					if(clickedBlock.getRelative(BlockFace.DOWN).getType() == Material.FIRE ||
					clickedBlock.getRelative(BlockFace.DOWN).getType() == Material.STATIONARY_LAVA){
						P.p.log("heat underneath cauldron");
						Material materialInHand = event.getMaterial();
						Player player = event.getPlayer();
						ItemStack item = event.getItem();

						if(BIngredients.possibleIngredients.contains(materialInHand)){//add ingredient to cauldron that meet the previous contitions
							if(BCauldron.ingredientAdd(clickedBlock,materialInHand)){
								if(item.getAmount() > 1){
									item.setAmount(item.getAmount() -1);
								} else {
									player.setItemInHand(new ItemStack(0));
								}
							}
						} else if(materialInHand == Material.GLASS_BOTTLE){//fill a glass bottle with potion
							if(BCauldron.fill(player,clickedBlock)){
								P.p.log("custom potion done");
								event.setCancelled(true);
								if(item.getAmount() > 1){
									item.setAmount(item.getAmount() -1);
								} else {
									player.setItemInHand(new ItemStack(0));
								}
							}
						} else if(materialInHand == Material.WATER_BUCKET){//reset cauldron when refilling to prevent unlimited source of potions
							if(clickedBlock.getData() != 0){
								if(clickedBlock.getData() < 3){
									BCauldron.remove(clickedBlock);
								}
							}
						}
					}
				}
			}
		}

	}


//TESTING!

	@EventHandler(priority = EventPriority.HIGH)
	public void onBrew(BrewEvent event){
		P.p.log("Brewing");
		int slot = 0;
		while(slot < 3){
			ItemStack item = event.getContents().getItem(slot);
			if(item != null){
				if(item.getType() == Material.POTION){
					if(item.hasItemMeta()){
						if(item.getItemMeta().getDisplayName().equals("Alkohol")){//TESTING only! neew a way to store get UID
							P.p.log("is Alcohol");
							event.getContents().setItem(slot,new ItemStack(2));
							if(!event.isCancelled()){
								event.setCancelled(true);
							}
						} else {
							event.getContents().setItem(slot,new ItemStack(Material.GLASS_BOTTLE));
						}
					} else {
						event.getContents().setItem(slot,new ItemStack(Material.GLASS_BOTTLE));
					}
				}
			}
		slot++;
		}
		
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event){
		if(event.getItem() != null){
			if(event.getItem().getType() == Material.POTION){
				P.p.log("consuming a potion");
				if(event.getItem().hasItemMeta()){
					PotionMeta potionMeta = ((PotionMeta) event.getItem().getItemMeta());
					if(potionMeta.hasCustomEffect(PotionEffectType.CONFUSION)){
						P.p.log("hasConfusion");
					}
				}
			}
		}


	}

}