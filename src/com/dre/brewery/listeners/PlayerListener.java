package com.dre.brewery.listeners;

import com.dre.brewery.*;
import com.dre.brewery.filedata.BConfig;
import com.dre.brewery.filedata.UpdateChecker;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.LegacyUtil;
import com.dre.brewery.utility.PermissionUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;


public class PlayerListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null) return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		Player player = event.getPlayer();
		Material type = clickedBlock.getType();

		// -- Clicking an Hopper --
		if (type == Material.HOPPER) {
			if (BConfig.brewHopperDump && event.getPlayer().isSneaking()) {
				if (!P.use1_9 || event.getHand() == EquipmentSlot.HAND) {
					ItemStack item = event.getItem();
					if (Brew.isBrew(item)) {
						event.setCancelled(true);
						BUtil.setItemInHand(event, Material.GLASS_BOTTLE, false);
						if (P.use1_11) {
							clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.ITEM_BOTTLE_EMPTY, 1f, 1f);
						}
					}
				}
			}
			return;
		}

		// -- Opening a Sealing Table --
		if (P.use1_14 && BSealer.isBSealer(clickedBlock)) {
			if (player.isSneaking()) {
				event.setUseInteractedBlock(Event.Result.DENY);
				return;
			}
			event.setCancelled(true);
			if (BConfig.enableSealingTable) {
				BSealer sealer = new BSealer(player);
				event.getPlayer().openInventory(sealer.getInventory());
			} else {
				P.p.msg(player, P.p.languageReader.get("Error_SealingTableDisabled"));
			}
			return;
		}

		if (player.isSneaking()) return;

		// -- Interacting with a Cauldron --
		if (LegacyUtil.getCauldronType(type) != null) {
			// Handle the Cauldron Interact
			// The Event might get cancelled in here
			BCauldron.clickCauldron(event);
			return;
		}

		// -- Opening a Minecraft Barrel --
		if (P.use1_14 && type == Material.BARREL) {
			if (!player.hasPermission("brewery.openbarrel.mc")) {
				event.setCancelled(true);
				P.p.msg(player, P.p.languageReader.get("Error_NoPermissions"));
			}
			return;
		}

		// Do not process Off Hand for Barrel interaction
		if (P.use1_9 && event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		// -- Access a Barrel --
		Barrel barrel = null;
		if (LegacyUtil.isWoodPlanks(type)) {
			if (BConfig.openEverywhere) {
				barrel = Barrel.getByWood(clickedBlock);
			}
		} else if (LegacyUtil.isWoodStairs(type)) {
			barrel = Barrel.getByWood(clickedBlock);
			if (barrel != null) {
				if (!BConfig.openEverywhere && barrel.isLarge()) {
					barrel = null;
				}
			}
		} else if (LegacyUtil.isFence(type) || LegacyUtil.isSign(type)) {
			barrel = Barrel.getBySpigot(clickedBlock);
		}

		if (barrel != null) {
			event.setCancelled(true);

			if (!barrel.hasPermsOpen(player, event)) {
				return;
			}

			barrel.open(player);

			if (P.use1_14) {

				// When right clicking a normal Block in 1.14 with a potion or any edible item in hand,
				// even when cancelled, the consume animation will continue playing while opening the Barrel inventory.
				// The Animation and sound will play endlessly while the inventory is open, though no item is consumed.
				// This seems to be a client bug.
				// This workaround switches the currently selected slot to another for a short time, it needs to be a slot with a different item in it.
				// This seems to make the client stop animating a consumption
				// If there is a better way to do this please let me know
				Material hand = event.getMaterial();
				if ((hand == Material.POTION || hand.isEdible()) && !LegacyUtil.isSign(type)) {
					PlayerInventory inv = player.getInventory();
					final int held = inv.getHeldItemSlot();
					int useSlot = -1;
					for (int i = 0; i < 9; i++) {
						ItemStack item = inv.getItem(i);
						if (item == null || item.getType() == Material.AIR) {
							useSlot = i;
							break;
						} else if (useSlot == -1 && item.getType() != hand) {
							useSlot = i;
						}
					}
					if (useSlot != -1) {
						inv.setHeldItemSlot(useSlot);
						P.p.getServer().getScheduler().scheduleSyncDelayedTask(P.p, () -> player.getInventory().setHeldItemSlot(held), 2);
					}
				}

				barrel.playOpeningSound();
			}
		}
	}

	@EventHandler
	public void onClickAir(PlayerInteractEvent event) {
		if (Wakeup.checkPlayer == null) return;

		if (event.getAction() == Action.LEFT_CLICK_AIR) {
			if (!event.hasItem()) {
				if (event.getPlayer() == Wakeup.checkPlayer) {
					Wakeup.tpNext();
				}
			}
		}
	}

	// player drinks a custom potion
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (item != null) {
			if (item.getType() == Material.POTION) {
				Brew brew = Brew.get(item);
				if (brew != null) {
					if (!BPlayer.drink(brew, item.getItemMeta(), player)) {
						event.setCancelled(true);
						return;
					}
					/*if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
						brew.remove(item);
					}*/
					if (P.use1_9) {
						if (player.getGameMode() != GameMode.CREATIVE) {
							// replace the potion with an empty potion to avoid effects
							event.setItem(new ItemStack(Material.POTION));
						} else {
							// Dont replace the item when keeping the potion, just cancel the event
							event.setCancelled(true);
						}
					}
				}
			} else if (BConfig.drainItems.containsKey(item.getType())) {
				BPlayer bplayer = BPlayer.get(player);
				if (bplayer != null) {
					bplayer.drainByItem(player, item.getType());
					if (BConfig.showStatusOnDrink) {
						bplayer.showDrunkeness(player);
					}
				}
			}
		}
	}

	// Player has died! Decrease Drunkeness by 20
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		BPlayer bPlayer = BPlayer.get(event.getPlayer());
		if (bPlayer != null) {
			if (bPlayer.getDrunkeness() > 20) {
				bPlayer.setData(bPlayer.getDrunkeness() - 20, 0);
			} else {
				BPlayer.remove(event.getPlayer());
			}
		}
	}

	// player walks while drunk, push him around!
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (BPlayer.hasPlayer(event.getPlayer())) {
			BPlayer.playerMove(event);
		}
	}

	// player talks while drunk, but he cant speak very well
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		DistortChat.playerChat(event);
	}

	// player commands while drunk, distort chat commands
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
		DistortChat.playerCommand(event);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerLoginAsync(AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
			if (BConfig.sqlDrunkSync && BConfig.sqlSync != null) {
				BConfig.sqlSync.fetchPlayerLoginData(event.getUniqueId());
			}
		}
	}

	// player joins while passed out
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
			final Player player = event.getPlayer();
			BPlayer bplayer = BPlayer.get(player);
			if (bplayer != null) {
				if (player.hasPermission("brewery.bypass.logindeny")) {
					if (bplayer.getDrunkeness() > 100) {
						bplayer.setData(100, 0);
					}
					bplayer.join(player);
					return;
				}
				switch (bplayer.canJoin()) {
					case 0:
						bplayer.join(player);
						return;
					case 2:
						event.disallow(PlayerLoginEvent.Result.KICK_OTHER, P.p.languageReader.get("Player_LoginDeny"));
						return;
					case 3:
						event.disallow(PlayerLoginEvent.Result.KICK_OTHER, P.p.languageReader.get("Player_LoginDenyLong"));
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		UpdateChecker.notify(event.getPlayer());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
		PermissionUtil.logout(event.getPlayer());
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
		PermissionUtil.logout(event.getPlayer());
	}
}
