package com.dre.brewery;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;

public class BPlayer {
	private static Map<String, BPlayer> players = new HashMap<String, BPlayer>();// Players name/uuid and BPlayer
	private static Map<Player, Integer> pTasks = new HashMap<Player, Integer>();// Player and count
	private static int taskId;

	// Settings
	public static Map<Material, Integer> drainItems = new HashMap<Material, Integer>();// DrainItem Material and Strength
	public static Material pukeItem;
	public static int hangoverTime;
	public static boolean overdrinkKick;
	public static boolean enableHome;
	public static boolean enableLoginDisallow;
	public static boolean enablePuke;
	public static String homeType;

	private int quality = 0;// = quality of drunkeness * drunkeness
	private int drunkeness = 0;// = amount of drunkeness
	private boolean passedOut = false;// if kicked because of drunkeness
	private int offlineDrunk = 0;// drunkeness when gone offline
	private Vector push = new Vector(0, 0, 0);
	private int time = 20;

	public BPlayer() {
	}

	// reading from file
	public BPlayer(String name, int quality, int drunkeness, int offlineDrunk, Boolean passedOut) {
		this.quality = quality;
		this.drunkeness = drunkeness;
		this.offlineDrunk = offlineDrunk;
		this.passedOut = passedOut;
		players.put(name, this);
	}

	public static BPlayer get(Player player) {
		if (!players.isEmpty()) {
			return players.get(P.playerString(player));
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
		return players.containsKey(P.playerString(player));
	}

	// Create a new BPlayer and add it to the list
	public static BPlayer addPlayer(Player player) {
		BPlayer bPlayer = new BPlayer();
		players.put(P.playerString(player), bPlayer);
		return bPlayer;
	}

	public static void remove(Player player) {
		players.remove(P.playerString(player));
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
		if (overdrinkKick && !player.hasPermission("brewery.bypass.overdrink")) {
			passOut(player);
		} else {
			quality = getQuality() * 100;
			drunkeness = 100;
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
				if (drunkeness <= -hangoverTime) {
					return true;
				}
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
						Entity entity = (Entity) player;
						// not in midair
						if (entity.isOnGround()) {
							time--;
							if (time == 0) {
								// push him only to the side? or any direction
								// like now
								push.setX(Math.random() - 0.5);
								push.setZ(Math.random() - 0.5);
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
		passedOut = true;
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
		if (drunkeness <= 80) {
			if (passedOut) {
				// he has suffered enough. Let him through
				return 0;
			}
			if (Math.random() > 0.4) {
				return 0;
			} else {
				return 2;
			}
		}
		if (drunkeness <= 100) {
			if (!passedOut) {
				if (Math.random() > 0.6) {
					return 0;
				} else {
					return 2;
				}
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
			players.remove(P.playerString(player));

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
		passedOut = false;
	}

	public void disconnecting() {
		offlineDrunk = drunkeness;
	}

	public void goHome(final Player player) {
		if (homeType != null) {
			Location home = null;
			if (homeType.equalsIgnoreCase("bed")) {
				home = player.getBedSpawnLocation();
			} else if (homeType.equalsIgnoreCase("ManagerXL")) {
				if (com.dre.managerxl.MPlayer.get(player.getName()) != null) {
					home = com.dre.managerxl.MPlayer.get(player.getName()).getHome();
				}
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
		pTasks.put(player, count);
	}

	public static void pukeTask() {
		for (Player player : pTasks.keySet()) {
			puke(player);
			int newCount = pTasks.get(player) - 1;
			if (newCount == 0) {
				pTasks.remove(player);
			} else {
				pTasks.put(player, newCount);
			}
		}
		if (pTasks.isEmpty()) {
			P.p.getServer().getScheduler().cancelTask(taskId);
		}
	}

	public static void puke(Player player) {
		Location loc = player.getLocation();
		Vector direction = loc.getDirection();
		direction.multiply(0.5);
		loc.setY(loc.getY() + 1.5);
		loc.setPitch(loc.getPitch() + 10);
		loc.add(direction);
		Item item = player.getWorld().dropItem(loc, new ItemStack(pukeItem));
		item.setVelocity(direction);
		item.setPickupDelay(Integer.MAX_VALUE);
	}


	// #### Effects ####

	public void drunkEffects(Player player) {
		int duration = 10 - getQuality();
		duration += drunkeness / 2;
		duration *= 20;
		if (duration > 960) {
			duration *= 5;
		} else if (duration < 460) {
			duration = 460;
		}
		PotionEffectType.CONFUSION.createEffect(duration, 0).apply(player);
	}

	public static void addQualityEffects(int quality, int brewAlc, Player player) {
		int duration = 7 - quality;
		if (quality == 0) {
			duration *= 500;
		} else if (quality <= 5) {
			duration *= 250;
		} else {
			duration = 100;
			if (brewAlc <= 10) {
				duration = 0;
			}
		}
		if (duration > 0) {
			PotionEffectType.POISON.createEffect(duration, 0).apply(player);
		}

		if (brewAlc > 10) {
			if (quality <= 5) {
				duration = 10 - quality;
				duration += brewAlc;
				duration *= 60;
			} else {
				duration = 120;
			}
			PotionEffectType.BLINDNESS.createEffect(duration, 0).apply(player);
		}
	}

	public static void addBrewEffects(Brew brew, Player player) {
		Map<String, Integer> effects = brew.getEffects();
		if (effects != null) {
			for (Map.Entry<String, Integer> entry : effects.entrySet()) {
				PotionEffectType type = PotionEffectType.getByName(entry.getKey().replace("X", ""));
				if (type != null) {
					int duration = (entry.getValue() * brew.getQuality()) / 8;
					if (type.isInstant()) {
						type.createEffect(0, duration - 1).apply(player);
					} else {
						int amplifier = brew.getQuality() / 4;
						duration /= type.getDurationModifier();
						type.createEffect(duration * 20, amplifier).apply(player);
					}
				}
			}
		}
	}

	public void hangoverEffects(final Player player) {
		int duration = offlineDrunk * 50 * getHangoverQuality();
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
					Player player = P.getPlayerfromString(name);
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
				if (bplayer.drain(P.getPlayerfromString(name), soberPerMin)) {
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
			if (bPlayer.passedOut) {
				section.set("passedOut", true);
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
		return Math.round(quality / drunkeness);
	}

	// opposite of quality
	public int getHangoverQuality() {
		if (drunkeness < 0) {
			return quality + 11;
		}
		return -getQuality() + 11;
	}

}