package com.dre.brewery.listeners;

import java.util.ArrayList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.dre.brewery.P;
import com.dre.brewery.Wakeup;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Brew;

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
				p.msg(sender, P.p.languageReader.get("CMD_Reload"));
			} else {
				p.msg(sender, P.p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("wakeup")) {

			if (p.permission.has(sender, "brewery.cmd.wakeup")) {
				cmdWakeup(sender, args);
			} else {
				p.msg(sender, P.p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("create")) {

			//TODO: create command

		} else if (cmd.equalsIgnoreCase("info")) {

			if (args.length > 1) {
				if (p.permission.has(sender, "brewery.cmd.infoOther")) {
					cmdInfo(sender, args[1]);
				} else {
					p.msg(sender, P.p.languageReader.get("Error_NoPermissions"));
				}
			} else {
				if (p.permission.has(sender, "brewery.cmd.info")) {
					cmdInfo(sender, null);
				} else {
					p.msg(sender, P.p.languageReader.get("Error_NoPermissions"));
				}
			}

		} else if (cmd.equalsIgnoreCase("copy") || cmd.equalsIgnoreCase("cp")) {

			if (p.permission.has(sender, "brewery.cmd.copy")) {
				if (args.length > 1) {
					cmdCopy(sender, P.p.parseInt(args[1]));
				} else {
					cmdCopy(sender, 1);
				}
			} else {
				p.msg(sender, P.p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("delete") || cmd.equalsIgnoreCase("rm")) {

			if (p.permission.has(sender, "brewery.cmd.delete")) {
				cmdDelete(sender);
			} else {
				p.msg(sender, P.p.languageReader.get("Error_NoPermissions"));
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
						p.msg(sender, P.p.languageReader.get("Error_NoPermissions"));
					}
				}

			} else {

				p.msg(sender, P.p.languageReader.get("Error_UnknownCommand"));
				p.msg(sender, P.p.languageReader.get("Error_ShowHelp"));

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
		cmds.add(P.p.languageReader.get("Help_Help"));

		if (p.permission.has(sender, "brewery.cmd.player")) {
			cmds.add (P.p.languageReader.get("Help_Player"));
		}

		if (p.permission.has(sender, "brewery.cmd.info")) {
			cmds.add (P.p.languageReader.get("Help_Info"));
		}

		if (p.permission.has(sender, "brewery.cmd.copy")) {
			cmds.add (P.p.languageReader.get("Help_Copy"));
		}

		if (p.permission.has(sender, "brewery.cmd.delete")) {
			cmds.add (P.p.languageReader.get("Help_Delete"));
		}

		if (p.permission.has(sender, "brewery.cmd.infoOther")) {
			cmds.add (P.p.languageReader.get("Help_InfoOther"));
		}

		if (p.permission.has(sender, "brewery.cmd.wakeup")) {
			cmds.add(P.p.languageReader.get("Help_Wakeup"));
			cmds.add(P.p.languageReader.get("Help_WakeupList"));
			cmds.add(P.p.languageReader.get("Help_WakeupCheck"));
			cmds.add(P.p.languageReader.get("Help_WakeupCheckSpecific"));
			cmds.add(P.p.languageReader.get("Help_WakeupAdd"));
			cmds.add(P.p.languageReader.get("Help_WakeupRemove"));
		}

		if (p.permission.has(sender, "brewery.cmd.reload")) {
			cmds.add(P.p.languageReader.get("Help_Reload"));
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
				p.msg(sender, P.p.languageReader.get("Etc_Usage"));
				p.msg(sender, P.p.languageReader.get("Help_WakeupRemove"));
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

			p.msg(sender, P.p.languageReader.get("Error_UnknownCommand"));
			p.msg(sender, P.p.languageReader.get("Error_ShowHelp"));

		}
	}

	public void cmdPlayer(CommandSender sender, String[] args) {

		int drunkeness = p.parseInt(args[1]);
		int quality = -1;
		if (args.length > 2) {
			quality = p.parseInt(args[2]);
			if (quality < 1 || quality > 10) {
				p.msg(sender, P.p.languageReader.get("CMD_Player_Error"));
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

		p.msg(sender, P.p.languageReader.get("CMD_Player", playerName, "" + drunkeness, "" + bPlayer.getQuality()));
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
				p.msg(sender, P.p.languageReader.get("Error_PlayerCommand"));
				return;
			}
		}

		BPlayer bPlayer = BPlayer.get(playerName);
		if (bPlayer == null) {
			p.msg(sender, P.p.languageReader.get("CMD_Info_NotDrunk", playerName));
		} else {
			p.msg(sender, P.p.languageReader.get("CMD_Info_Drunk", playerName, "" + bPlayer.getDrunkeness(), "" + bPlayer.getQuality()));
		}

	}

	public void cmdCopy(CommandSender sender, int count) {

		if (sender instanceof Player) {
			if (count < 1 || count > 36) {
				p.msg(sender, P.p.languageReader.get("Etc_Usage"));
				p.msg(sender, P.p.languageReader.get("Help_Copy"));
				return;
			}
			Player player = (Player) sender;
			ItemStack hand = player.getItemInHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					while (count > 0) {
						ItemStack item = brew.copy(hand);
						if (!(player.getInventory().addItem(item)).isEmpty()) {
							p.msg(sender, P.p.languageReader.get("CMD_Copy_Error", count));
							return;
						}
						count--;
					}
					return;
				}
			}

			p.msg(sender, P.p.languageReader.get("Error_ItemNotPotion"));

		} else {
			p.msg(sender, P.p.languageReader.get("Error_PlayerCommand"));
		}

	}

	public void cmdDelete(CommandSender sender) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			ItemStack hand = player.getItemInHand();
			if (hand != null) {
				if (Brew.get(hand) != null) {
					Brew.remove(hand);
					player.setItemInHand(new ItemStack(0));
					return;
				}
			}
			p.msg(sender, P.p.languageReader.get("Error_ItemNotPotion"));
		} else {
			p.msg(sender, P.p.languageReader.get("Error_PlayerCommand"));
		}

	}

}