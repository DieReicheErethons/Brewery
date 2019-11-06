package com.dre.brewery;

import com.dre.brewery.api.events.PlayerAlcEffectEvent;
import com.dre.brewery.api.events.PlayerDrinkEffectEvent;
import com.dre.brewery.api.events.PlayerPukeEvent;
import com.dre.brewery.api.events.PlayerPushEvent;
import com.dre.brewery.api.events.brew.BrewDrinkEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.recipe.BEffect;
import com.dre.brewery.utility.BUtil;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

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

	@Nullable
	public static BPlayer get(Player player) {
		if (!players.isEmpty()) {
			return players.get(BUtil.playerString(player));
		}
		return null;
	}

	// This method may be slow and should not be used if not needed
	@Nullable
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
		return players.containsKey(BUtil.playerString(player));
	}

	// Create a new BPlayer and add it to the list
	public static BPlayer addPlayer(Player player) {
		BPlayer bPlayer = new BPlayer();
		players.put(BUtil.playerString(player), bPlayer);
		return bPlayer;
	}

	public static void remove(Player player) {
		players.remove(BUtil.playerString(player));
	}

	public static int numDrunkPlayers() {
		return players.size();
	}

	public void remove() {
		for (Iterator<Map.Entry<String, BPlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<String, BPlayer> entry = iterator.next();
			if (entry.getValue() == this) {
				iterator.remove();
				return;
			}
		}
	}

	public static void clear() {
		players.clear();
	}

	// Drink a brew and apply effects, etc.
	public static boolean drink(Brew brew, ItemMeta meta, Player player) {
		BPlayer bPlayer = get(player);
		if (bPlayer == null) {
			bPlayer = addPlayer(player);
		}
		BrewDrinkEvent drinkEvent = new BrewDrinkEvent(brew, meta, player, bPlayer);
		P.p.getServer().getPluginManager().callEvent(drinkEvent);
		if (drinkEvent.isCancelled()) {
			if (bPlayer.drunkeness <= 0) {
				bPlayer.remove();
			}
			return false;
		}

		int brewAlc = drinkEvent.getAddedAlcohol();
		int quality = drinkEvent.getQuality();
		List<PotionEffect> effects = getBrewEffects(brew.getEffects(), quality);

		if (brewAlc < 1) {
			//no alcohol so we dont need to add a BPlayer
			applyDrinkEffects(effects, player);
			if (bPlayer.drunkeness <= 0) {
				bPlayer.remove();
			}
			return true;
		}

		effects.addAll(getQualityEffects(drinkEvent.getQuality(), brewAlc));
		bPlayer.drunkeness += brewAlc;
		if (quality > 0) {
			bPlayer.quality += quality * brewAlc;
		} else {
			bPlayer.quality += brewAlc;
		}
		applyDrinkEffects(effects, player);

		if (bPlayer.drunkeness > 100) {
			bPlayer.drinkCap(player);
		}
		return true;
	}

	// Player has drunken too much
	public void drinkCap(Player player) {
		quality = getQuality() * 100;
		drunkeness = 100;
		if (BConfig.overdrinkKick && !player.hasPermission("brewery.bypass.overdrink")) {
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
		int strength = BConfig.drainItems.get(mat);
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
				return drunkeness <= -BConfig.hangoverTime;
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
								PlayerPushEvent pushEvent = new PlayerPushEvent(player, push, this);
								P.p.getServer().getPluginManager().callEvent(pushEvent);
								push = pushEvent.getPush();
								if (pushEvent.isCancelled() || push.lengthSquared() <= 0) {
									time = -10;
									return;
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
		if (!BConfig.enableLoginDisallow) {
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
				if (BConfig.enableHome && !player.hasPermission("brewery.bypass.teleport")) {
					goHome(player);
				}
			}
			hangoverEffects(player);
			// wird der spieler noch gebraucht?
			players.remove(BUtil.playerString(player));

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
		String homeType = BConfig.homeType;
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
		if (!BConfig.enablePuke) {
			return;
		}

		PlayerPukeEvent event = new PlayerPukeEvent(player, count);
		P.p.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled() || event.getCount() < 1) {
			return;
		}

		if (pTasks.isEmpty()) {
			taskId = P.p.getServer().getScheduler().scheduleSyncRepeatingTask(P.p, new Runnable() {
				public void run() {
					pukeTask();
				}
			}, 1L, 1L);
		}
		pTasks.put(player, new MutableInt(event.getCount()));
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
		if (BConfig.pukeItem == null || BConfig.pukeItem == Material.AIR) {
			BConfig.pukeItem = Material.SOUL_SAND;
		}
		Location loc = player.getLocation();
		loc.setY(loc.getY() + 1.1);
		loc.setPitch(loc.getPitch() - 10 + pukeRand.nextInt(20));
		loc.setYaw(loc.getYaw() - 10 + pukeRand.nextInt(20));
		Vector direction = loc.getDirection();
		direction.multiply(0.5);
		loc.add(direction);
		Item item = player.getWorld().dropItem(loc, new ItemStack(BConfig.pukeItem));
		item.setVelocity(direction);
		item.setPickupDelay(32767); // Item can never be picked up when pickup delay is 32767
		//item.setTicksLived(6000 - pukeDespawntime); // Well this does not work...
		if (modAge) {
			int pukeDespawntime = BConfig.pukeDespawntime;
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
			P.p.errorLog("Failed to set Despawn Time on item " + BConfig.pukeItem.name());
		}
	}


	// #### Effects ####

	public static void applyDrinkEffects(List<PotionEffect> effects, Player player) {
		PlayerDrinkEffectEvent event = new PlayerDrinkEffectEvent(player, effects);
		P.p.getServer().getPluginManager().callEvent(event);
		effects = event.getEffects();
		if (event.isCancelled() || effects == null) {
			return;
		}
		for (PotionEffect effect : effects) {
			BUtil.reapplyPotionEffect(player, effect, true);
		}
	}

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
		List<PotionEffect> l = new ArrayList<>(1);
		l.add(PotionEffectType.CONFUSION.createEffect(duration, 0));

		PlayerAlcEffectEvent event = new PlayerAlcEffectEvent(player, l);
		P.p.getServer().getPluginManager().callEvent(event);
		l = event.getEffects();
		if (event.isCancelled() || l == null) {
			return;
		}
		for (PotionEffect effect : l) {
			effect.apply(player);
		}
	}

	public static List<PotionEffect> getQualityEffects(int quality, int brewAlc) {
		List<PotionEffect> out = new ArrayList<>(2);
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
			out.add(PotionEffectType.POISON.createEffect(duration, 0));
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
			out.add(PotionEffectType.BLINDNESS.createEffect(duration, 0));
		}
		return out;
	}

	public static void addQualityEffects(int quality, int brewAlc, Player player) {
		for (PotionEffect effect : getQualityEffects(quality, brewAlc)) {
			BUtil.reapplyPotionEffect(player, effect, true);
		}
	}

	public static List<PotionEffect> getBrewEffects(List<BEffect> effects, int quality) {
		List<PotionEffect> out = new ArrayList<>();
		if (effects != null) {
			for (BEffect effect : effects) {
				PotionEffect e = effect.generateEffect(quality);
				if (e != null) {
					out.add(e);
				}
			}
		}
		return out;
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

		BUtil.reapplyPotionEffect(player, PotionEffectType.SLOW.createEffect(duration, amplifier), true);
		BUtil.reapplyPotionEffect(player, PotionEffectType.HUNGER.createEffect(duration, amplifier), true);
	}


	// #### Scheduled ####

	public static void drunkeness() {
		for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
			String name = entry.getKey();
			BPlayer bplayer = entry.getValue();

			if (bplayer.drunkeness > 30) {
				if (bplayer.offlineDrunk == 0) {
					Player player = BUtil.getPlayerfromString(name);
					if (player != null) {

						bplayer.drunkEffects(player);

						if (BConfig.enablePuke) {
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
				if (bplayer.drain(BUtil.getPlayerfromString(name), soberPerMin)) {
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
