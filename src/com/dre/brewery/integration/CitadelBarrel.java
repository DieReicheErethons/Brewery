package com.dre.brewery.integration;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.dre.brewery.P;

import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.ReinforcementManager;
import vg.civcraft.mc.citadel.reinforcement.NullReinforcement;
import vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement;
import vg.civcraft.mc.citadel.reinforcement.Reinforcement;

/**
 * Basic Citadel support to prevent randos from stealing your barrel aging brews
 * 
 * @author ProgrammerDan
 */
public class CitadelBarrel {
	static P brewery = P.p;

	public static boolean checkAccess(Player player, Block sign) {
		ReinforcementManager manager = Citadel.getReinforcementManager();
		
		Reinforcement rein = manager.getReinforcement(sign);
		
		if (rein == null) return true; // no protections in place.
		
		if (rein instanceof PlayerReinforcement) {
			PlayerReinforcement prein = (PlayerReinforcement) rein;
			if (prein.canAccessChests(player)) {
				return true;
			}
		} else if (rein instanceof NullReinforcement) {
			return true;
		}
		// no support for multiblock atm, would require namelayer support.
		
		// special locked, or no access.
		brewery.msg(player, brewery.languageReader.get("Error_NoBarrelAccess"));
		return false;
	}
}
