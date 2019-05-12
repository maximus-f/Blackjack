package me.perotin.blackjack.events;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.BlackjackGame;
import me.perotin.blackjack.objects.BlackjackPlayer;
import me.perotin.blackjack.util.ItemBuilder;
import me.perotin.blackjack.util.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/* Created by Perotin on 12/27/18 */
public class BlackjackInventoryClickEvent implements Listener {

    private Blackjack plugin;

    private int houseIndex = 1;

    public BlackjackInventoryClickEvent(Blackjack plugin) {
        this.plugin = plugin;
    }

    private HashMap<UUID, BukkitRunnable> inTutorial = new HashMap<>();


    private void updateInventoryForStay(final ArrayList<String> houseCards, Inventory clicked, Player clicker, int x) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack toUpdate = clicked.getItem(x);
                if (toUpdate != null && toUpdate.getType() != XMaterial.AIR.parseMaterial() && toUpdate.getType() == XMaterial.RED_STAINED_GLASS_PANE.parseMaterial()) {
                    // update it
                    ItemBuilder builder = new ItemBuilder(toUpdate);
                    builder.type(XMaterial.GREEN_STAINED_GLASS_PANE.parseMaterial());
                    builder.name(ChatColor.YELLOW + BlackjackGame.convertToFullText(houseCards.get(houseIndex)));
                    houseIndex++;
                    if (houseIndex >= houseCards.size() - 1) {
                        houseIndex = 1;
                    }
                    clicked.setItem(x, builder.build());

                    clicker.playSound(clicker.getLocation(), Sound.BLOCK_ANVIL_HIT, 5, 1);

                }

            }
        }.runTaskLater(plugin, 20);
    }


    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (inTutorial.keySet().contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            if (event.getMessage().equalsIgnoreCase("cancel")) {
                inTutorial.remove(event.getPlayer());

            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory clicked = event.getInventory();
        InventoryView view = event.getView();
        if (event.getWhoClicked() instanceof Player) {
            Player clicker = (Player) event.getWhoClicked();

            BlackjackPlayer player = plugin.getPlayerFor(clicker);

            BlackjackGame currentGame = null;
            for (BlackjackGame game : plugin.getCurrentGames()) {
                if (game.getPlayer().getUniqueId().equals(clicker.getUniqueId())) {
                    // same player
                    String name = plugin.getString("menu-title").replace("$number$", ""+game.getBetAmount());
                    if (event.getView().getTitle().equals(name)) {
                        // same inventory, its safe to say its a game click
                        currentGame = game;
                    }


                }
            }

            if (currentGame != null) {
                event.setCancelled(true);
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() != XMaterial.AIR.parseMaterial()) {
                    // its a block
                    if (item.getType() == XMaterial.PAPER.parseMaterial() && item.getItemMeta().getDisplayName().equals(plugin.getString("tutorial-name"))) {
                        // tutorial
                        clicker.closeInventory();
                        List<String> tutorialMessages = plugin.getConfig().getStringList("tutorial");
                        for (String s : tutorialMessages) {

                            clicker.sendMessage(ChatColor.translateAlternateColorCodes('&', s));

                        }


                    }
                    if (item.getType() == XMaterial.MAP.parseMaterial() && item.getItemMeta().getDisplayName().equals(plugin.getString("hit-item"))) {
                        // they hit
                        currentGame.getNextCard();
                        int score = currentGame.getScoreUnder21(currentGame.getPlayerCards());
                        if (score > 21) {
                            // they lose
                            currentGame.endGame(BlackjackGame.Ending.LOSE);
                            player.addLoss();
                            return;
                        }
                        clicker.openInventory(currentGame.getInventory(true));


                    } else if (item.getType() == XMaterial.BARRIER.parseMaterial() && item.getItemMeta().getDisplayName().equals(plugin.getString("stand-item"))) {
                        // they stand
                        currentGame.setPlayerTurn(false);
                        int houseScore = currentGame.getScoreUnder21(currentGame.getHouseCards());
                        if (houseScore >= 17) {
                            // stay
                            int playerScore = currentGame.getScoreUnder21(currentGame.getPlayerCards());

                            final ArrayList<String> houseCards = currentGame.getHouseCards();
                            for (int x = 12; x <= 17; x++) {
                                // revealing house cards
                                updateInventoryForStay(houseCards, clicked, clicker, x);

                            }
                            // end game logic
                            if (houseScore > 21) {
                                currentGame.endGame(BlackjackGame.Ending.WIN);
                                player.addWin();
                            } else if (houseScore > playerScore) {
                                // house wins
                                player.addLoss();
                                currentGame.endGame(BlackjackGame.Ending.LOSE);
                            } else if (playerScore > houseScore) {
                                // player wins
                                currentGame.endGame(BlackjackGame.Ending.WIN);
                                player.addWin();
                            } else if (playerScore == houseScore) {
                                // tie
                                currentGame.endGame(BlackjackGame.Ending.TIE);
                            }

                        } else {
                            currentGame.getNextCard();
                            currentGame.updateInventoryForHouse();
                            int playerScore = currentGame.getScoreUnder21(currentGame.getPlayerCards());
                            houseScore = currentGame.getScoreUnder21(currentGame.getHouseCards());


                            if (houseScore >= 17) {
                                final ArrayList<String> houseCards = currentGame.getHouseCards();
                                for (int x = 12; x < 17; x++) {

                                    updateInventoryForStay(houseCards, clicked, clicker, x);
                                }
                                if (houseScore > 21) {
                                    // house lose
                                    currentGame.endGame(BlackjackGame.Ending.WIN);
                                    player.addWin();
                                } else if (houseScore > playerScore) {
                                    // house wins
                                    currentGame.endGame(BlackjackGame.Ending.LOSE);
                                    player.addLoss();
                                } else if (playerScore > houseScore) {
                                    // player wins
                                    currentGame.endGame(BlackjackGame.Ending.WIN);
                                    player.addWin();
                                } else if (playerScore == houseScore) {
                                    // tie
                                    currentGame.endGame(BlackjackGame.Ending.TIE);
                                }

                            } else {
                                currentGame.getNextCard();
                                playerScore = currentGame.getScoreUnder21(currentGame.getPlayerCards());
                                houseScore = currentGame.getScoreUnder21(currentGame.getHouseCards());
                                currentGame.updateInventoryForHouse();

                                if (houseScore >= 17) {
                                    final ArrayList<String> houseCards = currentGame.getHouseCards();
                                    for (int x = 12; x < 17; x++) {

                                        updateInventoryForStay(houseCards, clicked, clicker, x);
                                    }
                                    if (houseScore > 21) {
                                        // house lose
                                        currentGame.endGame(BlackjackGame.Ending.WIN);
                                        player.addWin();
                                    } else if (houseScore > playerScore) {
                                        // house wins
                                        currentGame.endGame(BlackjackGame.Ending.LOSE);
                                        player.addLoss();
                                    } else if (playerScore > houseScore) {
                                        // player wins
                                        currentGame.endGame(BlackjackGame.Ending.WIN);
                                        player.addWin();
                                    } else if (playerScore == houseScore) {
                                        // tie
                                        currentGame.endGame(BlackjackGame.Ending.TIE);
                                    }
                                } else {
                                    currentGame.getNextCard();
                                    currentGame.updateInventoryForHouse();

                                    playerScore = currentGame.getScoreUnder21(currentGame.getPlayerCards());
                                    houseScore = currentGame.getScoreUnder21(currentGame.getHouseCards());

                                    if (houseScore >= 17) {
                                        final ArrayList<String> houseCards = currentGame.getHouseCards();
                                        for (int x = 12; x < 17; x++) {

                                            updateInventoryForStay(houseCards, clicked, clicker, x);
                                        }
                                        if (houseScore > 21) {
                                            // house lose
                                            currentGame.endGame(BlackjackGame.Ending.WIN);
                                            player.addWin();

                                        } else if (houseScore > playerScore) {
                                            // house wins
                                            currentGame.endGame(BlackjackGame.Ending.LOSE);
                                            player.addLoss();
                                        } else if (playerScore > houseScore) {
                                            // player wins
                                            currentGame.endGame(BlackjackGame.Ending.WIN);
                                            player.addWin();

                                        } else if (playerScore == houseScore) {
                                            // tie
                                            currentGame.endGame(BlackjackGame.Ending.TIE);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}
