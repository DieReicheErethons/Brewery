package com.dre.brewery.filedata;

import com.dre.brewery.BreweryPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;


/**
 * Update Checker modified for BreweryX
 */
public class UpdateChecker {

	private final static BreweryPlugin plugin = BreweryPlugin.getInstance();
	private final int resourceID;
	private static String latestVersion = plugin.getDescription().getVersion();
	private static boolean updateAvailable = false;

	public UpdateChecker(int resourceID) {
		this.resourceID = resourceID;
	}

	public static void notify(final Player player) {
		if (!updateAvailable || !player.hasPermission("brewery.update")) {
			return;
		}
		plugin.msg(player, plugin.languageReader.get("Etc_UpdateAvailable", "v"+plugin.getDescription().getVersion(), "v"+latestVersion));
	}

	/**
	 * Query the API to find the latest approved file's details.
	 */
	public void query(final Consumer<String> consumer) {
		BreweryPlugin.getScheduler().runTaskAsynchronously(() -> {
			try (InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceID + "/~").openStream(); Scanner scann = new Scanner(is)) {
				if (scann.hasNext()) {
					consumer.accept(scann.next());
				}
			} catch (IOException e) {
				plugin.getLogger().log(Level.WARNING, "Cannot look for updates: " + e);
			}
		});
	}

	public static void setLatestVersion(String version) {
		latestVersion = version;
	}

	public static String getLatestVersion() {
		return latestVersion;
	}

	public static void setUpdateAvailable(boolean available) {
		updateAvailable = available;
	}
}
