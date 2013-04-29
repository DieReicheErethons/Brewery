package com.dre.brewery.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BIngredients;
import com.dre.brewery.Brew;
import com.dre.brewery.Barrel;

import com.dre.brewery.P;

public class PlayerListener implements Listener{
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerInteract(PlayerInteractEvent event){
		Block clickedBlock = event.getClickedBlock();
		
		if(clickedBlock!=null){
			if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
				if(clickedBlock.getType() == Material.CAULDRON){
					if(clickedBlock.getRelative(BlockFace.DOWN).getType() == Material.FIRE ||
					clickedBlock.getRelative(BlockFace.DOWN).getType() == Material.STATIONARY_LAVA ||
						clickedBlock.getRelative(BlockFace.DOWN).getType() == Material.LAVA){
						Material materialInHand = event.getMaterial();
						Player player = event.getPlayer();
						ItemStack item = event.getItem();

						//add ingredient to cauldron that meet the previous contitions
						if(BIngredients.possibleIngredients.contains(materialInHand)){
							if(BCauldron.ingredientAdd(clickedBlock,materialInHand)){
								if(item.getAmount() > 1){
									item.setAmount(item.getAmount() -1);
								} else {
									player.setItemInHand(new ItemStack(0));
								}
							}
						//fill a glass bottle with potion
						} else if(materialInHand == Material.GLASS_BOTTLE){
							if(BCauldron.fill(player,clickedBlock)){
								event.setCancelled(true);
								if(item.getAmount() > 1){
									item.setAmount(item.getAmount() -1);
								} else {
									player.setItemInHand(new ItemStack(0));
								}
							}
						//reset cauldron when refilling to prevent unlimited source of potions
						} else if(materialInHand == Material.WATER_BUCKET){
							if(clickedBlock.getData() != 0){
								if(clickedBlock.getData() < 3){
									//will only remove when existing
									BCauldron.remove(clickedBlock);
								}
							}
						}
					}
				//access a barrel
				} else if (clickedBlock.getType() == Material.FENCE ||
					clickedBlock.getType() == Material.NETHER_FENCE ||
					clickedBlock.getType() == Material.SIGN ||
					clickedBlock.getType() == Material.WALL_SIGN){
					Barrel barrel = Barrel.get(clickedBlock);
					if(barrel != null){
						event.setCancelled(true);
						Block broken = Barrel.getBrokenBlock(clickedBlock);
						//barrel is built correctly
						if(broken == null){
							barrel.open(event.getPlayer());
						} else {
							barrel.remove(broken);
						}
					}
				}
			}
		}

	}




	@EventHandler(priority = EventPriority.HIGH)
	public void onBrew(BrewEvent event){
		int slot = 0;
		BrewerInventory inv = event.getContents();
		ItemStack item;
		boolean custom = false;
		Integer[] contents = new Integer[3];
		while(slot < 3){
			item = inv.getItem(slot);
			contents[slot] = 0;
			if(item != null){
				if(item.getType() == Material.POTION){
					if(item.hasItemMeta()){
						PotionMeta potionMeta = ((PotionMeta) item.getItemMeta());
						if(potionMeta.hasCustomEffect(PotionEffectType.REGENERATION)){
							if(Brew.get(potionMeta) != null){
								//has custom potion in "slot"
								contents[slot] = 1;
								custom = true;
							}
						}
					}
				}
			}
		slot++;
		}
		if(custom){
			event.setCancelled(true);
			Brew.distillAll(inv,contents);
		}
		
	}

	//player drinks a custom potion
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event){
		if(event.getItem() != null){
			if(event.getItem().getType() == Material.POTION){
				if(event.getItem().hasItemMeta()){
					PotionMeta potionMeta = ((PotionMeta) event.getItem().getItemMeta());
					if(potionMeta.hasCustomEffect(PotionEffectType.REGENERATION)){
						for(PotionEffect effect:potionMeta.getCustomEffects()){
							if(effect.getType().getId() == 10){
								//tell him the ID for testing
								P.p.msg(event.getPlayer(),"ID: "+effect.getDuration());
							}
						}
					}
				}
			}
		}


	}

}