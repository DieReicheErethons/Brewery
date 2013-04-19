package com.dre.brewery;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.PotionMeta;

//import java.util.List;

import com.dre.brewery.P;
import com.dre.brewery.BRecipe;



public class BIngredients {
	public static ArrayList<Material> possibleIngredients=new ArrayList<Material>();
	public static ArrayList<BRecipe> recipes=new ArrayList<BRecipe>();
	public static Map<Material,String> cookedNames=new HashMap<Material,String>();

	private Map<Material,Integer> ingredients=new HashMap<Material,Integer>();
	//private int quality = 10;
	private String cookedName;
	private int cookedTime;
	private PotionEffect cookedEffect = (PotionEffectType.CONFUSION).createEffect(200,0);//vorrübergehend

	public BIngredients(){
	}

	public void add(Material ingredient){
		if(ingredients.containsKey(ingredient)){
			int newAmount = ingredients.get(ingredient) + 1;
			ingredients.put(ingredient,newAmount);
			P.p.log("Now "+newAmount+" of this in here");
		} else {
			this.ingredients.put(ingredient,1);
		}
	}

	public ItemStack cook(int state){

		for(BRecipe recipe:recipes){
			if(recipe.isCookingOnly()){
				//Nur Kochen
			}
		}

		if(state == 0){//TESTING sonst 1
			cookedName = "Schlammiger Sud";//not cooked long enough
		} else {
			for(Material ingredient:ingredients.keySet()){
				if(cookedNames.containsKey(ingredient)){
					P.p.log("Trank aus "+getIngredientsAmount()+" Zutaten");
					if(ingredients.get(ingredient) > (getIngredientsAmount() / 2)){//if more than half of the ingredients is of one kind
						cookedName = cookedNames.get(ingredient);
					}
				}
			}
		}
		if(cookedName == null){
			cookedName = "Undefinierbarer Sud";
		}

		cookedTime = state * 2;

		ItemStack potion = new ItemStack(Material.POTION);
		//new Potion(PotionType.getByEffect(PotionEffectType.SLOW)).toItemStack(1);

		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
		potionMeta.setDisplayName(cookedName);
		//List<String> lore = new ArrayList<String>();
		//lore.add("gesöff vom feinsten");
		//potionMeta.setLore(lore);
		potionMeta.clearCustomEffects();
		potionMeta.addCustomEffect(cookedEffect,true);//Add our own effects, overriding existing ones
		potion.setItemMeta(potionMeta);


		return potion;



	/*	while(i < possibleIngredients.size()){
			//Plain drinks
			if(getAmountOf(possibleIngredients.get(i)) > (ingredients.size() / 2)){
				cookedPotion = new Potion(PotionType.getByEffect(PotionEffectType.SLOW));
				P.p.log("making a plain potion");
				return cookedPotion;
			}

			//Special Recipies
			//Shroom Wodka
			if((getAmountOf(possibleIngredients.get(2)) * 3) >= (getAmountOf(badIngredients.get(0)) * 2)){
				if((getAmountOf(possibleIngredients.get(2)) * 2) <= (getAmountOf(badIngredients.get(0)) * 3)){
					//Type Shroom wodka
				}
			}



			i++;
		}*/
	}

	private int getIngredientsAmount(){
		int amount = 0;
		for(int value:ingredients.values()){
			amount += value;
		}
		return amount;
	}





	/*private int getAmountOf(Material ingredient){
		int amount = 0;
		for(Material bingredient:ingredients){
			if(bingredient == ingredient){
				amount++;
			}
		}
		P.p.log("Amount="+amount);
		return amount;
	}*/

}