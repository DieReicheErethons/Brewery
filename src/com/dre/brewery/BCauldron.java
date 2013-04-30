package com.dre.brewery;

import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;

import com.dre.brewery.BIngredients;

public class BCauldron {
	public static CopyOnWriteArrayList<BCauldron> bcauldrons=new CopyOnWriteArrayList<BCauldron>();

	private BIngredients ingredients;
	private Block block;
	private int state;

	public BCauldron(Block block,Material ingredient){
		this.block = block;
		this.state = 1;
		this.ingredients = new BIngredients();
		add(ingredient);
		bcauldrons.add(this);
	}

	//loading from file
	public BCauldron(Block block,BIngredients ingredients,int state){
		this.block = block;
		this.state = state;
		this.ingredients = ingredients;
		bcauldrons.add(this);
	}


	public void onUpdate(){
		//Check if fire still alive
		if(block.getRelative(BlockFace.DOWN).getType() == Material.FIRE ||
		block.getRelative(BlockFace.DOWN).getType() == Material.STATIONARY_LAVA ||
		block.getRelative(BlockFace.DOWN).getType() == Material.LAVA){
			//add a minute to cooking time
			state++;
		}
	}


	//add an ingredient to the cauldron
	public void add(Material ingredient){
		ingredients.add(ingredient);
		block.getWorld().playEffect(block.getLocation(),Effect.EXTINGUISH,0);
		if(state > 1){
			state--;
		}
	}

	//get cauldron from block and add given ingredient
	public static boolean ingredientAdd(Block block,Material ingredient){
		//if not empty
		if(block.getData() != 0){
			for(BCauldron bcauldron:bcauldrons){
				if(bcauldron.block.equals(block)){
					bcauldron.add(ingredient);
					return true;
				}
			}
			new BCauldron(block,ingredient);
			return true;
		}
		return false;
	}

	//fills players bottle with cooked brew
	public static boolean fill(Player player,Block block){
		for(BCauldron bcauldron:bcauldrons){
			if(bcauldron.block.equals(block)){
				ItemStack potion = bcauldron.ingredients.cook(bcauldron.state);
				if(potion != null){
					//Bukkit Bug, inventory not updating while in event so this will delay the give
					//but could also just use deprecated updateInventory()
					giveItem(player,potion);
					//player.getInventory().addItem(potion);
					//player.getInventory().updateInventory();
					if(block.getData() > 3){
						block.setData((byte)3);
					}
					block.setData((byte)(block.getData() - 1));

					if(block.getData() == 0){
						bcauldrons.remove(bcauldron);
					}
					return true;
				}
			}
		}
		return false;
	}

	//reset to normal cauldron
	public static void remove(Block block){
		for(BCauldron bcauldron:bcauldrons){
			if(bcauldron.block.equals(block)){
				bcauldrons.remove(bcauldron);
			}
		}
	}

	public static void save(ConfigurationSection config){
		int id = 0;
		for(BCauldron cauldron:bcauldrons){
			//cauldrons are randomly listed
			ConfigurationSection section = config.createSection(""+id);
			section.set("block",cauldron.block.getWorld().getName()+"/"+cauldron.block.getX()+"/"+cauldron.block.getY()+"/"+cauldron.block.getZ());
			if(cauldron.state != 1){
				section.set("state",cauldron.state);
			}
			cauldron.ingredients.save(section.createSection("ingredients"));
			id++;
		}
	}

	//bukkit bug not updating the inventory while executing event, have to schedule the give
	public static void giveItem(final Player player,final ItemStack item){
		P.p.getServer().getScheduler().runTaskLater(P.p, new Runnable() {
			public void run() {
				player.getInventory().addItem(item);
			}
		},1L);
	}

}