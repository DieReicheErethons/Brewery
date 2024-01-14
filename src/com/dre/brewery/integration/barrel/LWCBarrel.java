package com.dre.brewery.integration.barrel;

import com.dre.brewery.Barrel;
import com.dre.brewery.BreweryPlugin;
import com.griefcraft.listeners.LWCPlayerListener;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Flag;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.event.LWCProtectionDestroyEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

public class LWCBarrel {


	public static boolean denyDestroy(Player player, Barrel barrel) {
		LWC lwc = LWC.getInstance();
		Block sign = barrel.getBody().getSignOfSpigot();
		//if (!Boolean.parseBoolean(lwc.resolveProtectionConfiguration(sign, "ignoreBlockDestruction"))) {
			Protection protection = lwc.findProtection(sign);
			if (protection != null) {
				boolean canAccess = lwc.canAccessProtection(player, protection);
				boolean canAdmin = lwc.canAdminProtection(player, protection);

				try {
					LWCProtectionDestroyEvent evt = new LWCProtectionDestroyEvent(player, protection, LWCProtectionDestroyEvent.Method.BLOCK_DESTRUCTION, canAccess, canAdmin);
					lwc.getModuleLoader().dispatchEvent(evt);

					if (evt.isCancelled()) {
						return true;
					}
				} catch (Exception e) {
					lwc.sendLocale(player, "protection.internalerror", "id", "BLOCK_BREAK");
					BreweryPlugin.breweryPlugin.errorLog("Failed to dispatch LWCProtectionDestroyEvent");
					e.printStackTrace();
					return true;
				}
			}
		//}

		return false;
	}

	public static boolean checkAccess(Player player, Block sign, Plugin plugin) {
		LWC lwc = LWC.getInstance();

		// Disallow Chest Access with these permissions
		if (!lwc.hasPermission(player, "lwc.protect") && lwc.hasPermission(player, "lwc.deny") && !lwc.isAdmin(player) && !lwc.isMod(player)) {
			lwc.sendLocale(player, "protection.interact.error.blocked");
			return false;
		}

		// We just fake a BlockInteractEvent on the Sign for LWC, it handles it nicely. Otherwise we could copy LWCs listener in here...
		PlayerInteractEvent lwcEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), sign, BlockFace.EAST);
		for (RegisteredListener listener : HandlerList.getRegisteredListeners(plugin)) {
			if (listener.getListener() instanceof LWCPlayerListener) {
				try {
					listener.callEvent(lwcEvent);
					//noinspection deprecation
					if (lwcEvent.isCancelled()) {
						return false;
					}
				} catch (EventException e) {
					lwc.sendLocale(player, "protection.internalerror", "id", "PLAYER_INTERACT");
					BreweryPlugin.breweryPlugin.errorLog("Block Interact could not be passed to LWC");
					e.printStackTrace();
					return false;
				}
			}
		}

		return true;
	}

	// If a Barrel is destroyed without player
	public static void remove(Barrel barrel) {
		Protection protection = LWC.getInstance().findProtection(barrel.getBody().getSignOfSpigot());
		if (protection != null) {
			protection.remove();
		}
	}

	// Returns true if the block that exploded should not be removed
	public static boolean denyExplosion(Barrel barrel) {
		Protection protection = LWC.getInstance().findProtection(barrel.getBody().getSignOfSpigot());

		return protection != null && !protection.hasFlag(Flag.Type.ALLOWEXPLOSIONS);
	}

	// Returns true if the block that was destroyed should not be removed
	public static boolean denyDestroyOther(Barrel barrel) {
		return LWC.getInstance().findProtection(barrel.getBody().getSignOfSpigot()) != null;
	}
}
