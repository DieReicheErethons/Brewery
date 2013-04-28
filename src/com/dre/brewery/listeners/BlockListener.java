package com.dre.brewery.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;
import com.dre.brewery.P;

public class BlockListener implements Listener{
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSignChange(SignChangeEvent event){
		String[] lines = event.getLines();
		
		if(lines[0].equalsIgnoreCase("Fass")){
			if(Barrel.create(event.getBlock())){
				P.p.msg(event.getPlayer(),"Fass erfolgreich erstellt");
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockBreak(BlockBreakEvent event){
		Block block=event.getBlock();
		//remove cauldron
		if(block.getType() == Material.CAULDRON){
			if(block.getRelative(BlockFace.DOWN).getType() == Material.FIRE ||
			block.getRelative(BlockFace.DOWN).getType() == Material.STATIONARY_LAVA ||
			block.getRelative(BlockFace.DOWN).getType() == Material.LAVA){
				if(block.getData() != 0){
					//will only remove when existing
					BCauldron.remove(block);
				}
			}
		//remove barrel and throw potions on the ground
		} else if(block.getType() == Material.FENCE || block.getType() == Material.NETHER_FENCE){
			Barrel barrel = Barrel.get(block);
			if(barrel != null){
				barrel.remove(null);
			}
		}
	}
}
