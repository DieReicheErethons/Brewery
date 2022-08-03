package com.dre.brewery.listeners;

import com.dre.brewery.*;
import com.dre.brewery.api.events.brew.BrewModifyEvent;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.Ingredient;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.PermissionUtil;
import com.dre.brewery.utility.Tuple;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;

import static com.dre.brewery.utility.PermissionUtil.BPermission.*;

public class CommandListener implements CommandExecutor {

	public P p = P.p;

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

		String cmd = "help";
		if (args.length > 0) {
			cmd = args[0];
		}

		if (cmd.equalsIgnoreCase("help")) {

			cmdHelp(sender, args);

		} else if (cmd.equalsIgnoreCase("reload")) {

			if (sender.hasPermission("brewery.cmd.reload")) {
				p.reload(sender);
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

		} else if (cmd.equalsIgnoreCase("create") || cmd.equalsIgnoreCase("give")) {

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

		} else if (cmd.equalsIgnoreCase("seal") || cmd.startsWith("seal") || cmd.startsWith("Seal")) {

			if (sender.hasPermission("brewery.cmd.seal")) {
				cmdSeal(sender);
			} else {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
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

		} else if (cmd.equalsIgnoreCase("debuginfo")) {

			debugInfo(sender, args.length > 1 ? args[1] : null);

		} else if (cmd.equalsIgnoreCase("showstats")) {

			showStats(sender);

		} else if (cmd.equalsIgnoreCase("puke") || cmd.equalsIgnoreCase("vomit") || cmd.equalsIgnoreCase("barf")) {

			cmdPuke(sender, args);

		} else if (cmd.equalsIgnoreCase("drink")) {

			cmdDrink(sender, args);

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
		PermissionUtil.evaluateExtendedPermissions(sender);

		if (PLAYER.checkCached(sender)) {
			cmds.add (p.languageReader.get("Help_Player"));
		}

		if (INFO.checkCached(sender)) {
			cmds.add (p.languageReader.get("Help_Info"));
		}

		if (P.use1_13 && SEAL.checkCached(sender)) {
			cmds.add (p.languageReader.get("Help_Seal"));
		}

		if (UNLABEL.checkCached(sender)) {
			cmds.add (p.languageReader.get("Help_UnLabel"));
		}

		if (PermissionUtil.noExtendedPermissions(sender)) {
			return cmds;
		}

		if (INFO_OTHER.checkCached(sender)) {
			cmds.add (p.languageReader.get("Help_InfoOther"));
		}

		if (CREATE.checkCached(sender)) {
			cmds.add(p.languageReader.get("Help_Create"));
			cmds.add(p.languageReader.get("Help_Give"));
		}

		if (DRINK.checkCached(sender) || DRINK_OTHER.checkCached(sender)) {
			cmds.add(p.languageReader.get("Help_Drink"));
		}

		if (RELOAD.checkCached(sender)) {
			cmds.add(p.languageReader.get("Help_Configname"));
			cmds.add(p.languageReader.get("Help_Reload"));
		}

		if (PUKE.checkCached(sender) || PUKE_OTHER.checkCached(sender)) {
			cmds.add(p.languageReader.get("Help_Puke"));
		}

		if (WAKEUP.checkCached(sender)) {
			cmds.add(p.languageReader.get("Help_Wakeup"));
			cmds.add(p.languageReader.get("Help_WakeupList"));
			cmds.add(p.languageReader.get("Help_WakeupCheck"));
			cmds.add(p.languageReader.get("Help_WakeupCheckSpecific"));
			cmds.add(p.languageReader.get("Help_WakeupAdd"));
			cmds.add(p.languageReader.get("Help_WakeupRemove"));
		}

		if (STATIC.checkCached(sender)) {
			cmds.add(p.languageReader.get("Help_Static"));
		}

		if (COPY.checkCached(sender)) {
			cmds.add (p.languageReader.get("Help_Copy"));
		}

		if (DELETE.checkCached(sender)) {
			cmds.add (p.languageReader.get("Help_Delete"));
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
			if (BConfig.showStatusOnDrink) {
				bPlayer.showDrunkeness(player);
			}
		}

		if (drunkeness > 100) {
			if (player != null) {
				bPlayer.drinkCap(player);
			} else {
				if (!BConfig.overdrinkKick) {
					bPlayer.setData(100, 0);
				}
			}
		}
		p.msg(sender, p.languageReader.get("CMD_Player", playerName, "" + drunkeness, "" + bPlayer.getQuality()));

	}

	public void cmdInfo(CommandSender sender, String playerName) {

		boolean selfInfo = playerName == null;
		if (selfInfo) {
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
			if (selfInfo) {
				bPlayer.showDrunkeness(player);
			} else {
				p.msg(sender, p.languageReader.get("CMD_Info_Drunk", playerName, "" + bPlayer.getDrunkeness(), "" + bPlayer.getQuality()));
			}
		}

	}

	public void cmdItemName(CommandSender sender) {
		if (!(sender instanceof Player)) {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
			return;
		}

		Player player = (Player) sender;
		@SuppressWarnings("deprecation")
		ItemStack hand = P.use1_9 ? player.getInventory().getItemInMainHand() : player.getItemInHand();
		if (hand != null) {
			p.msg(sender, p.languageReader.get("CMD_Configname", hand.getType().name().toLowerCase(Locale.ENGLISH)));
		} else {
			p.msg(sender, p.languageReader.get("CMD_Configname_Error"));
		}

	}

	public void cmdSeal(CommandSender sender) {
		if (!P.use1_13) {
			P.p.msg(sender, "Sealing requires minecraft 1.13 or higher");
			return;
		}
		if (!(sender instanceof Player)) {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
			return;
		}

		Player player = (Player) sender;
		player.openInventory(new BSealer(player).getInventory());
	}

	@Deprecated
	public void cmdCopy(CommandSender sender, int count) {

		if (!(sender instanceof Player)) {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
			return;
		}
		if (count < 1 || count > 36) {
			p.msg(sender, p.languageReader.get("Etc_Usage"));
			p.msg(sender, p.languageReader.get("Help_Copy"));
			return;
		}
		Player player = (Player) sender;
		ItemStack hand = player.getItemInHand();
		if (hand != null) {
			if (Brew.isBrew(hand)) {
				while (count > 0) {
					ItemStack item = hand.clone();
					if (!(player.getInventory().addItem(item)).isEmpty()) {
						p.msg(sender, p.languageReader.get("CMD_Copy_Error", "" + count));
						return;
					}
					count--;
				}
				return;
			}
		}

		p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));

	}

