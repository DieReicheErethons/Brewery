package com.dre.brewery;

import java.util.concurrent.CopyOnWriteArrayList;

//import java.util.List;
//import java.util.ArrayList;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;
import org.bukkit.entity.HumanEntity;
import org.bukkit.Material;
import org.bukkit.material.Cauldron;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
//import org.bukkit.inventory.meta.ItemMeta;
//import org.bukkit.inventory.meta.PotionMeta;
//import org.bukkit.potion.Potion;
//import org.bukkit.potion.PotionEffect;
//import org.bukkit.potion.PotionEffectType;
import org.bukkit.World;
import org.bukkit.Effect;

//import org.bukkit.block.BrewingStand;

import com.dre.brewery.BIngredients;

import com.dre.brewery.P;

public class BCauldron {
	public static CopyOnWriteArrayList<BCauldron> bcauldrons=new CopyOnWriteArrayList<BCauldron>();

	private BIngredients ingredients;
	private Block block;
	private int state;

	public BCauldron(Block block,Material ingredient){
		P.p.log("aaand we got a fresh cauldron");
		this.block = block;
		this.state = 1;
		this.ingredients = new BIngredients();
		add(ingredient);
		bcauldrons.add(this);
	}


	public void onUpdate(){
		state++;//wie lange es schon kocht
	}


	public void add(Material ingredient){
		ingredients.add(ingredient);
		block.getWorld().playEffect(block.getLocation(),Effect.EXTINGUISH,0);
		if(state > 1){
			state--;
		}
	}

	public static boolean ingredientAdd(Block block,Material ingredient){
		if(block.getData() != 0){
			for(BCauldron bcauldron:bcauldrons){
				if(bcauldron.block.equals(block)){
					P.p.log("is existing Cauldron");
					bcauldron.add(ingredient);
					return true;
				}
			}
			new BCauldron(block,ingredient);
			return true;
		}
		return false;
	}

	public static boolean fill(Player player,Block block){
		for(BCauldron bcauldron:bcauldrons){
			if(bcauldron.block.equals(block)){
				ItemStack potion = bcauldron.ingredients.cook(bcauldron.state);
				if(potion != null){
					giveItem(player,potion);//Bukkit Bug, but could also just use deprecated updateInventory()
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

	public static void remove(Block block){
		for(BCauldron bcauldron:bcauldrons){
			if(bcauldron.block.equals(block)){
				bcauldrons.remove(bcauldron);//reset to normal cauldron (when refilling it)
			}
		}
	}

	public static void giveItem(final Player player,final ItemStack item){
		P.p.getServer().getScheduler().runTaskLater(P.p, new Runnable() {
			public void run() {
				player.getInventory().addItem(item);
			}
		},1L);
	}

}