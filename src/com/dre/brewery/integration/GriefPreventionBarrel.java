package com.dre.brewery.integration;


import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.dre.brewery.P;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

public class GriefPreventionBarrel {

	public static boolean checkAccess(Player player, Block sign) {

		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		/*if (!wc.Enabled()) {
			return true;
		}*/

		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getName());

		// block container use during pvp combat
		if (playerData.inPvpCombat() && wc.getPvPBlockContainers()) {
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoContainers);
			return false;
		}

		// check permissions for the claim the Barrel is in
		if (wc.getContainersRules().Allowed(sign.getLocation(), player, true).Denied()) {
			P.p.msg(player, P.p.languageReader.get("Error_NoBarrelAccess"));
			return false;
		}

		// drop any pvp protection, as the player opens a barrel
		if (playerData.pvpImmune) {
			playerData.pvpImmune = false;
			GriefPrevention.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
		}
		return true;
	}
}