	@Deprecated
	public void cmdDelete(CommandSender sender) {

		if (!(sender instanceof Player)) {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
			return;
		}
		Player player = (Player) sender;
		ItemStack hand = player.getItemInHand();
		if (hand != null) {
			if (Brew.isBrew(hand)) {
				player.setItemInHand(new ItemStack(Material.AIR));
				return;
			}
		}
		p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));

	}

	public void debugInfo(CommandSender sender, String recipeName) {
		if (!P.use1_9 || !sender.isOp()) return;
		if (!(sender instanceof Player)) {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
			return;
		}
		Player player = (Player) sender;
		ItemStack hand = player.getInventory().getItemInMainHand();
		if (hand != null) {
			Brew brew = Brew.get(hand);
			if (brew == null) return;
			P.p.log(brew.toString());
			BIngredients ingredients = brew.getIngredients();
			if (recipeName == null) {
				P.p.log("&lIngredients:");
				for (Ingredient ing : ingredients.getIngredientList()) {
					P.p.log(ing.toString());
				}
				P.p.log("&lTesting Recipes");
				for (BRecipe recipe : BRecipe.getAllRecipes()) {
					int ingQ = ingredients.getIngredientQuality(recipe);
					int cookQ = ingredients.getCookingQuality(recipe, false);
					int cookDistQ = ingredients.getCookingQuality(recipe, true);
					int ageQ = ingredients.getAgeQuality(recipe, brew.getAgeTime());
					P.p.log(recipe.getRecipeName() + ": ingQlty: " + ingQ + ", cookQlty:" + cookQ + ", cook+DistQlty: " + cookDistQ + ", ageQlty: " + ageQ);
				}
				BRecipe distill = ingredients.getBestRecipe(brew.getWood(), brew.getAgeTime(), true, brew.getLiquidType());
				BRecipe nonDistill = ingredients.getBestRecipe(brew.getWood(), brew.getAgeTime(), false, brew.getLiquidType());
				P.p.log("&lWould prefer Recipe: " + (nonDistill == null ? "none" : nonDistill.getRecipeName()) + " and Distill-Recipe: " + (distill == null ? "none" : distill.getRecipeName()));
			} else {
				BRecipe recipe = BRecipe.getMatching(recipeName);
				if (recipe == null) {
					P.p.msg(player, "Could not find Recipe " + recipeName);
					return;
				}
				P.p.log("&lIngredients in Recipe " + recipe.getRecipeName() + ":");
				for (RecipeItem ri : recipe.getIngredients()) {
					P.p.log(ri.toString());
				}
				P.p.log("&lIngredients in Brew:");
				for (Ingredient ingredient : ingredients.getIngredientList()) {
					int amountInRecipe = recipe.amountOf(ingredient);
					P.p.log(ingredient.toString() + ": " + amountInRecipe + " of this are in the Recipe");
				}
				int ingQ = ingredients.getIngredientQuality(recipe);
				int cookQ = ingredients.getCookingQuality(recipe, false);
				int cookDistQ = ingredients.getCookingQuality(recipe, true);
				int ageQ = ingredients.getAgeQuality(recipe, brew.getAgeTime());
				P.p.log("ingQlty: " + ingQ + ", cookQlty:" + cookQ + ", cook+DistQlty: " + cookDistQ  + ", ageQlty: " + ageQ);
			}

			P.p.msg(player, "Debug Info for item written into Log");
		}
	}

	public void showStats(CommandSender sender) {
		if (sender instanceof ConsoleCommandSender && !sender.isOp()) return;

		P.p.msg(sender, "Drunk Players: " + BPlayer.numDrunkPlayers());
		P.p.msg(sender, "Brews created: " + P.p.stats.brewsCreated);
		P.p.msg(sender, "Barrels built: " + Barrel.barrels.size());
		P.p.msg(sender, "Cauldrons boiling: " + BCauldron.bcauldrons.size());
		P.p.msg(sender, "Number of Recipes: " + BRecipe.getAllRecipes().size());
		P.p.msg(sender, "Wakeups: " + Wakeup.wakeups.size());
	}

	@SuppressWarnings("deprecation")
	public void cmdStatic(CommandSender sender) {

		if (!(sender instanceof Player)) {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
			return;
		}
		Player player = (Player) sender;
		ItemStack hand = player.getItemInHand();
		if (hand != null) {
			Brew brew = Brew.get(hand);
			if (brew != null) {
				if (brew.isStatic()) {
					if (!brew.isStripped()) {
						brew.setStatic(false, hand);
						p.msg(sender, p.languageReader.get("CMD_NonStatic"));
					} else {
						p.msg(sender, p.languageReader.get("Error_SealedAlwaysStatic"));
						return;
					}
				} else {
					brew.setStatic(true, hand);
					p.msg(sender, p.languageReader.get("CMD_Static"));
				}
				brew.touch();
				ItemMeta meta = hand.getItemMeta();
				assert meta != null;
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

	}

	@SuppressWarnings("deprecation")
	public void cmdUnlabel(CommandSender sender) {

		if (!(sender instanceof Player)) {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
			return;
		}
		Player player = (Player) sender;
		ItemStack hand = player.getItemInHand();
		if (hand != null) {
			Brew brew = Brew.get(hand);
			if (brew != null) {
				if (!brew.isUnlabeled()) {
					ItemMeta origMeta = hand.getItemMeta();
					brew.unLabel(hand);
					brew.touch();
					ItemMeta meta = hand.getItemMeta();
					assert meta != null;
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
				} else {
					p.msg(sender, p.languageReader.get("Error_AlreadyUnlabeled"));
					return;
				}
			}
		}
		p.msg(sender, p.languageReader.get("Error_ItemNotPotion"));

	}

	public void cmdCreate(CommandSender sender, String[] args) {
		if (args.length < 2) {
			p.msg(sender, p.languageReader.get("Etc_Usage"));
			p.msg(sender, p.languageReader.get("Help_Create"));
			return;
		}

		Tuple<Brew, Player> brewForPlayer = getFromCommand(sender, args);

		if (brewForPlayer != null) {
			if (brewForPlayer.b().getInventory().firstEmpty() == -1) {
				p.msg(sender, p.languageReader.get("CMD_Copy_Error", "1"));
				return;
			}

			ItemStack item = brewForPlayer.a().createItem(null);
			if (item != null) {
				brewForPlayer.b().getInventory().addItem(item);
				p.msg(sender, p.languageReader.get("CMD_Created"));
			}
		}

	}

	@Nullable
	public Tuple<Brew, Player> getFromCommand(CommandSender sender, String[] args) {
		if (args.length < 2) {
			return null;
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

		if (!(sender instanceof Player) && player == null) {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
			return null;
		}

		if (player == null) {
			player = ((Player) sender);
			pName = null;
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
		name = name.replaceAll("\"", "");

		BRecipe recipe = BRecipe.getMatching(name);
		if (recipe != null) {
			return new Tuple<>(recipe.createBrew(quality), player);
		} else {
			p.msg(sender, p.languageReader.get("Error_NoBrewName", name));
		}
		return null;
	}

	public void cmdPuke(CommandSender sender, String[] args) {
		if (!sender.hasPermission("brewery.cmd.puke")) {
			p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			return;
		}

		Player player = null;
		if (args.length > 1) {
			player = p.getServer().getPlayer(args[1]);
			if (player == null) {
				p.msg(sender, p.languageReader.get("Error_NoPlayer", args[1]));
				return;
			}
		}

		if (!(sender instanceof Player) && player == null) {
			p.msg(sender, p.languageReader.get("Error_PlayerCommand"));
			return;
		}
		if (player == null) {
			player = ((Player) sender);
		} else {
			if (!sender.hasPermission("brewery.cmd.pukeOther") && !player.equals(sender)) {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
				return;
			}
		}
		int count = 0;
		if (args.length > 2) {
			count = P.p.parseInt(args[2]);
		}
		if (count <= 0) {
			count = 20 + (int) (Math.random() * 40);
		}
		BPlayer.addPuke(player, count);
	}

	public void cmdDrink(CommandSender sender, String[] args) {
		if (!sender.hasPermission("brewery.cmd.drink") || !sender.hasPermission("brewery.cmd.drink")) {
			p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			return;
		}

		if (args.length < 2) {
			p.msg(sender, p.languageReader.get("Etc_Usage"));
			p.msg(sender, p.languageReader.get("Help_Drink"));
			return;
		}

		Tuple<Brew, Player> brewForPlayer = getFromCommand(sender, args);
		if (brewForPlayer != null) {
			Player player = brewForPlayer.b();
			if ((!sender.equals(player) && !sender.hasPermission("brewery.cmd.drinkOther")) ||
				(sender.equals(player) && !sender.hasPermission("brewery.cmd.drink"))) {
				p.msg(sender, p.languageReader.get("Error_NoPermissions"));
			} else {
				Brew brew = brewForPlayer.a();
				String brewName = brew.getCurrentRecipe().getName(brew.getQuality());
				BPlayer.drink(brew, null, player);

				p.msg(player, p.languageReader.get("CMD_Drink", brewName));
				if (!sender.equals(player)) {
					p.msg(sender, p.languageReader.get("CMD_DrinkOther", player.getDisplayName(), brewName));
				}
			}
		}
	}

}
