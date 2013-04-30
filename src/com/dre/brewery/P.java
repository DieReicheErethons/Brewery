package com.dre.brewery;

import java.util.Map;
import java.util.HashMap;

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

public class P extends JavaPlugin{
	public static P p;
	public static int lastBackup = 0;
	
	//Listeners
	public BlockListener blockListener;
	public PlayerListener playerListener;
	public EntityListener entityListener;
	
	
	@Override
	public void onEnable(){
		p = this;

		readConfig();
		readData();
		 
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
		
		saveData();

		this.log(this.getDescription().getName()+" disabled!");
	}

	public void msg(CommandSender sender,String msg){
		sender.sendMessage(ChatColor.DARK_GREEN+"[Brewery] "+ChatColor.WHITE+msg);
	}

	public void log(String msg){
		this.msg(Bukkit.getConsoleSender(), msg);
	}

	public void errorLog(String msg){
		Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_GREEN+"[Brewery] "+ChatColor.DARK_RED+"ERROR: "+ChatColor.RED+msg);
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

	//load all Data
	public void readData(){

		File file=new File(p.getDataFolder(), "data.yml");
		if(file.exists()){

			FileConfiguration data = YamlConfiguration.loadConfiguration(file);

			//loading Brew
			ConfigurationSection section = data.getConfigurationSection("Brew");
			if(section != null){
				//All sections have the UID as name
				for(String uid:section.getKeys(false)) {
						new Brew(
							parseInt(uid), loadIngredients(section.getConfigurationSection(uid+".ingredients")),
							section.getInt(uid+".quality",0), section.getInt(uid+".distillRuns",0), (float)section.getDouble(uid+".ageTime",0.0),  section.getInt(uid+".alcohol",0));
				}
			}

			//loading BCauldron
			section = data.getConfigurationSection("BCauldron");
			if(section != null){
				for(String cauldron:section.getKeys(false)) {
					//block is splitted into worldname/x/y/z
					String block = section.getString(cauldron+".block");
					if(block != null){
						String[] splitted = block.split("/");
						if(splitted.length == 4){
							new BCauldron(
								getServer().getWorld(splitted[0]).getBlockAt(parseInt(splitted[1]),parseInt(splitted[2]),parseInt(splitted[3])),
								loadIngredients(section.getConfigurationSection(cauldron+".ingredients")), section.getInt(cauldron+".state",1));
						} else {
							errorLog("Incomplete Block-Data in data.yml: "+section.getCurrentPath()+"."+cauldron);
						}
					} else {
						errorLog("Missing Block-Data in data.yml: "+section.getCurrentPath()+"."+cauldron);
					}
				}
			}

			//loading Barrel
			section = data.getConfigurationSection("Barrel");
			if(section != null){
				for(String barrel:section.getKeys(false)) {
					//block spigot is splitted into worldname/x/y/z
					String spigot = section.getString(barrel+".spigot");
					if(spigot != null){
						String[] splitted = spigot.split("/");
						if(splitted.length == 4){
							//load itemStacks from invSection
							ConfigurationSection invSection = section.getConfigurationSection(barrel+".inv");
							if(invSection != null){
								//Map<String,ItemStack> inventory = section.getValues(barrel+"inv");
								new Barrel(
									getServer().getWorld(splitted[0]).getBlockAt(parseInt(splitted[1]),parseInt(splitted[2]),parseInt(splitted[3])),
									invSection.getValues(true), (float)section.getDouble(barrel+".time",0.0));

							} else {
								//errorLog("Inventory of "+section.getCurrentPath()+"."+barrel+" in data.yml is missing");
								//Barrel has no inventory
								new Barrel(
									getServer().getWorld(splitted[0]).getBlockAt(parseInt(splitted[1]),parseInt(splitted[2]),parseInt(splitted[3])),
									(float)section.getDouble(barrel+".time",0.0));
							}
						} else {
							errorLog("Incomplete Block-Data in data.yml: "+section.getCurrentPath()+"."+barrel);
						}
					} else {
						errorLog("Missing Block-Data in data.yml: "+section.getCurrentPath()+"."+barrel);
					}
				}
			}
			
		} else {
			errorLog("No data.yml found, will create new one!");
		}
	}


	//loads BIngredients from ingredient section
	public BIngredients loadIngredients(ConfigurationSection config){
		if(config != null){
			ConfigurationSection matSection = config.getConfigurationSection("mats");
			if(matSection != null){
				//matSection has all the materials + amount in Integer form
				Map<Material,Integer> ingredients = new HashMap<Material,Integer>();
				for(String ingredient:matSection.getKeys(false)){
					//convert to Material
					ingredients.put(Material.getMaterial(parseInt(ingredient)), matSection.getInt(ingredient));
				}
				return new BIngredients(ingredients, config.getInt("cookedTime",0));
			}
		}
		errorLog("Ingredient section not found or incomplete in data.yml");
		return new BIngredients();
	}

	//save all Data
	public void saveData(){
		File datafile = new File(p.getDataFolder(), "data.yml");
		if(datafile.exists()){
			if(lastBackup > 10){
				datafile.renameTo(new File(p.getDataFolder(), "dataBackup.yml"));
			} else {
				lastBackup++;
			}
		}

		FileConfiguration configFile = new YamlConfiguration();

		if(!Brew.potions.isEmpty()){
			Brew.save(configFile.createSection("Brew"));
		}
		if(!BCauldron.bcauldrons.isEmpty()){
			BCauldron.save(configFile.createSection("BCauldron"));
		}

		if(!Barrel.barrels.isEmpty()){
			Barrel.save(configFile.createSection("Barrel"));
		}
		//BPlayer is not yet saved, as it is WIP

		try {
			configFile.save(datafile);
		} catch (IOException e) {
			e.printStackTrace();
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

			saveData();//save all data
		}

	}

}
