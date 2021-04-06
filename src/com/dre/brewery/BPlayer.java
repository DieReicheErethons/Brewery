package com.dre.brewery;

import com.dre.brewery.api.events.PlayerEffectEvent;
import com.dre.brewery.api.events.PlayerPukeEvent;
import com.dre.brewery.api.events.PlayerPushEvent;
import com.dre.brewery.api.events.brew.BrewDrinkEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.lore.BrewLore;
import com.dre.brewery.recipe.BEffect;
import com.dre.brewery.utility.BUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
	private static Map<String, BPlayer> players = new HashMap<>();// Players uuid and BPlayer
	private static Map<Player, MutableInt> pTasks = new HashMap<>();// Player and count
	private static int taskId;
	private static boolean modAge = true;
	private static Random pukeRand;
	private static Method itemHandle;
	private static Field age;

	private final String uuid;
	private int quality = 0;// = quality of drunkeness * drunkeness
	private int drunkeness = 0;// = amount of drunkeness
	private int offlineDrunk = 0;// drunkeness when gone offline
	private Vector push = new Vector(0, 0, 0);
	private int time = 20;

	public BPlayer(String uuid) {
		this.uuid = uuid;
	}

	// reading from file
	public BPlayer(String uuid, int quality, int drunkeness, int offlineDrunk) {
		this.quality = quality;
		this.drunkeness = drunkeness;
		this.offlineDrunk = offlineDrunk;
		this.uuid = uuid;
		players.put(uuid, this);
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
		BPlayer bPlayer = new BPlayer(BUtil.playerString(player));
		players.put(BUtil.playerString(player), bPlayer);
		return bPlayer;
	}

	public static void remove(Player player) {
		players.remove(BUtil.playerString(player));
		if (BConfig.sqlDrunkSync && BConfig.sqlSync != null) {
			BConfig.sqlSync.removePlayer(player.getUniqueId());
		}
	}

	public static void sqlRemoved(UUID uuid) {
		players.remove(uuid.toString());
	}

	public static int numDrunkPlayers() {
		return players.size();
	}

	public void remove() {
		for (Iterator<Map.Entry<String, BPlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<String, BPlayer> entry = iterator.next();
			if (entry.getValue() == this) {
				if (BConfig.sqlDrunkSync && BConfig.sqlSync != null) {
					BConfig.sqlSync.removePlayer(UUID.fromString(entry.getKey()));
				}
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
		if (meta != null) {
			P.p.getServer().getPluginManager().callEvent(drinkEvent);
			if (drinkEvent.isCancelled()) {
				if (bPlayer.drunkeness <= 0) {
					bPlayer.remove();
				}
				return false;
			}
		}

		if (brew.hasRecipe()) {
			brew.getCurrentRecipe().applyDrinkFeatures(player, brew.getQuality());
		}
		P.p.metricsForDrink(brew);

		int brewAlc = drinkEvent.getAddedAlcohol();
		int quality = drinkEvent.getQuality();
		List<PotionEffect> effects = getBrewEffects(brew.getEffects(), quality);

		applyEffects(effects, player, PlayerEffectEvent.EffectType.DRINK);
		if (brewAlc < 0) {
			bPlayer.drain(player, -brewAlc);
		} else if (brewAlc > 0) {
			bPlayer.drunkeness += brewAlc;
			if (quality > 0) {
				bPlayer.quality += quality * brewAlc;
			} else {
				bPlayer.quality += brewAlc;
			}
			
			applyEffects(getQualityEffects(quality, brewAlc), player, PlayerEffectEvent.EffectType.QUALITY);
		}
		
		if (bPlayer.drunkeness > 100) {
			bPlayer.drinkCap(player);
		}
		bPlayer.syncToSQL(false);
		
		if (BConfig.showStatusOnDrink) {
			bPlayer.showDrunkeness(player);
		}

		if (bPlayer.drunkeness <= 0) {
			bPlayer.remove();
		}
		return true;
	}

	/**
	 * Show the Player his current drunkeness and quality as an Actionbar graphic or when unsupported, in chat
	 */
	public void showDrunkeness(Player player) {
		try {
			// It this returns false, then the Action Bar is not supported. Do not repeat the message as it was sent into chat
			if (sendDrunkenessMessage(player)) {
				P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, () -> sendDrunkenessMessage(player), 40);
				P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, () -> sendDrunkenessMessage(player), 80);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Send one Message to the player, showing his drunkeness or hangover
	 *
	 * @param player The Player to send the message to
	 * @return false if the message should not be repeated.
	 */
	public boolean sendDrunkenessMessage(Player player) {
		StringBuilder b = new StringBuilder(100);

		int strength = drunkeness;
		boolean hangover = false;
		if (offlineDrunk > 0) {
			strength = offlineDrunk;
			hangover = true;
		}

		b.append(P.p.languageReader.get(hangover ? "Player_Hangover" : "Player_Drunkeness"));

		// Drunkeness or Hangover Strength Bars
		b.append(": §7[");
		// Show 25 Bars, color one per 4 drunkeness
		int bars;
		if (strength <= 0) {
			bars = 0;
		} else if (strength == 1) {
			bars = 1;
		} else {
			bars = Math.round(strength / 4.0f);
		}
		int noBars = 25 - bars;
		if (bars > 0) {
			b.append(hangover ? "§c" : "§6");
		}
		for (int addedBars = 0; addedBars < bars; addedBars++) {
			b.append("|");
			if (addedBars == 20) {
				// color the last 4 bars red
				b.append("§c");
			}
		}
		if (noBars > 0) {
			b.append("§0");
			for (; noBars > 0; noBars--) {
				b.append("|");
			}
		}
		b.append("§7] ");


		int quality;
		if (hangover) {
			quality = 11 - getHangoverQuality();
		} else {
			quality = strength > 0 ? getQuality() : 0;
		}

		// Quality Stars
		int stars = quality / 2;
		boolean half = quality % 2 > 0;
		int noStars = 5 - stars - (half ? 1 : 0);

		b.append("§7[").append(BrewLore.getQualityColor(quality));
		for (; stars > 0; stars--) {
			b.append("⭑");
		}
		if (half) {
			b.append("⭒");
		}
		if (noStars > 0) {
			b.append("§0");
			for (; noStars > 0; noStars--) {
				b.append("⭑");
			}
		}
		b.append("§7]");
		final String text = b.toString();
		if (hangover) {
			P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, () -> player.sendTitle("", text, 30, 100, 90), 160);
			return false;
		}
		try {
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
			return true;
		} catch (UnsupportedOperationException | NoSuchMethodError e) {
			player.sendMessage(text);
			return false;
		}
	}

	// Player has drunken too much
	public void drinkCap(Player player) {
		quality = getQuality() * 100;
		drunkeness = 100;
		syncToSQL(false);
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
			if (drunkeness == 0) {
				drunkeness--;
			}
			quality = getQuality();
			if (drunkeness <= -offlineDrunk) {
				syncToSQL(true);
				return drunkeness <= -BConfig.hangoverTime;
			}
		}
		syncToSQL(offlineDrunk > 0);
		return false;
	}

	// player is drunk
	public void move(PlayerMoveEvent event) {
		// has player more alc than 10
		if (drunkeness >= 10 && BConfig.stumbleModifier > 0.001f) {
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
								push.multiply(BConfig.stumbleModifier);
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
		syncToSQL(false);
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
		P.p.getServer().getScheduler().runTaskLater(P.p, () -> login(player), 1L);
	}

	// he may be having a hangover
	public void login(final Player player) {
		if (drunkeness < 10) {
			if (offlineDrunk > 60) {
				if (BConfig.enableHome && !player.hasPermission("brewery.bypass.teleport")) {
					goHome(player);
				}
			}
			if (offlineDrunk > 20) {
				hangoverEffects(player);
				showDrunkeness(player);
			}
			if (drunkeness <= 0) {
				// wird der spieler noch gebraucht?
				remove(player);
			}

		} else if (offlineDrunk - drunkeness >= 30) {
			Location randomLoc = Wakeup.getRandom(player.getLocation());
			if (randomLoc != null) {
				if (!player.hasPermission("brewery.bypass.teleport")) {
					player.teleport(randomLoc);
					P.p.msg(player, P.p.languageReader.get("Player_Wake"));
				}
			}
			offlineDrunk = 0;
			syncToSQL(false);
		}
		offlineDrunk = 0;
	}

	public void disconnecting() {
		offlineDrunk = drunkeness;
		syncToSQL(false);
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
	// runs every 6 sec, average chance is 15%, so should puke about every 40 sec
	// good quality can decrease the chance by up to 15%
	public void drunkPuke(Player player) {
		if (drunkeness >= 90) {
			// chance between 20% and 10%
			if (Math.random() < 0.20f - (getQuality() / 100f)) {
				addPuke(player, 20 + (int) (Math.random() * 40));
			}
		} else if (drunkeness >= 80) {
			// chance between 15% and 0%
			if (Math.random() < 0.15f - (getQuality() / 66f)) {
				addPuke(player, 10 + (int) (Math.random() * 30));
			}
		} else if (drunkeness >= 70) {
			// chance between 10% at 1 quality and 0% at 6 quality
			if (Math.random() < 0.10f - (getQuality() / 60f)) {
				addPuke(player, 10 + (int) (Math.random() * 20));
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
		BUtil.reapplyPotionEffect(player, PotionEffectType.HUNGER.createEffect(80, 4), true);

		if (pTasks.isEmpty()) {
			taskId = P.p.getServer().getScheduler().scheduleSyncRepeatingTask(P.p, BPlayer::pukeTask, 1L, 1L);
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
				if (itemHandle == null) {
					itemHandle = Class.forName(P.p.getServer().getClass().getPackage().getName() + ".entity.CraftItem").getMethod("getHandle", (Class<?>[]) null);
				}
				Object entityItem = itemHandle.invoke(item, (Object[]) null);
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

	public static void applyEffects(List<PotionEffect> effects, Player player, PlayerEffectEvent.EffectType effectType) {
		PlayerEffectEvent event = new PlayerEffectEvent(player, effectType, effects);
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

		PlayerEffectEvent event = new PlayerEffectEvent(player, PlayerEffectEvent.EffectType.ALCOHOL, l);
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
		List<PotionEffect> list = getQualityEffects(quality, brewAlc);
		PlayerEffectEvent event = new PlayerEffectEvent(player, PlayerEffectEvent.EffectType.QUALITY, list);
		P.p.getServer().getPluginManager().callEvent(event);
		list = event.getEffects();
		if (event.isCancelled() || list == null) {
			return;
		}
		for (PotionEffect effect : list) {
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
		List<BEffect> effects = brew.getEffects();
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

		List<PotionEffect> list = new ArrayList<>(2);
		list.add(PotionEffectType.SLOW.createEffect(duration, amplifier));
		list.add(PotionEffectType.HUNGER.createEffect(duration, amplifier));

		PlayerEffectEvent event = new PlayerEffectEvent(player, PlayerEffectEvent.EffectType.HANGOVER, list);
		P.p.getServer().getPluginManager().callEvent(event);
		list = event.getEffects();
		if (event.isCancelled() || list == null) {
			return;
		}
		for (PotionEffect effect : list) {
			BUtil.reapplyPotionEffect(player, effect, true);
		}
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
				String uuid = entry.getKey();
				BPlayer bplayer = entry.getValue();
				if (bplayer.drunkeness == soberPerMin) {
					// Prevent 0 drunkeness
					soberPerMin++;
				}
				if (bplayer.drain(BUtil.getPlayerfromString(uuid), soberPerMin)) {
					iter.remove();
					if (BConfig.sqlDrunkSync && BConfig.sqlSync != null) {
						BConfig.sqlSync.removePlayer(UUID.fromString(uuid));
					}
				}
			}
		}
	}

	// Sync Drunkeness Data to SQL if enabled
	public void syncToSQL(boolean playerOffline) {
		if (BConfig.sqlDrunkSync && BConfig.sqlSync != null) {
			BConfig.sqlSync.updatePlayer(UUID.fromString(uuid), this, playerOffline);
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


	public String getUuid() {
		return uuid;
	}

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
		syncToSQL(false);
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

	public int getQualityData() {
		return quality;
	}

	// opposite of quality
	public int getHangoverQuality() {
		if (drunkeness < 0) {
			return quality + 11;
		}
		return -getQuality() + 11;
	}

	/**
	 * Drunkeness at the time he went offline
	 */
	public int getOfflineDrunkeness() {
		return offlineDrunk;
	}

}
