package me.perotin.blackjack.objects;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.events.BlackjackSessionClickEvent;
import me.perotin.blackjack.util.ItemBuilder;
import me.perotin.blackjack.util.XMaterial;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
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
    }

//    public void showBetChangeMenu(Player player){
//        Inventory menu = Bukkit.createInventory(null, 27, "Change bet amount...");
//        // setting clickers
//        menu.setItem(10, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.RED + "-1000").build());
//        menu.setItem(11, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.RED + "-100").build());
//        menu.setItem(12, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.RED + "-10").build());
//
//        menu.setItem(14, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.GREEN + "+10").build());
//        menu.setItem(15, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.GREEN + "+100").build());
//        menu.setItem(16, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.GREEN + "+1000").build());
//        menu.setItem(13, new ItemBuilder(XMaterial.ARROW.parseMaterial()).name(ChatColor.YELLOW + "Play next game!").lore(ChatColor.GRAY + "$"+betAmount).build());
//
//        player.openInventory(menu);
//        BlackjackSessionClickEvent.changingAmount.put(player.getUniqueId(), menu);
//
//    }

    public void endGame(BlackjackGame game, BlackjackGame.Ending end) {
        Blackjack plugin = Blackjack.getInstance();
//        player.sendMessage(plugin.getString("end-game")
//                .replace("$score$", getScoreUnder21(getPlayerCards()) + "")
//                .replace("$score2$", getScoreUnder21(getHouseCards()) + ""));
        game.setEnd(end);
        double betAmount = game.getBetAmount();
        Player player = game.getPlayer();
        if (end == BlackjackGame.Ending.WIN) {
            EconomyResponse er;
            boolean taxxed = false;
            if (plugin.getTaxPercent() != 0.0 && plugin.getTaxPercent() <= 100.0) {
                double tax = plugin.getTaxPercent() / 100.0;
                double postTax = betAmount - (tax * betAmount);
                er = Blackjack.getEconomy().depositPlayer(player, postTax + betAmount);
                taxxed = true;
            } else {
                er = Blackjack.getEconomy().depositPlayer(player, betAmount + betAmount);

            }


//            player.sendMessage(plugin.getString("earnings")
//                    .replace("$result$", plugin.getString("won"))
//                    .replace("$number$", earnings + ""));

            plugin.setServerImpact(plugin.getServerImpact() - betAmount);
            plugin.increaseGamesPlayed();
            plugin.increaseServerLosses();
            if (er.transactionSuccess()) {
                if (taxxed) {
                    double tax = plugin.getTaxPercent() / 100.0;
                    //  player.sendMessage(plugin.getString("taxxed").replace("$amount$", postTax+""));
                } else {
                    player.sendMessage(ChatColor.GREEN + "+" + betAmount);

                }
            }
        } else if (end == BlackjackGame.Ending.LOSE) {

//            player.sendMessage(plugin.getString("earnings")
//                    .replace("$result$", plugin.getString("lost"))
//                    .replace("$number$", getBetAmount() + ""));
//            plugin.setServerImpact(plugin.getServerImpact() + betAmount);
            plugin.increaseGamesPlayed();
            plugin.increaseServerWins();

        } else if (end == BlackjackGame.Ending.TIE) {
            // tie
            //player.sendMessage(plugin.getString("tied"));
            EconomyResponse er = Blackjack.getEconomy().depositPlayer(player, betAmount);
            plugin.increaseGamesPlayed();
        } else {
            // they surrender
            double surrender = betAmount - (betAmount * (plugin.getSurrenderPercentage() / 100));
//            player.sendMessage(plugin.getString("surrender-message")
//            .replace("$amount$", surrender+"").replace("$bet$", betAmount+""));

            Blackjack.getEconomy().depositPlayer(player, surrender);
            plugin.setServerImpact(plugin.getServerImpact() + surrender);
            plugin.increaseGamesPlayed();
            plugin.increaseServerWins();

        }

        showEndMenu(game);

    }

    public void startNewGame(){
        if(Blackjack.getEconomy().getBalance(getPlayer()) >= betAmount) {
            BlackjackGame game = new BlackjackGame(getPlayer(), betAmount);
            getGames().add(game);
            EconomyResponse er = Blackjack.getEconomy().withdrawPlayer(getPlayer(), betAmount);

            getPlayer().openInventory(game.getInventory(true));
        } else {
            getPlayer().closeInventory();
            endSession();
            getPlayer().sendMessage(Blackjack.getInstance().getString("cannot-bet-that-much"));
        }

    }

    public void showEndMenu(BlackjackGame game){
        Blackjack plugin = Blackjack.getInstance();

        Inventory menu = Bukkit.createInventory(null, 36, "Continue playing, " +game.getPlayer().getName()+"?");
        IntStream.range(0, 10).forEach(i -> menu.setItem(i, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem().getType()).name("").build()));
        IntStream.range(27, 36).forEach(i -> menu.setItem(i, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem().getType()).name("").build()));
        menu.setItem(18, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name("").build());
        menu.setItem(27, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name("").build());
        menu.setItem(17, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name("").build());
        menu.setItem(26, new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial()).name("").build());
        menu.setItem(3, new ItemBuilder(XMaterial.BOOKSHELF.parseMaterial()).name(ChatColor.YELLOW + "Dealer's total: " + game.getScoreUnder21(game.getHouseCards())).build());
        menu.setItem(5, new ItemBuilder(XMaterial.BOOKSHELF.parseMaterial()).name(ChatColor.YELLOW + "Your total: " + game.getScoreUnder21(game.getPlayerCards())).build());





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
            menu.setItem(19, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.RED + "-1000").build());
            menu.setItem(20, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.RED + "-100").build());
            menu.setItem(21, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.RED + "-10").build());

            menu.setItem(23, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.GREEN + "+10").build());
            menu.setItem(24, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.GREEN + "+100").build());
            menu.setItem(25, new ItemBuilder(XMaterial.DANDELION.parseItem().getType()).name(ChatColor.GREEN + "+1000").build());
            menu.setItem(22, new ItemBuilder(XMaterial.BOOK.parseMaterial()).name(ChatColor.YELLOW + "Change bet amount!").lore(ChatColor.GRAY + "$" + betAmount).build());
        } else {
            menu.setItem(22, new ItemBuilder(XMaterial.BOOK.parseMaterial()).name(ChatColor.YELLOW + "Your bet amount!").lore(ChatColor.GRAY + "$" + betAmount).build());

        }

        game.getPlayer().openInventory(menu);
    }



}
