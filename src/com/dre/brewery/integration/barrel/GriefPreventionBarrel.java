package com.dre.brewery.integration.barrel;

import com.dre.brewery.P;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public class GriefPreventionBarrel {

	private static P brewery = P.p;

	public static boolean checkAccess(BarrelAccessEvent event) {
		GriefPrevention griefPrevention = GriefPrevention.instance;
		Player player = event.getPlayer();
		PlayerData playerData = griefPrevention.dataStore.getPlayerData(player.getUniqueId());

		if (!griefPrevention.claimsEnabledForWorld(player.getWorld()) || playerData.ignoreClaims || !griefPrevention.config_claims_preventTheft) {
			return true;
		}

		// block container use during pvp combat
		if (playerData.inPvpCombat()) {
			return false;
		}

		// check permissions for the claim the Barrel is in
		Claim claim = griefPrevention.dataStore.getClaimAt(event.getSpigot().getLocation(), false, playerData.lastClaim);
		if (claim != null) {
			playerData.lastClaim = claim;
			Supplier<String> supplier = claim.checkPermission(player, ClaimPermission.Inventory, null);
			String noContainersReason = supplier != null ? supplier.get() : null;
			if (noContainersReason != null) {
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
