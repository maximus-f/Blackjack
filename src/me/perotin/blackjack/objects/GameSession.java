package me.perotin.blackjack.objects;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.util.ItemBuilder;
import me.perotin.blackjack.util.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/* Created by Perotin on 6/28/19 */
public class GameSession {

    private UUID uuid;
    private List<BlackjackGame> games;
    private double betAmount;


    public GameSession(UUID uuid, BlackjackGame game) {
        this.uuid = uuid;
        this.games = new ArrayList<>();
        games.add(game);
        this.betAmount = game.getBetAmount();
    }

    public double getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(double betAmount) {
        this.betAmount = betAmount;
    }

    private double getTotalEarnings(){
        double earnings = 0;
        for(BlackjackGame game : games){
            earnings += game.getResult();
        }
        return earnings;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public List<BlackjackGame> getGames() {
        return games;
    }

    public void addGame(List<BlackjackGame> games) {
        this.games.addAll(games);
    }

    private Player getPlayer(){
        return Bukkit.getPlayer(uuid);
    }

    public void endSession(){
       getPlayer().closeInventory();
        ItemBuilder stopPlaying = new ItemBuilder(XMaterial.REDSTONE.parseItem());
        stopPlaying.name(Blackjack.getInstance().getString("stop-playing"));

       if(getTotalEarnings() > 0) {
           getPlayer().sendMessage(Blackjack.getInstance().getString("session-ending")
                   .replace("$amount$", games.size() + "").replace("$outcome$", ChatColor.GREEN + "+"+getTotalEarnings() + ""));
       } else if (getTotalEarnings() < 0) {
           getPlayer().sendMessage(Blackjack.getInstance().getString("session-ending")
                   .replace("$amount$", games.size() + "").replace("$outcome$", ChatColor.RED + ""+getTotalEarnings()));
        } else {
           getPlayer().sendMessage(Blackjack.getInstance().getString("session-ending")
                   .replace("$amount$", games.size() + "").replace("$outcome$", getTotalEarnings()+"" ));
       }
       Blackjack.getInstance().getSessions().remove(this);

       Bukkit.getScheduler().runTaskLater(Blackjack.getInstance(), () -> {
           Bukkit.broadcastMessage(getPlayer().getInventory().getContents().length + "!");



           Bukkit.broadcastMessage(stopPlaying.build().toString());

           if (getPlayer().getInventory().contains(stopPlaying.build())) {
               // people can shift click item into their inventory so remove it
               getPlayer().getInventory().remove(stopPlaying.build());

           }
       },20*2);
    }


    public void endGame(BlackjackGame game, BlackjackGame.Ending end) {
        Blackjack plugin = Blackjack.getInstance();
        getPlayer().sendMessage(plugin.getString("end-game")
                .replace("$score$", game.getScoreUnder21(game.getPlayerCards()) + "")
                .replace("$score2$", game.getScoreUnder21(game.getHouseCards()) + ""));
        game.setEnd(end);
        double betAmount = game.getBetAmount();
        Player player = game.getPlayer();
        int playerScore = game.getScoreUnder21(game.getPlayerCards());
        double multiplierAmount = (plugin.getBlackJackMultiplier() * betAmount) + betAmount;
        boolean blackJack = false;
        if (end == BlackjackGame.Ending.WIN) {

            boolean taxxed = false;
            if (plugin.getTaxPercent() != 0.0 && plugin.getTaxPercent() <= 100.0) {
                double tax = plugin.getTaxPercent() / 100.0;
                double postTax = betAmount - (tax * betAmount);
                if(playerScore == 21 && plugin.getBlackJackMultiplier() > 1) {
                    blackJack = true;
                    Blackjack.deposit( (plugin.getBlackJackMultiplier()*postTax) + betAmount, player);
                } else {
                    Blackjack.deposit( postTax + betAmount, player);
                }
                taxxed = true;
            } else {
                if(playerScore == 21 && plugin.getBlackJackMultiplier() > 1) {
                    blackJack = true;
                   Blackjack.deposit(multiplierAmount, player);
                } else {
                     Blackjack.deposit( betAmount + betAmount, player);

                }

            }


//            player.sendMessage(plugin.getString("earnings")
//                    .replace("$result$", plugin.getString("won"))
//                    .replace("$number$", earnings + ""));

            if(blackJack) {
                plugin.setServerImpact(plugin.getServerImpact() - (plugin.getBlackJackMultiplier()* betAmount));
            } else plugin.setServerImpact(plugin.getServerImpact() - betAmount);

            plugin.increaseGamesPlayed();
            plugin.increaseServerLosses();

                if (taxxed) {
                    double tax = plugin.getTaxPercent() / 100.0;
                    double postTax = tax * betAmount;

                    player.sendMessage(plugin.getString("taxxed").replace("$amount$", postTax+""));
                } else {
                    if(blackJack){
                    player.sendMessage(plugin.getString("blackjack-win").replace("$amount$", plugin.getConfig().getDouble("multiplier")+"")+
                            ChatColor.GREEN + "+" + (plugin.getBlackJackMultiplier() * betAmount));
                } else {
                        player.sendMessage( plugin.getString("amount-added").replace("$amount$", betAmount+""));
                    }
                }

        } else if (end == BlackjackGame.Ending.LOSE) {

//            player.sendMessage(plugin.getString("earnings")
//                    .replace("$result$", plugin.getString("lost"))
//                    .replace("$number$", getBetAmount() + ""));
            plugin.setServerImpact(plugin.getServerImpact() + betAmount);
            plugin.increaseGamesPlayed();
            plugin.increaseServerWins();
            player.sendMessage( plugin.getString("amount-subtracted").replace("$amount$", betAmount+""));


        } else if (end == BlackjackGame.Ending.TIE) {
            // tie
            //player.sendMessage(plugin.getString("tied"));
           Blackjack.deposit(betAmount, player);
            plugin.increaseGamesPlayed();
        } else {
            // they surrender
            double surrender = betAmount - (betAmount * (plugin.getSurrenderPercentage() / 100));
//            player.sendMessage(plugin.getString("surrender-message")
//            .replace("$amount$", surrender+"").replace("$bet$", betAmount+""));

            Blackjack.deposit(surrender, player);
            plugin.setServerImpact(plugin.getServerImpact() + surrender);
            plugin.increaseGamesPlayed();
            plugin.increaseServerWins();
            player.sendMessage( plugin.getString("amount-subtracted").replace("$amount$", surrender+""));


        }

        showEndMenu(game);

    }

    public void startNewGame(){
        BlackjackPlayer player = Blackjack.getInstance().getPlayerFor(getPlayer());
        if(player.getBalance() >= betAmount) {
            BlackjackGame game = new BlackjackGame(getPlayer(), betAmount);
            getGames().add(game);
             Blackjack.withdraw(betAmount, getPlayer());

            getPlayer().openInventory(game.getInventory(true));
        } else {
            getPlayer().closeInventory();
            endSession();
            getPlayer().sendMessage(Blackjack.getInstance().getString("cannot-bet-that-much"));
        }

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



        Inventory menu = Bukkit.createInventory(null, 36, "Continue playing, " +game.getPlayer().getName()+"?");
        IntStream.range(0, 10).forEach(i -> menu.setItem(i, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem().getType()).name("").build()));
        IntStream.range(27, 36).forEach(i -> menu.setItem(i, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem().getType()).name("").build()));
        menu.setItem(18, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name(" ").build());
        menu.setItem(27, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name(" ").build());
        menu.setItem(17, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name(" ").build());
        menu.setItem(26, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name(" ").build());
        menu.setItem(3, new ItemBuilder(XMaterial.BOOKSHELF.parseMaterial()).name(plugin.getString("dealer-cards") + game.getScoreUnder21(game.getHouseCards())).build());
        menu.setItem(5, new ItemBuilder(XMaterial.BOOKSHELF.parseMaterial()).name(plugin.getString("player-cards") + game.getScoreUnder21(game.getPlayerCards())).build());

        if(game.getScoreUnder21(game.getPlayerCards()) == 21 && game.getResult() > 0){
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
        gameEarnings.name(plugin.getString("earnings-gui")).lore(ChatColor.GRAY + ""+game.getResult());
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
            menu.setItem(19, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.RED + "-1000").build());
            menu.setItem(20, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.RED + "-100").build());
            menu.setItem(21, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.RED + "-10").build());

            menu.setItem(23, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.GREEN + "+10").build());
            menu.setItem(24, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.GREEN + "+100").build());
            menu.setItem(25, new ItemBuilder(XMaterial.SUNFLOWER.parseItem().getType()).name(ChatColor.GREEN + "+1000").build());
            menu.setItem(22, new ItemBuilder(XMaterial.BOOK.parseMaterial()).name(plugin.getString("change-bet-amount", ChatColor.YELLOW + "Change bet amount!")).lore(ChatColor.GRAY + "$" + betAmount).build());
        } else {
            menu.setItem(22, new ItemBuilder(XMaterial.BOOK.parseMaterial()).name(plugin.getString("your-bet-amount", ChatColor.YELLOW + "Your bet amount!")).lore(ChatColor.GRAY + "$" + betAmount).build());

        }

        game.getPlayer().openInventory(menu);
    }



}
