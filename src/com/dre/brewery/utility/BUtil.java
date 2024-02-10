package com.dre.brewery.utility;

import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class BUtil {

	/* **************************************** */
	/* *********                      ********* */
	/* *********     Bukkit Utils     ********* */
	/* *********                      ********* */
	/* **************************************** */

	private static final String WITH_DELIMITER = "((?<=%1$s)|(?=%1$s))";

	/**
	 * Check if the Chunk of a Block is loaded !without loading it in the process!
	 */
	public static boolean isChunkLoaded(Block block) {
		return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
	}

	/**
	 * Color code a message. Supports HEX colors and default minecraft colors!
	 * @param msg The message to color
	 * @return The colored message, or null if msg was null
	 */
	public static String color(String msg) {
		if (msg == null) return null;
		String[] texts = msg.split(String.format(WITH_DELIMITER, "&"));

		StringBuilder finalText = new StringBuilder();

		for (int i = 0; i < texts.length; i++) {
			if (texts[i].equalsIgnoreCase("&")) {
				//get the next string
				i++;
				if (texts[i].charAt(0) == '#') {
					finalText.append(net.md_5.bungee.api.ChatColor.of(texts[i].substring(0, 7)) + texts[i].substring(7));
				} else {
					finalText.append(ChatColor.translateAlternateColorCodes('&', "&" + texts[i]));
				}
			} else {
				finalText.append(texts[i]);
			}
		}
		return finalText.toString();
	}

	/**
	 * Creates a weighted mix between the two given colours
	 * <p>where the weight is calculated from the distance of the currentPos to the prev and next
	 *
	 * @param prevColor Previous Color
	 * @param prevPos Position of the Previous Color
	 * @param currentPos Current Position
	 * @param nextColor Next Color
	 * @param nextPos Position of the Next Color
	 * @return Mixed Color
	 */
	public static Color weightedMixColor(Color prevColor, int prevPos, int currentPos, Color nextColor, int nextPos) {
		float diffPrev = currentPos - prevPos;
		float diffNext = nextPos - currentPos;
		float total = diffNext + diffPrev;
		float percentNext = diffPrev / total;
		float percentPrev = diffNext / total;

			/*5 #8# 15
			8-5 = 3 -> 3/10
			15-8 = 7 -> 7/10*/

		return Color.fromRGB(
			Math.min(255, (int) ((nextColor.getRed() * percentNext) + (prevColor.getRed() * percentPrev))),
			Math.min(255, (int) ((nextColor.getGreen() * percentNext) + (prevColor.getGreen() * percentPrev))),
			Math.min(255, (int) ((nextColor.getBlue() * percentNext) + (prevColor.getBlue() * percentPrev)))
		);
	}

	/**
	 * Sets the Item in the Players hand, depending on which hand he used and if the hand should be swapped
	 *
	 * @param event Interact Event to tell which hand the player used
	 * @param mat The Material of the new item
	 * @param swapped If true, will set the opposite Hand instead of the one he used
	 */
	@SuppressWarnings("deprecation")
	public static void setItemInHand(PlayerInteractEvent event, Material mat, boolean swapped) {
		if (BreweryPlugin.use1_9) {
			if ((event.getHand() == EquipmentSlot.OFF_HAND) != swapped) {
				event.getPlayer().getInventory().setItemInOffHand(new ItemStack(mat));
			} else {
				event.getPlayer().getInventory().setItemInMainHand(new ItemStack(mat));
			}
		} else {
			event.getPlayer().setItemInHand(new ItemStack(mat));
		}
	}

	/**
	 * Returns either uuid or Name of player, depending on bukkit version
	 */
	public static String playerString(Player player) {
		if (BreweryPlugin.useUUID) {
			return player.getUniqueId().toString();
		} else {
			return player.getName();
		}
	}

	/**
	 * returns the Player if online
	 */
	public static Player getPlayerfromString(String name) {
		if (BreweryPlugin.useUUID) {
			try {
				return Bukkit.getPlayer(UUID.fromString(name));
			} catch (Exception e) {
				return Bukkit.getPlayerExact(name);
			}
		}
		return Bukkit.getPlayerExact(name);
	}

	/**
	 * Apply a Potion Effect, if player already has this effect, overwrite the existing effect.
	 *
	 * @param onlyIfStronger Optionally only overwrite if the new one is stronger, i.e. has higher level or longer duration
	 */
	public static void reapplyPotionEffect(Player player, PotionEffect effect, boolean onlyIfStronger) {
		final PotionEffectType type = effect.getType();
		if (player.hasPotionEffect(type)) {
			PotionEffect plEffect;
			if (BreweryPlugin.use1_11) {
				plEffect = player.getPotionEffect(type);
			} else {
				plEffect = player.getActivePotionEffects().stream().filter(e -> e.getType().equals(type)).findAny().get();
			}
			if (!onlyIfStronger ||
				plEffect.getAmplifier() < effect.getAmplifier() ||
				(plEffect.getAmplifier() == effect.getAmplifier() && plEffect.getDuration() < effect.getDuration())) {
				player.removePotionEffect(type);
			} else {
				return;
			}
		}
		effect.apply(player);
	}

	/**
	 * Load A List of Strings from config, if found a single String, will convert to List
	 */
	@Nullable
	public static List<String> loadCfgStringList(ConfigurationSection cfg, String path) {
		if (cfg.isString(path)) {
			List<String> list = new ArrayList<>(1);
			list.add(cfg.getString(path));
			return list;
		} else if (cfg.isList(path)) {
			return cfg.getStringList(path);
		}
		return null;
	}

	/**
	 * Load a String from config, if found a List, will return the first String
	 */
	@Nullable
	public static String loadCfgString(ConfigurationSection cfg, String path) {
		if (cfg.isString(path)) {
			return cfg.getString(path);
		} else if (cfg.isList(path)) {
			List<String> list = cfg.getStringList(path);
			if (!list.isEmpty()) {
				return list.get(0);
			}
		}
		return null;
	}

	/* **************************************** */
	/* *********                      ********* */
	/* *********     String Utils     ********* */
	/* *********                      ********* */
	/* **************************************** */

	/**
	 * Returns the Index of a String from the list that contains this substring
	 *
	 * @param list The List in which to search for a substring
	 * @param substring Part of the String to search for in each of <tt>list</tt>
	 */
	public static int indexOfSubstring(List<String> list, String substring) {
		if (list.isEmpty()) return -1;
		for (int index = 0, size = list.size(); index < size; index++) {
			String string = list.get(index);
			if (string.contains(substring)) {
				return index;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of a String from the list that starts with 'lineStart', returns -1 if not found;
	 */
	public static int indexOfStart(List<String> list, String lineStart) {
		for (int i = 0, size = list.size(); i < size; i++) {
			if (list.get(i).startsWith(lineStart)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Replaces the Placeholders %player_name% and %quality% in the given input string
	 *
	 * @param input The String to replace the placeholders in
	 * @param player Player Name to replace %player_name%
	 * @param quality Quality to replace %quality%
	 * @return The String with all placeholders replaced
	 */
	public static String applyPlaceholders(String input, String player, int quality) {
		return input.replaceAll("%player_name%", player).replaceAll("%quality%", String.valueOf(quality));
	}

	/* **************************************** */
	/* *********                      ********* */
	/* *********     Brewery Utils    ********* */
	/* *********                      ********* */
	/* **************************************** */

	/**
	 * create empty World save Sections
	 */
	public static void createWorldSections(ConfigurationSection section) {
		for (World world : BreweryPlugin.getInstance().getServer().getWorlds()) {
			String worldName = world.getName();
			if (worldName.startsWith("DXL_")) {
				worldName = getDxlName(worldName);
			} else {
				worldName = world.getUID().toString();
			}
			section.createSection(worldName);
		}
	}

	/**
	 * Returns true if the Block can be destroyed by the Player or something else (null)
	 *
	 * @param player The Player that destroyed a Block, Null if no Player involved
	 * @return True if the Block can be destroyed
	 */
	public static boolean blockDestroy(Block block, Player player, BarrelDestroyEvent.Reason reason) {
		if (block == null || block.getType() == null) {
			return true;
		}
		Material type = block.getType();
		if (type == Material.CAULDRON || type == LegacyUtil.WATER_CAULDRON) {
			// will only remove when existing
			BCauldron.remove(block);
			return true;

		} else if (LegacyUtil.isFence(type)) {
			// remove barrel and throw potions on the ground
			Barrel barrel = Barrel.getBySpigot(block);
			if (barrel != null) {
				if (barrel.hasPermsDestroy(player, block, reason)) {
					barrel.remove(null, player, true);
					return true;
				} else {
					return false;
				}
			}
			return true;

		} else if (LegacyUtil.isSign(type)) {
			// remove small Barrels
			Barrel barrel2 = Barrel.getBySpigot(block);
			if (barrel2 != null) {
				if (!barrel2.isLarge()) {
					if (barrel2.hasPermsDestroy(player, block, reason)) {
						barrel2.remove(null, player, true);
						return true;
					} else {
						return false;
					}
				} else {
					barrel2.getBody().destroySign();
				}
			}
			return true;

		} else if (LegacyUtil.isWoodPlanks(type) || LegacyUtil.isWoodStairs(type)){
			Barrel barrel3 = Barrel.getByWood(block);
			if (barrel3 != null) {
				if (barrel3.hasPermsDestroy(player, block, reason)) {
					barrel3.remove(block, player, true);
				} else {
					return false;
				}
			}
		}
		return true;
	}

	/* **************************************** */
	/* *********                      ********* */
	/* *********     Other Utils      ********* */
	/* *********                      ********* */
	/* **************************************** */

	/**
	 * prints a list of Strings at the specified page
	 *
	 * @param sender The CommandSender to send the Page to
	 */
	public static void list(CommandSender sender, ArrayList<String> strings, int page) {
		int pages = (int) Math.ceil(strings.size() / 7F);
		if (page > pages || page < 1) {
			page = 1;
		}

		sender.sendMessage(color("&7-------------- &f" + BreweryPlugin.getInstance().languageReader.get("Etc_Page") + " &6" + page + "&f/&6" + pages + " &7--------------"));

		ListIterator<String> iter = strings.listIterator((page - 1) * 7);

		for (int i = 0; i < 7; i++) {
			if (iter.hasNext()) {
				sender.sendMessage(color(iter.next()));
			} else {
				break;
			}
		}
	}

	/**
	 * gets the Name of a DXL World
	 */
	public static String getDxlName(String worldName) {
		File dungeonFolder = new File(worldName);
		if (dungeonFolder.isDirectory()) {
			for (File file : dungeonFolder.listFiles()) {
				if (!file.isDirectory()) {
					if (file.getName().startsWith(".id_")) {
						return file.getName().substring(1).toLowerCase();
					}
				}
			}
		}
		return worldName;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static void saveFile(InputStream in, File dest, String name, boolean overwrite) throws IOException {
		if (in == null) return;
		if (!dest.exists()) {
			dest.mkdirs();
		}
		File result = new File(dest, name);
		if (result.exists()) {
			if (overwrite) {
				result.delete();
			} else {
				return;
			}
		}

		OutputStream out = new FileOutputStream(result);
		byte[] buffer = new byte[1024];

		int length;
		//copy the file content in bytes
		while ((length = in.read(buffer)) > 0){
			out.write(buffer, 0, length);
		}

		in.close();
		out.close();
	}

}
