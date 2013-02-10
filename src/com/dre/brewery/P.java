package com.dre.brewery;

import org.bukkit.plugin.java.JavaPlugin;

import com.dre.brewery.listeners.BlockListener;

public class P extends JavaPlugin{
	public static P p;
	
	//Listeners
	public BlockListener blockListener;
	
	
	@Override
	public void onEnable(){
		 p = this;
		 
		 //Listeners
		 blockListener = new BlockListener();
		 
		 p.getServer().getPluginManager().registerEvents(blockListener, p);
		
	}
	
	@Override
	public void onDisable(){
		
	}
}
