package com.dre.brewery;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.bukkit.configuration.ConfigurationSection;

import com.dre.brewery.Brew;

public class BPlayer {
	public static Map<String, BPlayer> players = new HashMap<String, BPlayer>();// Players
																				// name
																				// and
																				// BPlayer

	private int quality = 0;// = quality of drunkeness * drunkeness
	private int drunkeness = 0;// = amount of drunkeness
	private Vector push = new Vector(0, 0, 0);
	private int time = 20;

	public BPlayer() {
	}

	// reading from file
	public BPlayer(String name, int quality, int drunkeness) {
		this.quality = quality;
		this.drunkeness = drunkeness;
		players.put(name, this);
	}

	public static BPlayer get(String name) {
		if (!players.isEmpty()) {
			if (players.containsKey(name)) {
				return players.get(name);
			}
		}
		return null;
	}

	// returns true if drinking was successful
	public static boolean drink(int uid, String name) {
		Brew brew = Brew.get(uid);
		if (brew != null) {
			BPlayer bPlayer = get(name);
			if (bPlayer == null) {
				bPlayer = new BPlayer();
				players.put(name, bPlayer);
			}
			bPlayer.drunkeness += brew.getAlcohol();
			bPlayer.quality += brew.getQuality() * brew.getAlcohol();
			P.p.log(name + " ist nun " + bPlayer.drunkeness + "% betrunken, mit einer Qualität von " + bPlayer.getQuality());
			return true;
		}
		return false;
	}

	// push the player around if he moves
	public static void playerMove(PlayerMoveEvent event) {
		BPlayer bPlayer = get(event.getPlayer().getName());
		if (bPlayer != null) {
			bPlayer.move(event);
		}
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

	// decreasing drunkeness over time
	public static void onUpdate() {
		if (!players.isEmpty()) {
			for (BPlayer bplayer : players.values()) {
				bplayer.drunkeness -= 2;
				if (bplayer.drunkeness <= 0) {
					players.remove(bplayer);
				}
			}
		}
	}

	// save all data
	public static void save(ConfigurationSection config) {
		if (!players.isEmpty()) {
			for (String name : players.keySet()) {
				ConfigurationSection section = config.createSection(name);
				section.set("quality", players.get(name).quality);
				section.set("drunk", players.get(name).drunkeness);
			}
		}
	}

	// getter
	public int getDrunkeness() {
		return drunkeness;
	}

	public int getQuality() {
		return Math.round(quality / drunkeness);
	}

}