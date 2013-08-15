package com.dre.brewery.listeners;

import java.util.ArrayList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.dre.brewery.P;
import com.dre.brewery.Wakeup;
import com.dre.brewery.BPlayer;

public class CommandListener implements CommandExecutor {

	public P p = P.p;

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		String cmd = "help";
		if (args.length > 0) {
			cmd = args[0];
		}

		if (cmd.equalsIgnoreCase("help")) {

			cmdHelp(sender, args);

		} else if (cmd.equalsIgnoreCase("reload")) {

			if (p.permission.has(sender, "brewery.cmd.reload")) {
				p.reload();
				p.msg(sender, "&aConfig wurde neu eingelesen");
			} else {
				p.msg(sender, "&cDu hast keine Rechte dies zu tun!");
			}

		} else if (cmd.equalsIgnoreCase("wakeup")) {

			if (p.permission.has(sender, "brewery.cmd.wakeup")) {
				cmdWakeup(sender, args);
			} else {
				p.msg(sender, "&cDu hast keine Rechte dies zu tun!");
			}

		} else if (cmd.equalsIgnoreCase("create")) {

			//TODO: create command

		} else if (cmd.equalsIgnoreCase("info")) {

			if (args.length > 1) {
				if (p.permission.has(sender, "brewery.cmd.infoOther")) {
					cmdInfo(sender, args[1]);
				} else {
					p.msg(sender, "&cDu hast keine Rechte dies zu tun!");
				}
			} else {
				if (p.permission.has(sender, "brewery.cmd.info")) {
					cmdInfo(sender, null);
				} else {
					p.msg(sender, "&cDu hast keine Rechte dies zu tun!");
				}
			}

		} else {

			if (p.getServer().getPlayerExact(cmd) != null || BPlayer.players.containsKey(cmd)) {

				if (args.length == 1) {
					if (p.permission.has(sender, "brewery.cmd.infoOther")) {
						cmdInfo(sender, cmd);
					}
				} else {
					if (p.permission.has(sender, "brewery.cmd.player")) {
						cmdPlayer(sender, args);
					} else {
						p.msg(sender, "&cDu hast keine Rechte dies zu tun!");
					}
				}

			} else {

				p.msg(sender, "Unbekannter Befehl.");
				p.msg(sender, "benutze &6/br help &fum die Hilfe anzuzeigen");

			}
		}

		return true;
	}

	public void cmdHelp(CommandSender sender, String[] args) {

		int page = 1;
		if (args.length > 1) {
			page = p.parseInt(args[1]);
		}

		ArrayList<String> commands = getCommands(sender);

		if (page == 1) {
			p.msg(sender, "&6" + p.getDescription().getName() + " v" + p.getDescription().getVersion());	
		}

		p.list(sender, commands, page);

	}

	public ArrayList<String> getCommands(CommandSender sender) {

		ArrayList<String> cmds = new ArrayList<String>();
		cmds.add("&6/br help <Seite> &9Zeigt eine bestimmte Hilfeseite an");

		if (p.permission.has(sender, "brewery.cmd.reload")) {
			cmds.add("&6/br reload &9Config neuladen");
		}

		if (p.permission.has(sender, "brewery.cmd.wakeup")) {
			cmds.add("&6/br Wakeup List <Seite>&9 Listet alle Aufwachpunkte auf");
			cmds.add("&6/br Wakeup List <Seite> <Welt>&9 Listet die Aufwachpunkte einer Welt auf");
			cmds.add("&6/br Wakeup Check &9Teleportiert zu allen Aufwachpunkten");
			cmds.add("&6/br Wakeup Check <id> &9Teleportiert zu einem Aufwachpunkt");
			cmds.add("&6/br Wakeup Add &9Setzt einen Aufwachpunkt");
			cmds.add("&6/br Wakeup Remove <id> &9Entfernt einen Aufwachpunkt");
		}

		if (p.permission.has(sender, "brewery.cmd.player")) {
			cmds.add ("&6/br <Spieler> <%Trunkenheit> <Qualität>&9 Setzt Trunkenheit (und Qualität) eines Spielers");
		}

		if (p.permission.has(sender, "brewery.cmd.info")) {
			cmds.add ("&6/br Info&9 Zeigt deine aktuelle Trunkenheit und Qualität an");
		}

		if (p.permission.has(sender, "brewery.cmd.infoOther")) {
			cmds.add ("&6/br Info <Spieler>&9 Zeigt die aktuelle Trunkenheit und Qualität von <Spieler> an");
		}

		return cmds;
	}

	public void cmdWakeup(CommandSender sender, String[] args) {

		if (args.length == 1) {
			cmdHelp(sender, args);
			return;
		}

		if (args[1].equalsIgnoreCase("add")) {

			Wakeup.set(sender);

		} else if (args[1].equalsIgnoreCase("list")){

			int page = 1;
			String world = null;
			if (args.length > 2) {
				page = p.parseInt(args[2]);
			}
			if (args.length > 3) {
				world = args[3];
			}
			Wakeup.list(sender, page, world);

		} else if (args[1].equalsIgnoreCase("remove")){

			if (args.length > 2) {
				int id = p.parseInt(args[2]);
				Wakeup.remove(sender, id);
			} else {
				p.msg(sender, "Benutzung:");
				p.msg(sender, "&6/br Wakeup Remove <id>");
			}

		} else if (args[1].equalsIgnoreCase("check")){

			int id = -1;
			if (args.length > 2) {
				id = p.parseInt(args[2]);
				if (id < 0) {
					id = 0;
				}
			}
			Wakeup.check(sender, id, id == -1);

		} else if (args[1].equalsIgnoreCase("cancel")){

			Wakeup.cancel(sender);

		} else {

			p.msg(sender, "Unbekannter Befehl.");
			p.msg(sender, "benutze &6/br help &fum die Hilfe anzuzeigen");

		}
	}

	public void cmdPlayer(CommandSender sender, String[] args) {

		int drunkeness = p.parseInt(args[1]);
		int quality = -1;
		if (args.length > 2) {
			quality = p.parseInt(args[2]);
			if (quality < 1 || quality > 10) {
				p.msg(sender, "&cDie Qualität muss zwischen 1 und 10 liegen!");
				return;
			}
		}

		String playerName = args[0];
		BPlayer bPlayer = BPlayer.get(playerName);
		if (bPlayer == null) {
			if (drunkeness == 0) {
				return;
			}
			bPlayer = new BPlayer();
			BPlayer.players.put(playerName, bPlayer);
		}

		if (drunkeness == 0) {
			BPlayer.players.remove(playerName);
		} else {
			bPlayer.setData(drunkeness, quality);
		}

		p.msg(sender, "&a" + playerName + " ist nun &6" + drunkeness + "% &abetrunken, mit einer Qualität von &6" + bPlayer.getQuality());
		if (drunkeness > 100) {
			bPlayer.drinkCap(p.getServer().getPlayer(playerName));
		}

	}

	public void cmdInfo(CommandSender sender, String playerName) {

		if (playerName == null) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				playerName = player.getName();
			} else {
				p.msg(sender, "&cDieser Befehl kann nur als Spieler ausgeführt werden");
				return;
			}
		}

		BPlayer bPlayer = BPlayer.get(playerName);
		if (bPlayer == null) {
			p.msg(sender, playerName + " ist nicht betrunken");
		} else {
			p.msg(sender, playerName + " ist &6" + bPlayer.getDrunkeness() + "% &fbetrunken, mit einer Qualität von &6" + bPlayer.getQuality());
		}

	}

}