package com.dre.brewery.integration.barrel;

import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.player.KingdomPlayer;

public class KingdomsXBarrel {

	public static boolean checkAccess(BarrelAccessEvent event) {
		Land land = SimpleChunkLocation.of(event.getSpigot().getLocation()).getLand();
		return land == null || land.getKingdomId() == null || !land.isClaimed()
				|| land.getKingdomId().equals(KingdomPlayer.getKingdomPlayer(event.getPlayer()).getKingdomId());
	}

}
