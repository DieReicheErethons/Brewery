package com.dre.brewery.integration.barrel;

import com.dre.brewery.Barrel;
import com.dre.brewery.BarrelBody;
import com.dre.brewery.P;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.utility.LegacyUtil;
import nl.rutgerkok.blocklocker.BlockLockerAPIv2;
import nl.rutgerkok.blocklocker.ProtectableBlocksSettings;
import nl.rutgerkok.blocklocker.ProtectionType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.List;

public class BlocklockerBarrel implements ProtectableBlocksSettings {
	private static Block lastBarrelSign;

	@Override
	public boolean canProtect(Block block) {
		return isOrWillCreateBarrel(block);
	}

	@Override
	public boolean canProtect(ProtectionType protectionType, Block block) {
		if (protectionType != ProtectionType.CONTAINER) return false;

		return isOrWillCreateBarrel(block);
	}

	public boolean isOrWillCreateBarrel(Block block) {
		if (!P.p.isEnabled() || !BConfig.useBlocklocker) {
			return false;
		}
		if (!LegacyUtil.isWoodPlanks(block.getType()) && !LegacyUtil.isWoodStairs(block.getType())) {
			// Can only be a barrel if it's a planks block
			return false;
		}
		if (Barrel.getByWood(block) != null) {
			// Barrel already exists
			return true;
		}
		if (lastBarrelSign == null) {
			// No player wants to create a Barrel
			return false;
		}
		for (BlockFace face : new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
			Block sign = block.getRelative(face);
			if (lastBarrelSign.equals(sign)) {
				Block spigot = BarrelBody.getSpigotOfSign(sign);
				byte signoffset = 0;
				if (!spigot.equals(sign)) {
					signoffset = (byte) (sign.getY() - spigot.getY());
				}
				Barrel barrel = new Barrel(spigot, signoffset);

				return barrel.getBody().getBrokenBlock(true) == null;
			}
		}
		return false;
	}

	public static boolean checkAccess(BarrelAccessEvent event) {
		Block sign = event.getBarrel().getBody().getSignOfSpigot();
		if (!LegacyUtil.isSign(sign.getType())) {
			return true;
		}
		return BlockLockerAPIv2.isAllowed(event.getPlayer(), sign, true);
	}

	public static void createdBarrelSign(Block sign) {
		// The Player created a sign with "Barrel" on it, he want's to create a barrel
		lastBarrelSign = sign;
	}

	public static void clearBarrelSign() {
		lastBarrelSign = null;
	}

	public static void registerBarrelAsProtectable() {
		try {
			List<ProtectableBlocksSettings> extraProtectables = BlockLockerAPIv2.getPlugin().getChestSettings().getExtraProtectables();
			if (extraProtectables.stream().noneMatch(blockSettings -> blockSettings instanceof BlocklockerBarrel)) {
				extraProtectables.add(new BlocklockerBarrel());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
