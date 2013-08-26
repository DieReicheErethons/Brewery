package com.dre.brewery;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.command.CommandSender;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Wakeup {

	public static ArrayList<Wakeup> wakeups = new ArrayList<Wakeup>();
	public static P p = P.p;
	public static int checkId = -1;
	public static Player checkPlayer = null;

	private Location loc;
	private boolean active = true;

	public Wakeup(Location loc) {
		this.loc = loc;
	}

	// get the nearest of two random Wakeup-Locations
	public static Location getRandom(Location playerLoc) {
		if (wakeups.isEmpty()) {
			return null;
		}

		ArrayList<Wakeup> worldWakes = new ArrayList<Wakeup>();

		for (Wakeup wakeup : wakeups) {
			if (wakeup.active) {
				if (wakeup.loc.getWorld().equals(playerLoc.getWorld())) {
					worldWakes.add(wakeup);
				}
			}
		}

		if (worldWakes.isEmpty()) {
			return null;
		}

		Wakeup w1 = calcRandom(worldWakes);
		worldWakes.remove(w1);

		while (!w1.check()) {
			p.errorLog("Please Check Wakeup-Location with id: &6" + wakeups.indexOf(w1));

			w1 = calcRandom(worldWakes);
			if (w1 == null) {
				return null;
			}
			worldWakes.remove(w1);
		}

		Wakeup w2 = calcRandom(worldWakes);
		if (w2 != null) {
			worldWakes.remove(w2);

			while (!w2.check()) {
				p.errorLog("Please Check Wakeup-Location with id: &6" + wakeups.indexOf(w2));

				w2 = calcRandom(worldWakes);
				if (w2 == null) {
					return w1.loc;
				}
				worldWakes.remove(w2);
			}


			if (w1.loc.distance(playerLoc) > w2.loc.distance(playerLoc)) {
				return w2.loc;
			}
		}
		return w1.loc;
	}

	public static Wakeup calcRandom(ArrayList<Wakeup> worldWakes) {
		if (worldWakes.isEmpty()) {
			return null;
		}
		return worldWakes.get((int) Math.round(Math.random() * ((float) worldWakes.size() - 1.0)));
	}

	public static void set(CommandSender sender) {
		if (sender instanceof Player) {

			Player player = (Player) sender;
			wakeups.add(new Wakeup(player.getLocation()));
			p.msg(sender, "&aAufwachpunkt mit id: &6" + (wakeups.size() - 1) + " &awurde erfolgreich erstellt!");

		} else {
			p.msg(sender, "&cDieser Befehl kann nur als Spieler ausgeführt werden");
		}
	}

	public static void remove(CommandSender sender, int id) {
		if (wakeups.isEmpty() || id < 0 || id >= wakeups.size()) {
			p.msg(sender, "&cDer Aufwachpunkt mit der id: &6" + id + " &cexistiert nicht!");
			return;
		}

		Wakeup wakeup = wakeups.get(id);

		if (wakeup.active) {
			wakeup.active = false;
			p.msg(sender, "&aDer Aufwachpunkt mit der id: &6" + id + " &awurde erfolgreich gelöscht!");

		} else {
			p.msg(sender, "&cDer Aufwachpunkt mit der id: &6" + id + " &cwurde bereits gelöscht!");
		}
	}

	public static void list(CommandSender sender, int page, String worldOnly) {
		if (wakeups.isEmpty()) {
			p.msg(sender, "&cEs wurden noch keine Aufwachpunkte erstellt!");
			return;
		}

		ArrayList<String> locs = new ArrayList<String>();
		for (int id = 0; id < wakeups.size(); id++) {

			Wakeup wakeup = wakeups.get(id);

			String s = "&m";
			if (wakeup.active) {
				s = "";
			}

			String world = wakeup.loc.getWorld().getName();

			if (worldOnly == null || world.equalsIgnoreCase(worldOnly)) {
				int x = (int) wakeup.loc.getX();
				int y = (int) wakeup.loc.getY();
				int z = (int) wakeup.loc.getZ();

				locs.add("&6" + s + id + "&f" + s + ": " + world + " " + x + "," + y + "," + z);
			}
		}
		p.list(sender, locs, page);
	}

	public static void check(CommandSender sender, int id, boolean all) {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (!all) {
				if (wakeups.isEmpty() || id >= wakeups.size()) {
					p.msg(sender, "&cDer Aufwachpunkt mit der id: &6" + id + "&c existiert nicht!");
					return;
				}

				Wakeup wakeup = wakeups.get(id);
				if (wakeup.check()) {
					player.teleport(wakeup.loc);
				} else {
					String world = wakeup.loc.getWorld().getName();
					int x = (int) wakeup.loc.getX();
					int y = (int) wakeup.loc.getY();
					int z = (int) wakeup.loc.getZ();
					p.msg(sender, "&cDer Aufwachpunkt mit der id: &6" + id + "&c An Position &6" + world + " " + x + "," + y + "," + z + "&c ist mit Blöcken gefüllt!");
				}

			} else {
				if (wakeups.isEmpty()) {
					p.msg(sender, "&cEs wurden noch keine Aufwachpunkte erstellt!");
					return;
				}
				if (checkPlayer != null && checkPlayer != player) {
					checkId = -1;
				}
				checkPlayer = player;
				tpNext();
			}


		} else {
			p.msg(sender, "&cDieser Befehl kann nur als Spieler ausgeführt werden");
		}
	}

	public boolean check() {
		return (!loc.getBlock().getType().isSolid() && !loc.getBlock().getRelative(0, 1, 0).getType().isSolid());
	}

	public static void tpNext() {
		checkId++;
		if (checkId >= wakeups.size()) {
			p.msg(checkPlayer, "&aDies war der letzte Aufwachpunkt");
			checkId = -1;
			checkPlayer = null;
			return;
		}

		Wakeup wakeup = wakeups.get(checkId);
		if (!wakeup.active) {
			tpNext();
			return;
		}

		String world = wakeup.loc.getWorld().getName();
		int x = (int) wakeup.loc.getX();
		int y = (int) wakeup.loc.getY();
		int z = (int) wakeup.loc.getZ();

		if (wakeup.check()) {
			p.msg(checkPlayer, "Teleport zu Aufwachpunkt mit der id: &6" + checkId + "&f An Position: &6" + world + " " + x + "," + y + "," + z);
			checkPlayer.teleport(wakeup.loc);
		} else {
			p.msg(checkPlayer, "&cDer Aufwachpunkt mit der id: &6" + checkId + "&c An Position &6" + world + " " + x + "," + y + "," + z + "&c ist mit Blöcken gefüllt!");
		}			
		p.msg(checkPlayer, "Zum nächsten Aufwachpunkt: Mit Faust in die Luft schlagen");
		p.msg(checkPlayer, "Zum Abbrechen: &9/br Wakeup Cancel");
	}

	public static void cancel(CommandSender sender) {
		if (checkPlayer != null) {
			checkPlayer = null;
			checkId = -1;
			p.msg(sender, "&6Aufwachpunkte-Check wurde abgebrochen");
			return;
		}
		p.msg(sender, "&cEs läuft kein Aufwachpunkte-Check");
	}


	public static void save(ConfigurationSection section, ConfigurationSection oldData) {
		p.createWorldSections(section);

		// loc is saved as a String in world sections with format x/y/z/pitch/yaw
		if (!wakeups.isEmpty()) {

			Iterator<Wakeup> iter = wakeups.iterator();
			for (int id = 0; iter.hasNext(); id++) {
				Wakeup wakeup = iter.next();

				if (!wakeup.active) {
					continue;
				}

				String worldName = wakeup.loc.getWorld().getName();
				String prefix = null;

				if (worldName.startsWith("DXL_")) {
					prefix = p.getDxlName(worldName) + "." + id;
				} else {
					prefix = wakeup.loc.getWorld().getUID().toString() + "." + id;
				}

				section.set(prefix, wakeup.loc.getX() + "/" + wakeup.loc.getY() + "/" + wakeup.loc.getZ() + "/" + wakeup.loc.getPitch() + "/" + wakeup.loc.getYaw());
			}
		}

		// copy Wakeups that are not loaded
		if (oldData != null){
			for (String uuid : oldData.getKeys(false)) {
				if (!section.contains(uuid)) {
					section.set(uuid, oldData.get(uuid));
				}
			}
		}
	}

}