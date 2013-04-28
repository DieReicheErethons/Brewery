package com.dre.brewery;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import java.io.File;
import org.apache.commons.lang.math.NumberUtils;

import com.dre.brewery.listeners.BlockListener;
import com.dre.brewery.listeners.PlayerListener;
import com.dre.brewery.listeners.EntityListener;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import java.io.IOException;

import org.bukkit.inventory.ItemStack;

public class P extends JavaPlugin{
	public static P p;
	
	//Listeners
	public BlockListener blockListener;
	public PlayerListener playerListener;
	public EntityListener entityListener;
	
	
	@Override
	public void onEnable(){
		p = this;

		readConfig();
		 
		//Listeners
		blockListener = new BlockListener();
		playerListener = new PlayerListener();
		entityListener = new EntityListener();
		 
		p.getServer().getPluginManager().registerEvents(blockListener, p);
		p.getServer().getPluginManager().registerEvents(playerListener, p);
		p.getServer().getPluginManager().registerEvents(entityListener, p);
		p.getServer().getScheduler().runTaskTimer(p, new BreweryRunnable(), 1200, 1200);


		this.log(this.getDescription().getName()+" enabled!");
	}
	
	@Override
	public void onDisable(){


		//Disable listeners
		HandlerList.unregisterAll(p);

		//Stop shedulers
		p.getServer().getScheduler().cancelTasks(this);


		File datafile = new File(p.getDataFolder(), "data.yml");
		FileConfiguration configFile = new YamlConfiguration();

		//braucht eine gute db
		ItemStack test = new ItemStack(2);//speichert später die custom potions (nicht als itemstack)
		configFile.set("ItemStack.Stack", test);

		try {
			configFile.save(datafile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		this.log(this.getDescription().getName()+" disabled!");
	}

		public void msg(CommandSender sender,String msg){
		sender.sendMessage(ChatColor.DARK_GREEN+"[Brewery] "+ChatColor.WHITE+msg);
	}

	public void log(String msg){
		this.msg(Bukkit.getConsoleSender(), msg);
	}


	public void readConfig(){

		File file=new File(p.getDataFolder(), "config.yml");
		if(!file.exists()){
			saveDefaultConfig();
		}
		FileConfiguration config = getConfig();

		//loading recipes
		ConfigurationSection configSection = config.getConfigurationSection("recipes");
		if(configSection != null){
			for(String recipeId:configSection.getKeys(false)){
				BIngredients.recipes.add(new BRecipe(configSection,recipeId));
			}
		}

		//loading cooked names and possible ingredients
		configSection = config.getConfigurationSection("cooked");
		if(configSection != null){
			for(String ingredient:configSection.getKeys(false)){
				BIngredients.cookedNames.put(Material.getMaterial(ingredient.toUpperCase()),(configSection.getString(ingredient)));
				BIngredients.possibleIngredients.add(Material.getMaterial(ingredient.toUpperCase()));
			}
		}

	}

	public int parseInt(String string){
		return NumberUtils.toInt(string, 0);
	}



	public class BreweryRunnable implements Runnable  {

		public BreweryRunnable() {
		}

		@Override
		public void run() {
			p.log("Update");
			for(BCauldron cauldron:BCauldron.bcauldrons){
				cauldron.onUpdate();//runs every min to update cooking time
			}
			Barrel.onUpdate();//runs every min to check and update ageing time
		}

	}

}
