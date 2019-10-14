package com.dre.brewery.listeners;

import com.dre.brewery.*;
import com.dre.brewery.api.events.brew.BrewModifyEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Locale;

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

			if (sender.hasPermission("brewery.cmd.reload")) {
				p.reload(sender);
				p.msg(sender, p.languageReader.get("CMD_Reload"));
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("configname") || cmd.equalsIgnoreCase("itemname") || cmd.equalsIgnoreCase("iteminfo")) {

			if (sender.hasPermission("brewery.cmd.reload")) {
				cmdItemName(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("wakeup")) {

			if (sender.hasPermission("brewery.cmd.wakeup")) {
				cmdWakeup(sender, args);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("create")) {

			if (sender.hasPermission("brewery.cmd.create")) {
				cmdCreate(sender, args);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("info")) {

			if (args.length > 1) {
				if (sender.hasPermission("brewery.cmd.infoOther")) {
					cmdInfo(sender, args[1]);
				} else {
					p.msg(sender, p.languageReader.get("Error_NoPermissions"));
				}
			} else {
				if (sender.hasPermission("brewery.cmd.info")) {
					cmdInfo(sender, null);
				} else {
					p.msg(sender, p.languageReader.get("Error_NoPermissions"));
				}
			}

		} else if (cmd.equalsIgnoreCase("copy") || cmd.equalsIgnoreCase("cp")) {

			if (sender.hasPermission("brewery.cmd.copy")) {
				if (args.length > 1) {
					cmdCopy(sender, p.parseInt(args[1]));
				} else {
					cmdCopy(sender, 1);
				}
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("delete") || cmd.equalsIgnoreCase("rm") || cmd.equalsIgnoreCase("remove")) {

			if (sender.hasPermission("brewery.cmd.delete")) {
				cmdDelete(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("persist") || cmd.equalsIgnoreCase("persistent")) {

			if (sender.hasPermission("brewery.cmd.persist")) {
				cmdPersist(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("static")) {

			if (sender.hasPermission("brewery.cmd.static")) {
				cmdStatic(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else if (cmd.equalsIgnoreCase("unlabel")) {

			if (sender.hasPermission("brewery.cmd.unlabel")) {
				cmdUnlabel(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			}

		} else {

			if (p.getServer().getPlayerExact(cmd) != null || BPlayer.hasPlayerbyName(cmd)) {

				if (args.length == 1) {
					if (sender.hasPermission("brewery.cmd.infoOther")) {
						cmdInfo(sender, cmd);
					}
				} else {
					if (sender.hasPermission("brewery.cmd.player")) {
						cmdPlayer(sender, args);
					} else {
						p.msg(sender, p.languageReader.get("Error_NoPermissions"));
					}
				}

			} else {

				p.msg(sender, p.languageReader.get("Error_UnknownCommand"));
				p.msg(sender, p.languageReader.get("Error_ShowHelp"));

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

		BUtil.list(sender, commands, page);

	}

	public ArrayList<String> getCommands(CommandSender sender) {

		ArrayList<String> cmds = new ArrayList<>();
		cmds.add(p.languageReader.get("Help_Help"));

		if (sender.hasPermission("brewery.cmd.player")) {
			cmds.add (p.languageReader.get("Help_Player"));
		}

		if (sender.hasPermission("brewery.cmd.info")) {
			cmds.add (p.languageReader.get("Help_Info"));
		}

		if (sender.hasPermission("brewery.cmd.unlabel")) {
			cmds.add (p.languageReader.get("Help_UnLabel"));
		}

		if (sender.hasPermission("brewery.cmd.copy")) {
			cmds.add (p.languageReader.get("Help_Copy"));
		}

		if (sender.hasPermission("brewery.cmd.delete")) {
			cmds.add (p.languageReader.get("Help_Delete"));
		}

		if (sender.hasPermission("brewery.cmd.infoOther")) {
			cmds.add (p.languageReader.get("Help_InfoOther"));
		}

		if (sender.hasPermission("brewery.cmd.wakeup")) {
			cmds.add(p.languageReader.get("Help_Wakeup"));
			cmds.add(p.languageReader.get("Help_WakeupList"));
			cmds.add(p.languageReader.get("Help_WakeupCheck"));
			cmds.add(p.languageReader.get("Help_WakeupCheckSpecific"));
			cmds.add(p.languageReader.get("Help_WakeupAdd"));
			cmds.add(p.languageReader.get("Help_WakeupRemove"));
		}

		if (sender.hasPermission("brewery.cmd.reload")) {
			cmds.add(p.languageReader.get("Help_Configname"));
			cmds.add(p.languageReader.get("Help_Reload"));
		}

		if (sender.hasPermission("brewery.cmd.persist")) {
			cmds.add(p.languageReader.get("Help_Persist"));
		}

		if (sender.hasPermission("brewery.cmd.static")) {
			cmds.add(p.languageReader.get("Help_Static"));
		}

		if (sender.hasPermission("brewery.cmd.create")) {
			cmds.add(p.languageReader.get("Help_Create"));
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
				p.msg(sender, p.languageReader.get("Etc_Usage"));
				p.msg(sender, p.languageReader.get("Help_WakeupRemove"));
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

			p.msg(sender, p.languageReader.get("Error_UnknownCommand"));
			p.msg(sender, p.languageReader.get("Error_ShowHelp"));

		}
	}

	public void cmdPlayer(CommandSender sender, String[] args) {

		int drunkeness = p.parseInt(args[1]);
		if (drunkeness < 0) {
			return;
		}
		int quality = -1;
		if (args.length > 2) {
			quality = p.parseInt(args[2]);
			if (quality < 1 || quality > 10) {
				p.msg(sender, p.languageReader.get("CMD_Player_Error"));
				return;
			}
		}

		String playerName = args[0];
		Player player = P.p.getServer().getPlayerExact(playerName);
		BPlayer bPlayer;
		if (player == null) {
			bPlayer = BPlayer.getByName(playerName);
		} else {
			bPlayer = BPlayer.get(player);
		}
		if (bPlayer == null && player != null) {
			if (drunkeness == 0) {
				return;
			}
			bPlayer = BPlayer.addPlayer(player);
		}
		if (bPlayer == null) {
			return;
		}

		if (drunkeness == 0) {
			bPlayer.remove();
		} else {
			bPlayer.setData(drunkeness, quality);
		}

		if (drunkeness > 100) {
			if (player != null) {
				bPlayer.drinkCap(player);
			} else {
				if (!BPlayer.overdrinkKick) {
					bPlayer.setData(100, 0);
				}
			}
		}
		p.msg(sender, p.languageReader.get("CMD_Player", playerName, "" + drunkeness, "" + bPlayer.getQuality()));

	}

	public void cmdInfo(CommandSender sender, String playerName) {

		if (playerName == null) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				playerName = player.getName();
			} else {
				p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
				return;
			}
		}

		Player player = P.p.getServer().getPlayerExact(playerName);
		BPlayer bPlayer;
		if (player == null) {
			bPlayer = BPlayer.getByName(playerName);
		} else {
			bPlayer = BPlayer.get(player);
		}
		if (bPlayer == null) {
			p.msg(sender, p.languageReader.get("CMD_Info_NotDrunk", playerName));
		} else {
			p.msg(sender, p.languageReader.get("CMD_Info_Drunk", playerName, "" + bPlayer.getDrunkeness(), "" + bPlayer.getQuality()));
		}

	}

	public void cmdItemName(CommandSender sender) {
		if (sender instanceof Player) {

			Player player = (Player) sender;
			ItemStack hand = P.use1_9 ? player.getInventory().getItemInMainHand() : player.getItemInHand();
			if (hand != null) {
				p.msg(sender, p.languageReader.get("CMD_Configname", hand.getType().name().toLowerCase(Locale.ENGLISH)));
			} else {
				p.msg(sender, p.languageReader.get("CMD_Configname_Error"));
			}

		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}
	}

	@Deprecated
	@SuppressWarnings("deprecation")
	public void cmdCopy(CommandSender sender, int count) {

		if (sender instanceof Player) {
			if (count < 1 || count > 36) {
				p.msg(sender, p.languageReader.get("Etc_Usage"));
				p.msg(sender, p.languageReader.get("Help_Copy"));
				return;
			}
			Player player = (Player) sender;
			ItemStack hand = player.getItemInHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					while (count > 0) {
						ItemStack item = hand.clone();
						if (!(player.getInventory().addItem(item)).isEmpty()) {
							p.msg(sender, p.languageReader.get("CMD_Copy_Error", "" + count));
							return;
						}
						count--;
					}
					if (brew.isPersistent()) {
						p.msg(sender, p.languageReader.get("CMD_CopyNotPersistent"));
					}
					return;
				}
			}

			p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));

		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}

	}

	@Deprecated
	@SuppressWarnings("deprecation")
	public void cmdDelete(CommandSender sender) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			ItemStack hand = player.getItemInHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					if (brew.isPersistent()) {
						p.msg(sender, p.languageReader.get("CMD_PersistRemove"));
					} else {
						//brew.remove(hand);
						player.setItemInHand(new ItemStack(Material.AIR));
					}
					return;
				}
			}
			p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));
		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}

	}

	@Deprecated
	@SuppressWarnings("deprecation")
	public void cmdPersist(CommandSender sender) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			ItemStack hand = player.getItemInHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					if (brew.isPersistent()) {
						brew.removePersistence();
						brew.setStatic(false, hand);
						p.msg(sender, p.languageReader.get("CMD_UnPersist"));
					} else {
						brew.makePersistent();
						brew.setStatic(true, hand);
						p.msg(sender, p.languageReader.get("CMD_Persistent"));
					}
					brew.touch();
					brew.save(hand);
					return;
				}
			}
			p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));
		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}

	}

	@SuppressWarnings("deprecation")
	public void cmdStatic(CommandSender sender) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			ItemStack hand = player.getItemInHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					if (brew.isStatic()) {
						if (!brew.isPersistent()) {
							brew.setStatic(false, hand);
							p.msg(sender, p.languageReader.get("CMD_NonStatic"));
						} else {
							p.msg(sender, p.languageReader.get("Error_PersistStatic"));
						}
					} else {
						brew.setStatic(true, hand);
						p.msg(sender, p.languageReader.get("CMD_Static"));
					}
					brew.touch();
					ItemMeta meta = hand.getItemMeta();
					BrewModifyEvent modifyEvent = new BrewModifyEvent(brew, meta, BrewModifyEvent.Type.STATIC);
					P.p.getServer().getPluginManager().callEvent(modifyEvent);
					if (modifyEvent.isCancelled()) {
						return;
					}
					brew.save(meta);
					hand.setItemMeta(meta);
					return;
				}
			}
			p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));
		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}

	}

	@SuppressWarnings("deprecation")
	public void cmdUnlabel(CommandSender sender) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			ItemStack hand = player.getItemInHand();
			if (hand != null) {
				Brew brew = Brew.get(hand);
				if (brew != null) {
					ItemMeta origMeta = hand.getItemMeta();
					brew.unLabel(hand);
					brew.touch();
					ItemMeta meta = hand.getItemMeta();
					BrewModifyEvent modifyEvent = new BrewModifyEvent(brew, meta, BrewModifyEvent.Type.UNLABEL);
					P.p.getServer().getPluginManager().callEvent(modifyEvent);
					if (modifyEvent.isCancelled()) {
						hand.setItemMeta(origMeta);
						return;
					}
					brew.save(meta);
					hand.setItemMeta(meta);
					p.msg(sender, p.languageReader.get("CMD_UnLabel"));
					return;
				}
			}
			p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));
		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}

	}

	public void cmdCreate(CommandSender sender, String[] args) {

		if (args.length < 2) {
			p.msg(sender, p.languageReader.get("Etc_Usage"));
			p.msg(sender, p.languageReader.get("Help_Create"));
			return;
		}

		int quality = 10;
		boolean hasQuality = false;
		String pName = null;
		if (args.length > 2) {
			quality = p.parseInt(args[args.length - 1]);

			if (quality <= 0 || quality > 10) {
				pName = args[args.length - 1];
				if (args.length > 3) {
					quality = p.parseInt(args[args.length - 2]);
				}
			}
			if (quality > 0 && quality <= 10) {
				hasQuality = true;
			} else {
				quality = 10;
			}
		}
		Player player = null;
		if (pName != null) {
			player = p.getServer().getPlayer(pName);
		}

		if (sender instanceof Player || player != null) {
			if (player == null) {
				player = ((Player) sender);
			}
			int stringLength = args.length - 1;
			if (pName != null) {
				stringLength--;
			}
			if (hasQuality) {
				stringLength--;
			}

			String name;
			if (stringLength > 1) {
				StringBuilder builder = new StringBuilder(args[1]);

				for (int i = 2; i < stringLength + 1; i++) {
					builder.append(" ").append(args[i]);
				}
				name = builder.toString();
			} else {
				name = args[1];
			}

			if (player.getInventory().firstEmpty() == -1) {
				p.msg(sender, p.languageReader.get("CMD_Copy_Error", "1"));
				return;
			}

			BRecipe recipe = null;
			for (BRecipe r : BIngredients.recipes) {
				if (r.hasName(name)) {
					recipe = r;
					break;
				}
			}
			if (recipe != null) {
				ItemStack item = recipe.create(quality);
				if (item != null) {
					player.getInventory().addItem(item);
				}
			} else {
				p.msg(sender, p.languageReader.get("Error_NoBrewName", name));
			}

		} else {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
		}
	}

}
