package me.perotin.blackjack.objects;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.util.ItemBuilder;
import me.perotin.blackjack.util.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/* Created by Perotin on 6/28/19 */
public class GameSession {

    private final UUID uuid;
    private final List<BlackjackGame> games = new ArrayList<>();
    private BlackjackMenu menu;
    private double betAmount;

    public GameSession(UUID uuid, BlackjackGame game) {
        this.uuid = uuid;
        games.add(game);
        betAmount = game.getBetAmount();
    }

    public UUID getUuid() { return uuid; }

    public List<BlackjackGame> getGames() { return games; }

    public double getBetAmount() { return betAmount; }

    public void setBetAmount(double amount) { this.betAmount = amount; }

    public BlackjackGame latestGame() { return games.get(games.size() - 1); }

    public BlackjackGame hasOngoingGame() {
        BlackjackGame game = latestGame();
        return game.getSettlement() == null ? game : null;
    }

    private double getTotalEarnings() {
        double total = 0;
        for (BlackjackGame game : games) {
            if (game.getSettlement() != null) total += game.getSettlement().net;
        }
        return total;
    }

    private Player getPlayer() { return latestGame().getPlayer(); }

    public boolean ownsMenu(Inventory inventory) {
        return menu != null && inventory.getHolder() == menu;
    }

    public void openGame() {
        BlackjackGame game = hasOngoingGame();
        if (game == null) return;
        String title = Blackjack.getInstance().getString("menu-title").replace("$number$", "" + game.getBetAmount());
        menu = new BlackjackMenu(54, title);
        game.fillInventory(menu.getInventory());
        getPlayer().openInventory(menu.getInventory());
    }

    public void endSession() {
        menu = null;
        Blackjack plugin = Blackjack.getInstance();
        plugin.getSessions().remove(this);
        getPlayer().closeInventory();
        double total = getTotalEarnings();
        String display = total > 0 ? ChatColor.GREEN + "+" + total : Double.toString(total);
        getPlayer().sendMessage(plugin.getString("session-ending")
                .replace("$amount$", Integer.toString(games.size())).replace("$outcome$", display));
    }

    public void forfeitAndClose() {
        BlackjackGame game = hasOngoingGame();
        if (game != null) settle(game, BlackjackGame.Ending.LOSE, false);
        endSession();
    }

    public void endGame(BlackjackGame game, BlackjackGame.Ending ending) {
        settle(game, ending, true);
    }

    private void settle(BlackjackGame game, BlackjackGame.Ending ending, boolean showMenu) {
        if (game != hasOngoingGame()) return;
        Blackjack plugin = Blackjack.getInstance();
        Player player = getPlayer();
        RoundResult result = game.finish(ending, plugin.getBlackJackMultiplier(), plugin.getTaxPercent(),
                plugin.getSurrenderPercentage(), plugin.isUsingCash());
        if (result == null) return;
        if (!plugin.deposit(result.payout, player)) {
            boolean saved = PendingPayout.save(plugin, player, game.getUuid(), result);
            plugin.getSessions().remove(this);
            menu = null;
            player.closeInventory();
            if (saved) {
                player.sendMessage("Your payout was declined. Use /blackjack to retry before your next bet.");
            } else {
                player.sendMessage("Your payout requires administrator review. Round: " + game.getUuid());
            }
            return;
        }
        plugin.recordOutcome(player, ending, result.net);
        if (!showMenu) return;
        player.sendMessage(plugin.getString("end-game")
                .replace("$score$", "" + game.getScoreUnder21(game.getPlayerCards()))
                .replace("$score2$", "" + game.getScoreUnder21(game.getHouseCards())));
        if (result.tax > 0) {
            player.sendMessage(plugin.getString("taxxed").replace("$amount$", "" + result.tax));
        }
        if (result.blackjack) {
            player.sendMessage(plugin.getString("blackjack-win").replace("$amount$", "" + plugin.getBlackJackMultiplier())
                    + ChatColor.GREEN + "+" + result.net);
        } else if (ending == BlackjackGame.Ending.TIE) {
            player.sendMessage(plugin.getString("tied"));
        } else {
            String message = result.net < 0 ? "amount-subtracted" : "amount-added";
            player.sendMessage(plugin.getString(message).replace("$amount$", "" + Math.abs(result.net)));
        }
        showEndMenu(game);
    }

    public void startNewGame() {
        if (hasOngoingGame() != null) return;
        Blackjack plugin = Blackjack.getInstance();
        Player player = getPlayer();
        if (plugin.getPlayerFor(player).getBalance() < betAmount || !plugin.withdraw(betAmount, player)) {
            player.sendMessage(plugin.getString("cannot-bet-that-much"));
            return;
        }
        games.add(new BlackjackGame(player, betAmount));
        openGame();
    }

