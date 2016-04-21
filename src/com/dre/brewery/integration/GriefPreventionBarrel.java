package com.dre.brewery.integration;

import com.dre.brewery.P;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class GriefPreventionBarrel {

	static P brewery = P.p;
	static GriefPrevention griefPrevention = GriefPrevention.instance;

	public static boolean checkAccess(Player player, Block sign) {
		PlayerData playerData = griefPrevention.dataStore.getPlayerData(player.getUniqueId());

		if (!griefPrevention.claimsEnabledForWorld(player.getWorld()) || playerData.ignoreClaims || !griefPrevention.config_claims_preventTheft) {
			return true;
		}

		// block container use during pvp combat
		if (playerData.inPvpCombat()) {
			brewery.msg(player, brewery.languageReader.get("Error_NoBarrelAccess"));
			return false;
		}

		// check permissions for the claim the Barrel is in
		Claim claim = griefPrevention.dataStore.getClaimAt(sign.getLocation(), false, playerData.lastClaim);
		if (claim != null) {
			playerData.lastClaim = claim;
			String noContainersReason = claim.allowContainers(player);
			if (noContainersReason != null) {
				brewery.msg(player, brewery.languageReader.get("Error_NoBarrelAccess"));
				return false;
			}
		}

		// drop any pvp protection, as the player opens a barrel
		if (playerData.pvpImmune) {
			playerData.pvpImmune = false;
		}

		return true;
	}

}
