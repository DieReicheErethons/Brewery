package com.dre.brewery.utility;

import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class PermissionUtil {

	public static Map<CommandSender, Boolean> extendedPermsCache = new HashMap<>();

	public static void logout(CommandSender sender) {
		extendedPermsCache.remove(sender);
	}

	/**
	 * Update the Permission Cache, sets it to false if the sender
	 * currently doesn't have any more than the default permission
	 *
	 * @param sender The sender of which to update the permission cache
	 */
	public static void evaluateExtendedPermissions(CommandSender sender) {
		for (BPermission perm : BPermission.values()) {
			if (perm != BPermission.UNLABEL) { // This is the default permission
				if (sender.hasPermission(perm.permission)) {
					extendedPermsCache.put(sender, true);
					return;
				}
			}
		}
		extendedPermsCache.put(sender, false);
	}

	/**
	 * Check if the Sender might have more than just the default permissions
	 *
	 * @return false if there _might be_ more permissions for this sender
	 */
	public static boolean noExtendedPermissions(CommandSender sender) {
		Boolean extendedPerms = extendedPermsCache.get(sender);

		if (extendedPerms == null) {
			evaluateExtendedPermissions(sender);
			extendedPerms = extendedPermsCache.get(sender);
		}

		return extendedPerms == null || !extendedPerms;
	}

	/**
	 * Returns true if the Sender has the permission, returns false if the cache says he is unlikely to have it
	 *
	 * @return true only if the sender definitely has the permission. false if he hasn't or it's unlikely
	 */
	public static boolean hasCachedPermission(CommandSender sender, String permission) {
		BPermission perm = BPermission.get(permission);
		if (perm != null) {
			return hasCachedPermission(sender, perm);
		} else {
			return sender.hasPermission(permission);
		}
	}

	/**
	 * Returns true if the Sender has the permission, returns false if the cache says he is unlikely to have it
	 *
	 * @return true only if the sender definitely has the permission. false if he hasn't or it's unlikely
	 */
	public static boolean hasCachedPermission(CommandSender sender, BPermission bPerm) {
		if (bPerm != BPermission.UNLABEL) {
			if (noExtendedPermissions(sender)) {
				return false;
			}
		}

		return sender.hasPermission(bPerm.permission);
	}


	/**
	 * Brewery Permissions of _only_ the Commands
	 */
	public enum BPermission {
		PLAYER("brewery.cmd.player"),
		SEAL("brewery.cmd.seal"),
		UNLABEL("brewery.cmd.unlabel"),

		INFO("brewery.cmd.info"),
		INFO_OTHER("brewery.cmd.infoOther"),

		CREATE("brewery.cmd.create"),
		DRINK("brewery.cmd.drink"),
		DRINK_OTHER("brewery.cmd.drinkOther"),

		RELOAD("brewery.cmd.reload"),
		PUKE("brewery.cmd.puke"),
		PUKE_OTHER("brewery.cmd.pukeOther"),
		WAKEUP("brewery.cmd.wakeup"),

		STATIC("brewery.cmd.static"),
		COPY("brewery.cmd.copy"),
		DELETE("brewery.cmd.delete");

		public String permission;

		BPermission(String permission) {
			this.permission = permission;
		}

		public boolean checkCached(CommandSender sender) {
			return hasCachedPermission(sender, this);
		}

		public static BPermission get(String permission) {
			for (BPermission bPerm : BPermission.values()) {
				if (bPerm.permission.equals(permission)) {
					return bPerm;
				}
			}
			return null;
		}
	}
}