    public void showEndMenu(BlackjackGame game){
        Blackjack plugin = Blackjack.getInstance();
        List<Integer> decoSpots = new ArrayList<>();
        IntStream.range(0, 10).forEach(decoSpots::add);
        IntStream.range(27, 36).forEach(decoSpots::add);
        decoSpots.addAll(Arrays.asList(18, 17, 26));
        decoSpots.remove(5);
        decoSpots.remove(4);
        decoSpots.remove(3);



        String name = plugin.getString("continue-playing")
                .replace("$player$", game.getPlayer().getName());
        this.menu = new BlackjackMenu(36, name);
        Inventory menu = this.menu.getInventory();
        IntStream.range(0, 10).forEach(i -> menu.setItem(i, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem().getType()).name("").build()));
        IntStream.range(27, 36).forEach(i -> menu.setItem(i, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem().getType()).name("").build()));
        menu.setItem(18, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name(" ").build());
        menu.setItem(27, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name(" ").build());
        menu.setItem(17, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name(" ").build());
        menu.setItem(26, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name(" ").build());
        menu.setItem(3, new ItemBuilder(XMaterial.BOOKSHELF.parseMaterial()).name(plugin.getString("dealer-cards") + game.getScoreUnder21(game.getHouseCards())).build());
        menu.setItem(5, new ItemBuilder(XMaterial.BOOKSHELF.parseMaterial()).name(plugin.getString("player-cards") + game.getScoreUnder21(game.getPlayerCards())).build());


        if(game.getSettlement().blackjack){
            //blackjack
            BukkitRunnable runnable = new BukkitRunnable() {
                int counter = 0;

                @Override
                public void run() {
                    int innerCounter = 0;
                    for(int i : decoSpots){
                        if(counter % 2 == 0){
                            if(innerCounter % 2 == 0) {
                                menu.setItem(i, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE.parseItem()).name(plugin.getString("blackjack")).build());
                                innerCounter++;
                                if(decoSpots.get(decoSpots.size() - 1) == i)  counter++;
                            } else {

                                menu.setItem(i, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem()).name(plugin.getString("blackjack")).build());
                                innerCounter++;
                                if(decoSpots.get(decoSpots.size() - 1) == i)   counter++;

                            }
                        } else {
                            if(innerCounter % 2 == 0) {

                                menu.setItem(i, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem()).name(plugin.getString("blackjack")).build());
                                innerCounter++;
                                if(decoSpots.get(decoSpots.size() - 1) == i) counter++;

                            } else {

                                menu.setItem(i, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE.parseItem()).name(plugin.getString("blackjack")).build());
                                innerCounter++;
                                if(decoSpots.get(decoSpots.size() - 1) == i) counter++;

                            }
                        }

                    }

                }
            };

            runnable.runTaskTimer(plugin, 0, 20);
            BukkitTask cancel = new BukkitRunnable() {
                @Override
                public void run() {
                    runnable.cancel();
                }
            }.runTaskLater(plugin, 20*20);


        }




        ItemBuilder gameEarnings = new ItemBuilder(XMaterial.ENCHANTED_BOOK.parseItem());
        gameEarnings.name(plugin.getString("earnings-gui")).lore(ChatColor.GRAY + ""+game.getSettlement().net);
        menu.setItem(13, gameEarnings.build());

        ItemBuilder keepPlaying = new ItemBuilder(XMaterial.EMERALD.parseItem());
        keepPlaying.name(plugin.getString("keep-playing"));
        menu.setItem(14, keepPlaying.build());

        ItemBuilder stopPlaying = new ItemBuilder(XMaterial.REDSTONE.parseItem());
        stopPlaying.name(plugin.getString("stop-playing"));
        menu.setItem(12, stopPlaying.build());

        ItemBuilder totalEarnings = new ItemBuilder(XMaterial.PAPER.parseItem());
        totalEarnings.name(plugin.getString("total-earnings")
        .replace("$amount$", getTotalEarnings()+""));
        menu.setItem(4, totalEarnings.build());

        if(Blackjack.getInstance().getConfig().getBoolean("enable-change-bet")) {
            int betIncrease1 = Blackjack.getInstance().getConfig().getInt("bet-modifiers.increaseValue1");
            int betIncrease2 = Blackjack.getInstance().getConfig().getInt("bet-modifiers.increaseValue2");
            int betIncrease3 = Blackjack.getInstance().getConfig().getInt("bet-modifiers.increaseValue3");
            int betDecrease1 = Blackjack.getInstance().getConfig().getInt("bet-modifiers.decreaseValue1");
            int betDecrease2 = Blackjack.getInstance().getConfig().getInt("bet-modifiers.decreaseValue2");
            int betDecrease3 = Blackjack.getInstance().getConfig().getInt("bet-modifiers.decreaseValue3");

            menu.setItem(19, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.RED + "-" + betDecrease3).build());
            menu.setItem(20, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.RED + "-" + betDecrease2).build());
            menu.setItem(21, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.RED + "-" + betDecrease1).build());

            menu.setItem(23, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.GREEN + "+" + betIncrease1).build());
            menu.setItem(24, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.GREEN + "+" + betIncrease2).build());
            menu.setItem(25, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.GREEN + "+" + betIncrease3).build());
            menu.setItem(22, new ItemBuilder(XMaterial.BOOK.parseMaterial()).name(plugin.getString("change-bet-amount", ChatColor.YELLOW + "Change bet amount!")).lore(ChatColor.GRAY + "$" + betAmount).build());
        } else {
            menu.setItem(22, new ItemBuilder(XMaterial.BOOK.parseMaterial()).name(plugin.getString("your-bet-amount", ChatColor.YELLOW + "Your bet amount!")).lore(ChatColor.GRAY + "$" + betAmount).build());

        }

        if (menu != null && game.getPlayer() != null) {
            game.getPlayer().openInventory(menu);
        }
    }



}
