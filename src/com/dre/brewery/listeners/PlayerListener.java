package com.dre.brewery.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BIngredients;
import com.dre.brewery.Brew;
import com.dre.brewery.Barrel;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Words;
import com.dre.brewery.Wakeup;
import com.dre.brewery.P;
import com.dre.brewery.filedata.UpdateChecker;


public class PlayerListener implements Listener {
	public static boolean openEverywhere;

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();

		if (clickedBlock != null) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Player player = event.getPlayer();
				if (!player.isSneaking()) {
					Material type = clickedBlock.getType();

					// Interacting with a Cauldron
					if (type == Material.CAULDRON) {
						Material materialInHand = event.getMaterial();
						ItemStack item = event.getItem();

						if (materialInHand == Material.WATCH) {
							BCauldron.printTime(player, clickedBlock);
							return;

							// fill a glass bottle with potion
						} else if (materialInHand == Material.GLASS_BOTTLE) {
							if (player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
								if (BCauldron.fill(player, clickedBlock)) {
									event.setCancelled(true);
									if (player.hasPermission("brewery.cauldron.fill")) {
										if (item.getAmount() > 1) {
											item.setAmount(item.getAmount() - 1);
										} else {
											player.setItemInHand(new ItemStack(Material.AIR));
										}
									}
								}
							} else {
								event.setCancelled(true);
							}
							return;

							// reset cauldron when refilling to prevent
							// unlimited source of potions
						} else if (materialInHand == Material.WATER_BUCKET) {
							if (BCauldron.getFillLevel(clickedBlock) != 0 && BCauldron.getFillLevel(clickedBlock) < 2) {
								// will only remove when existing
								BCauldron.remove(clickedBlock);
							}
							return;

							// Its possible to empty a Cauldron with a Bucket in 1.9
						} else if (P.use1_9 && materialInHand == Material.BUCKET) {
							if (BCauldron.getFillLevel(clickedBlock) == 2) {
								// will only remove when existing
								BCauldron.remove(clickedBlock);
							}
							return;
						}

						// Check if fire alive below cauldron when adding ingredients
						Block down = clickedBlock.getRelative(BlockFace.DOWN);
						if (down.getType() == Material.FIRE || down.getType() == Material.STATIONARY_LAVA || down.getType() == Material.LAVA) {

							// add ingredient to cauldron that meet the previous conditions
							if (BIngredients.possibleIngredients.contains(materialInHand)) {

								if (player.hasPermission("brewery.cauldron.insert")) {
									if (BCauldron.ingredientAdd(clickedBlock, item)) {
										boolean isBucket = item.getType().equals(Material.WATER_BUCKET)
												|| item.getType().equals(Material.LAVA_BUCKET)
												|| item.getType().equals(Material.MILK_BUCKET);
										if (item.getAmount() > 1) {
											item.setAmount(item.getAmount() - 1);

											if (isBucket) {
												BCauldron.giveItem(player, new ItemStack(Material.BUCKET));
											}
										} else {
											if (isBucket) {
												player.setItemInHand(new ItemStack(Material.BUCKET));
											} else {
												player.setItemInHand(new ItemStack(Material.AIR));
											}
										}
									}
								} else {
									P.p.msg(player, P.p.languageReader.get("Perms_NoCauldronInsert"));
								}
								event.setCancelled(true);
							} else {
								event.setCancelled(true);
							}
						}
						return;
					}

					// Access a Barrel
					Barrel barrel = null;
					if (type == Material.WOOD) {
						if (openEverywhere) {
							barrel = Barrel.get(clickedBlock);
						}
					} else if (Barrel.isStairs(type)) {
						for (Barrel barrel2 : Barrel.barrels) {
							if (barrel2.hasStairsBlock(clickedBlock)) {
								if (openEverywhere || !barrel2.isLarge()) {
									barrel = barrel2;
								}
								break;
							}
						}
					} else if (Barrel.isFence(type) || type == Material.SIGN_POST || type == Material.WALL_SIGN) {
						barrel = Barrel.getBySpigot(clickedBlock);
					}

					if (barrel != null) {
						event.setCancelled(true);

						if (!barrel.hasPermsOpen(player, event)) {
							return;
						}

						barrel.open(player);
					}
				}
			}
		}

		if (event.getAction() == Action.LEFT_CLICK_AIR) {
			if (!event.hasItem()) {
				if (Wakeup.checkPlayer != null) {
					if (event.getPlayer() == Wakeup.checkPlayer) {
						Wakeup.tpNext();
					}
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
					BPlayer.drink(brew, player);
					if (player.getGameMode() != GameMode.CREATIVE) {
						brew.remove(item);
					}
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
			} else if (BPlayer.drainItems.containsKey(item.getType())) {
				BPlayer bplayer = BPlayer.get(player);
				if (bplayer != null) {
					bplayer.drainByItem(player, item.getType());
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
		Words.playerChat(event);
	}

	// player commands while drunk, distort chat commands
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
		Words.playerCommand(event);
	}

	// player joins while passed out
	@EventHandler()
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
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
	}
}
