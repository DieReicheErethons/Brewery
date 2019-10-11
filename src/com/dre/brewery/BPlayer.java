package com.dre.brewery;

import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class BPlayer {
	private static Map<String, BPlayer> players = new HashMap<>();// Players name/uuid and BPlayer
	private static Map<Player, MutableInt> pTasks = new HashMap<>();// Player and count
	private static int taskId;
	private static boolean modAge = true;
	private static Random pukeRand;
	private static Method gh;
	private static Field age;

	// Settings
	public static Map<Material, Integer> drainItems = new HashMap<>();// DrainItem Material and Strength
	public static Material pukeItem;
	public static int pukeDespawntime;
	public static int hangoverTime;
	public static boolean overdrinkKick;
	public static boolean enableHome;
	public static boolean enableLoginDisallow;
	public static boolean enablePuke;
	public static String homeType;

	private int quality = 0;// = quality of drunkeness * drunkeness
	private int drunkeness = 0;// = amount of drunkeness
	private int offlineDrunk = 0;// drunkeness when gone offline
	private Vector push = new Vector(0, 0, 0);
	private int time = 20;

	public BPlayer() {
	}

	// reading from file
	public BPlayer(String name, int quality, int drunkeness, int offlineDrunk) {
		this.quality = quality;
		this.drunkeness = drunkeness;
		this.offlineDrunk = offlineDrunk;
		players.put(name, this);
	}

	public static BPlayer get(Player player) {
		if (!players.isEmpty()) {
			return players.get(Util.playerString(player));
		}
		return null;
	}

	// This method may be slow and should not be used if not needed
	public static BPlayer getByName(String playerName) {
		if (P.useUUID) {
			for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
				OfflinePlayer p = P.p.getServer().getOfflinePlayer(UUID.fromString(entry.getKey()));
				if (p != null) {
					String name = p.getName();
					if (name != null) {
						if (name.equalsIgnoreCase(playerName)) {
							return entry.getValue();
						}
					}
				}
			}
			return null;
		}
		return players.get(playerName);
	}

	// This method may be slow and should not be used if not needed
	public static boolean hasPlayerbyName(String playerName) {
		if (P.useUUID) {
			for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
				OfflinePlayer p = P.p.getServer().getOfflinePlayer(UUID.fromString(entry.getKey()));
				if (p != null) {
					String name = p.getName();
					if (name != null) {
						if (name.equalsIgnoreCase(playerName)) {
							return true;
						}
					}
				}
			}
			return false;
		}
		return players.containsKey(playerName);
	}

	public static boolean isEmpty() {
		return players.isEmpty();
	}

	public static boolean hasPlayer(Player player) {
		return players.containsKey(Util.playerString(player));
	}

	// Create a new BPlayer and add it to the list
	public static BPlayer addPlayer(Player player) {
		BPlayer bPlayer = new BPlayer();
		players.put(Util.playerString(player), bPlayer);
		return bPlayer;
	}

	public static void remove(Player player) {
		players.remove(Util.playerString(player));
	}

	public static int numDrunkPlayers() {
		return players.size();
	}

	public void remove() {
		for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
			if (entry.getValue() == this) {
				players.remove(entry.getKey());
				return;
			}
		}
	}

	public static void clear() {
		players.clear();
	}

	// Drink a brew and apply effects, etc.
	public static void drink(Brew brew, Player player) {
		int brewAlc = brew.calcAlcohol();
		if (brewAlc == 0) {
			//no alcohol so we dont need to add a BPlayer
			addBrewEffects(brew, player);
			return;
		}
		BPlayer bPlayer = get(player);
		if (bPlayer == null) {
			bPlayer = addPlayer(player);
		}
		bPlayer.drunkeness += brewAlc;
		if (brew.getQuality() > 0) {
			bPlayer.quality += brew.getQuality() * brewAlc;
		} else {
			bPlayer.quality += brewAlc;
		}

		if (bPlayer.drunkeness <= 100) {

			addBrewEffects(brew, player);
			addQualityEffects(brew.getQuality(), brewAlc, player);

		} else {
			bPlayer.drinkCap(player);
		}
	}

	// Player has drunken too much
	public void drinkCap(Player player) {
		quality = getQuality() * 100;
		drunkeness = 100;
		if (overdrinkKick && !player.hasPermission("brewery.bypass.overdrink")) {
			P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, () -> passOut(player), 1);
		} else {
			addPuke(player, 60 + (int) (Math.random() * 60.0));
			P.p.msg(player, P.p.languageReader.get("Player_CantDrink"));
		}
	}

	// push the player around if he moves
	public static void playerMove(PlayerMoveEvent event) {
		BPlayer bPlayer = get(event.getPlayer());
		if (bPlayer != null) {
			bPlayer.move(event);
		}
	}

	// Eat something to drain the drunkeness
	public void drainByItem(Player player, Material mat) {
		int strength = drainItems.get(mat);
		if (drain(player, strength)) {
			remove(player);
		}
	}

	// drain the drunkeness by amount, returns true when player has to be removed
	public boolean drain(Player player, int amount) {
		if (drunkeness > 0) {
			quality -= getQuality() * amount;
		}
		drunkeness -= amount;
		if (drunkeness > 0) {
			if (offlineDrunk == 0) {
				if (player == null) {
					offlineDrunk = drunkeness;
				}
			}
		} else {
			if (offlineDrunk == 0) {
				return true;
			}
			quality = getQuality();
			if (drunkeness <= -offlineDrunk) {
				return drunkeness <= -hangoverTime;
			}
		}
		return false;
	}

	// player is drunk
	public void move(PlayerMoveEvent event) {
		// has player more alc than 10
		if (drunkeness >= 10) {
			if (drunkeness <= 100) {
				if (time > 1) {
					time--;
				} else {
					// Is he moving
					if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
						Player player = event.getPlayer();
						// We have to cast here because it had issues otherwise on previous versions of Minecraft
						// Dont know if thats still the case, but we better leave it
						Entity entity = (Entity) player;
						// not in midair
						if (entity.isOnGround()) {
							time--;
							if (time == 0) {
								// push him only to the side? or any direction
								// like now
								if (P.use1_9) { // Pushing is way stronger in 1.9
									push.setX((Math.random() - 0.5) / 2.0);
									push.setZ((Math.random() - 0.5) / 2.0);
								} else {
									push.setX(Math.random() - 0.5);
									push.setZ(Math.random() - 0.5);
								}
								player.setVelocity(push);
							} else if (time < 0 && time > -10) {
								// push him some more in the same direction
								player.setVelocity(push);
							} else {
								// when more alc, push him more often
								time = (int) (Math.random() * (201.0 - (drunkeness * 2)));
							}
						}
					}
				}
			}
		}
	}

	public void passOut(Player player) {
		player.kickPlayer(P.p.languageReader.get("Player_DrunkPassOut"));
		offlineDrunk = drunkeness;
	}


	// #### Login ####

	// can the player login or is he too drunk
	public int canJoin() {
		if (drunkeness <= 70) {
			return 0;
		}
		if (!enableLoginDisallow) {
			if (drunkeness <= 100) {
				return 0;
			} else {
				return 3;
			}
		}
		if (drunkeness <= 90) {
			if (Math.random() > 0.4) {
				return 0;
			} else {
				return 2;
			}
		}
		if (drunkeness <= 100) {
			if (Math.random() > 0.6) {
				return 0;
			} else {
				return 2;
			}
		}
		return 3;
	}

	// player joins
	public void join(final Player player) {
		if (offlineDrunk == 0) {
			return;
		}
		// delayed login event as the player is not fully accessible pre login
		P.p.getServer().getScheduler().runTaskLater(P.p, new Runnable() {
			public void run() {
				login(player);
			}
		}, 1L);
	}

	// he may be having a hangover
	public void login(final Player player) {
		if (drunkeness < 10) {
			if (offlineDrunk > 60) {
				if (enableHome && !player.hasPermission("brewery.bypass.teleport")) {
					goHome(player);
				}
			}
			hangoverEffects(player);
			// wird der spieler noch gebraucht?
			players.remove(Util.playerString(player));

		} else if (offlineDrunk - drunkeness >= 30) {
			Location randomLoc = Wakeup.getRandom(player.getLocation());
			if (randomLoc != null) {
				if (!player.hasPermission("brewery.bypass.teleport")) {
					player.teleport(randomLoc);
					P.p.msg(player, P.p.languageReader.get("Player_Wake"));
				}
			}
		}

		offlineDrunk = 0;
	}

	public void disconnecting() {
		offlineDrunk = drunkeness;
	}

	public void goHome(final Player player) {
		if (homeType != null) {
			Location home = null;
			if (homeType.equalsIgnoreCase("bed")) {
				home = player.getBedSpawnLocation();
			} else if (homeType.startsWith("cmd: ")) {
				player.performCommand(homeType.substring(5));
			} else if (homeType.startsWith("cmd:")) {
				player.performCommand(homeType.substring(4));
			} else {
				P.p.errorLog("Config.yml 'homeType: " + homeType + "' unknown!");
			}
			if (home != null) {
				player.teleport(home);
			}
		}
	}


	// #### Puking ####

	// Chance that players puke on big drunkeness
	// runs every 6 sec, average chance is 10%, so should puke about every 60 sec
	// good quality can decrease the chance by up to 10%
	public void drunkPuke(Player player) {
		if (drunkeness >= 80) {
			if (drunkeness >= 90) {
				if (Math.random() < 0.15 - (getQuality() / 100)) {
					addPuke(player, 20 + (int) (Math.random() * 40));
				}
			} else {
				if (Math.random() < 0.08 - (getQuality() / 100)) {
					addPuke(player, 10 + (int) (Math.random() * 30));
				}
			}
		}
	}

	// make a Player puke "count" items
	public static void addPuke(Player player, int count) {
		if (!enablePuke) {
			return;
		}

		if (pTasks.isEmpty()) {
			taskId = P.p.getServer().getScheduler().scheduleSyncRepeatingTask(P.p, new Runnable() {
				public void run() {
					pukeTask();
				}
			}, 1L, 1L);
		}
		pTasks.put(player, new MutableInt(count));
	}

	public static void pukeTask() {
		for (Iterator<Map.Entry<Player, MutableInt>> iter = pTasks.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry<Player, MutableInt> entry = iter.next();
			Player player = entry.getKey();
			MutableInt count = entry.getValue();
			if (!player.isValid() || !player.isOnline()) {
				iter.remove();
			}
			puke(player);
			count.decrement();
			if (count.intValue() <= 0) {
				iter.remove();
			}
		}
		if (pTasks.isEmpty()) {
			P.p.getServer().getScheduler().cancelTask(taskId);
		}
	}

	public static void puke(Player player) {
		if (pukeRand == null) {
			pukeRand = new Random();
		}
		if (pukeItem == null || pukeItem == Material.AIR) {
			pukeItem = Material.SOUL_SAND;
		}
		Location loc = player.getLocation();
		loc.setY(loc.getY() + 1.1);
		loc.setPitch(loc.getPitch() - 10 + pukeRand.nextInt(20));
		loc.setYaw(loc.getYaw() - 10 + pukeRand.nextInt(20));
		Vector direction = loc.getDirection();
		direction.multiply(0.5);
		loc.add(direction);
		Item item = player.getWorld().dropItem(loc, new ItemStack(pukeItem));
		item.setVelocity(direction);
		item.setPickupDelay(32767); // Item can never be picked up when pickup delay is 32767
		//item.setTicksLived(6000 - pukeDespawntime); // Well this does not work...
		if (modAge) {
			if (pukeDespawntime >= 5800) {
				return;
			}
			try {
				if (gh == null) {
					gh = Class.forName(P.p.getServer().getClass().getPackage().getName() + ".entity.CraftItem").getMethod("getHandle", (Class<?>[]) null);
				}
				Object entityItem = gh.invoke(item, (Object[]) null);
				if (age == null) {
					age = entityItem.getClass().getDeclaredField("age");
					age.setAccessible(true);
				}

				// Setting the age determines when an item is despawned. At age 6000 it is removed.
				if (pukeDespawntime <= 0) {
					// Just show the item for a tick
					age.setInt(entityItem, 5999);
				} else if (pukeDespawntime <= 120) {
					// it should despawn in less than 6 sec. Add up to half of that randomly
					age.setInt(entityItem, 6000 - pukeDespawntime + pukeRand.nextInt((int) (pukeDespawntime / 2F)));
				} else {
					// Add up to 5 sec randomly
					age.setInt(entityItem, 6000 - pukeDespawntime + pukeRand.nextInt(100));
				}
				return;
			} catch (InvocationTargetException | ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
				e.printStackTrace();
			}
			modAge = false;
			P.p.errorLog("Failed to set Despawn Time on item " + pukeItem.name());
		}
	}


	// #### Effects ####

	public void drunkEffects(Player player) {
		int duration = 10 - getQuality();
		duration += drunkeness / 2;
		duration *= 5;
		if (duration > 240) {
			duration *= 5;
		} else if (duration < 115) {
			duration = 115;
		}
		if (!P.use1_14) {
			duration *= 4;
		}
		PotionEffectType.CONFUSION.createEffect(duration, 0).apply(player);
	}

	public static void addQualityEffects(int quality, int brewAlc, Player player) {
		int duration = 7 - quality;
		if (quality == 0) {
			duration *= 125;
		} else if (quality <= 5) {
			duration *= 62;
		} else {
			duration = 25;
			if (brewAlc <= 10) {
				duration = 0;
			}
		}
		if (!P.use1_14) {
			duration *= 4;
		}
		if (duration > 0) {
			PotionEffectType.POISON.createEffect(duration, 0).apply(player);
		}

		if (brewAlc > 10) {
			if (quality <= 5) {
				duration = 10 - quality;
				duration += brewAlc;
				duration *= 15;
			} else {
				duration = 30;
			}
			if (!P.use1_14) {
				duration *= 4;
			}
			PotionEffectType.BLINDNESS.createEffect(duration, 0).apply(player);
		}
	}

	public static void addBrewEffects(Brew brew, Player player) {
		ArrayList<BEffect> effects = brew.getEffects();
		if (effects != null) {
			for (BEffect effect : effects) {
				effect.apply(brew.getQuality(), player);
			}
		}
	}

	public void hangoverEffects(final Player player) {
		int duration = offlineDrunk * 25 * getHangoverQuality();
		if (!P.use1_14) {
			duration *= 2;
		}
		int amplifier = getHangoverQuality() / 3;

		PotionEffectType.SLOW.createEffect(duration, amplifier).apply(player);
		PotionEffectType.HUNGER.createEffect(duration, amplifier).apply(player);
	}


	// #### Sheduled ####

	public static void drunkeness() {
		for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
			String name = entry.getKey();
			BPlayer bplayer = entry.getValue();

			if (bplayer.drunkeness > 30) {
				if (bplayer.offlineDrunk == 0) {
					Player player = Util.getPlayerfromString(name);
					if (player != null) {

						bplayer.drunkEffects(player);

						if (enablePuke) {
							bplayer.drunkPuke(player);
						}

					}
				}
			}
		}
	}

	// decreasing drunkeness over time
	public static void onUpdate() {
		if (!players.isEmpty()) {
			int soberPerMin = 2;
			Iterator<Map.Entry<String, BPlayer>> iter = players.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, BPlayer> entry = iter.next();
				String name = entry.getKey();
				BPlayer bplayer = entry.getValue();
				if (bplayer.drunkeness == soberPerMin) {
					// Prevent 0 drunkeness
					soberPerMin++;
				}
				if (bplayer.drain(Util.getPlayerfromString(name), soberPerMin)) {
					iter.remove();
				}
			}
		}
	}

	// save all data
	public static void save(ConfigurationSection config) {
		for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
			ConfigurationSection section = config.createSection(entry.getKey());
			BPlayer bPlayer = entry.getValue();
			section.set("quality", bPlayer.quality);
			section.set("drunk", bPlayer.drunkeness);
			if (bPlayer.offlineDrunk != 0) {
				section.set("offDrunk", bPlayer.offlineDrunk);
			}
		}
	}


	// #### getter/setter ####

	public int getDrunkeness() {
		return drunkeness;
	}

	public void setData(int drunkeness, int quality) {
		if (quality > 0) {
			this.quality = quality * drunkeness;
		} else {
			if (this.quality == 0) {
				this.quality = 5 * drunkeness;
			} else {
				this.quality = getQuality() * drunkeness;
			}
		}
		this.drunkeness = drunkeness;
	}

	public int getQuality() {
		if (drunkeness == 0) {
			P.p.errorLog("drunkeness should not be 0!");
			return quality;
		}
		if (drunkeness < 0) {
			return quality;
		}
		return Math.round((float) quality / (float) drunkeness);
	}

	// opposite of quality
	public int getHangoverQuality() {
		if (drunkeness < 0) {
			return quality + 11;
		}
		return -getQuality() + 11;
	}

}
