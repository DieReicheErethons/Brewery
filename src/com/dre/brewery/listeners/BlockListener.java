package com.dre.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class BlockListener implements Listener{
	
	@EventHandler
	public void onSignChange(SignChangeEvent event){
		String[] lines = event.getLines();
		
		if(lines[0].equalsIgnoreCase("[Barrel]")){
			event.getPlayer().sendMessage("Barrel created!");
		}
	}
}
