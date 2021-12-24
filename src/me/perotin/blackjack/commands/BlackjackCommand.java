package me.perotin.blackjack.commands;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.BlackjackGame;
import me.perotin.blackjack.objects.BlackjackPlayer;
import me.perotin.blackjack.objects.GameSession;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.stream.IntStream;

/* Created by Perotin on 12/27/18 */
public class BlackjackCommand implements CommandExecutor {


    private Blackjack plugin;


    public BlackjackCommand(Blackjack plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;
            BlackjackPlayer bj = Blackjack.getInstance().getPlayerFor(player);

            if(player.hasPermission("blackjack.play")) {
                double betMax = plugin.getBetMax();
                double betMin = plugin.getBetMin();
                if (args.length >= 1) {
                    Double betAmount;

                    try {
                        betAmount = Double.parseDouble(args[0]);
                    } catch (NumberFormatException ex) {
                        if(Bukkit.getPlayer(args[0]) != null){
                            BlackjackPlayer blackjackPlayer = plugin.getPlayerFor(Bukkit.getPlayer(args[0]));

                            // send player stats
                            // eventually do all player lookups, not just online
                            if(blackjackPlayer != null){
                                player.sendMessage(ChatColor.BLACK + "------------- " + ChatColor.RED + args[0] + ChatColor.BLACK + " -------------");
                                player.sendMessage(plugin.getString("wins-stat").replace("$amount$", ""+blackjackPlayer.getWins()));
                                player.sendMessage(plugin.getString("loss-stat").replace("$amount$", ""+blackjackPlayer.getLosses()));
                                double ratio;
                                if(blackjackPlayer.getLosses() == 0) {
                                     ratio = blackjackPlayer.getWins();
                                } else {
                                    ratio = (double) blackjackPlayer.getWins() / blackjackPlayer.getLosses();
                                }

                                player.sendMessage(plugin.getString("ratio-stat").replace("$amount$",  new DecimalFormat("#.##").format(ratio)));

                                return true;
                            } else {
                                // should never be the case
                                player.sendMessage(ChatColor.RED+ "Something has gone wrong...");
                                return true;
                            }


                        } else{
                            player.sendMessage(plugin.getString("number-or-player"));
                            return true;
                        }
                    }

                    if(betAmount < 0){
                        betAmount = 0.0;
                    }
                    if(betMax > 0 && betAmount > betMax){
                        player.sendMessage(plugin.getString("bet-max-message")
                        .replace("$amount$", betMax+""));
                        return true;
                    }
                    if(betMin > 0 && betAmount < betMin){
                        player.sendMessage(plugin.getString("bet-min-message")
                                .replace("$amount$", betMin+""));
                        return true;
                    }

                    // search up if they already have an on-going game
                    boolean noOtherGames = true;
                    if(plugin.getSessionFor(player.getUniqueId()) != null) {
                        for (BlackjackGame game : plugin.getSessionFor(player.getUniqueId()).getGames()) {
                            if (game.getPlayer().getUniqueId().equals(player.getUniqueId()) && game.getResult() == Double.MAX_VALUE) {
                                // they have an ongoing game
                                noOtherGames = false;
                                player.sendMessage(plugin.getString("previous-game"));
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        player.openInventory(game.getInventory(true));
                                        IntStream.range(0, 25).forEach(i -> player.sendMessage(""));

                                    }
                                }.runTaskLater(plugin, 60);
                            }
                        }
                    }
                    if(plugin.isOverflow()) {
                        int overflowAmount = plugin.getConfig().getInt("bet-overflow-max");
                        if(betAmount > bj.getBalance() + overflowAmount){
                            // too much
                            player.sendMessage(plugin.getString("can-only-bet")
                            .replace("$amount$", overflowAmount+""));
                            return true;
                        }

                    } else {
                        if(betAmount > bj.getBalance()){
                            // can't
                            player.sendMessage(plugin.getString("cannot-bet-that-much"));
                            return true;
                        }
                    }
                    if(noOtherGames) {
                        if(plugin.getSessionFor(player.getUniqueId()) != null){
                            // they have a session already
                            GameSession session= plugin.getSessionFor(player.getUniqueId());
                            session.showEndMenu(session.getGames().get(session.getGames().size()-1));
                            return true;
                        }

                        BlackjackGame game = new BlackjackGame(player, betAmount);
                        GameSession session = new GameSession(player.getUniqueId(), game);
                       // EconomyResponse er = Blackjack.getEconomy().withdrawPlayer(player, betAmount);
                        Blackjack.withdraw(betAmount, player);
                        plugin.getSessions().add(session);
                        player.openInventory(game.getInventory(true));
                        if(plugin.getConfig().getBoolean("custom-command")){
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), plugin.getConfig().getString("command").replace("$amount$", betAmount+""));
                        }
                    }

                } else {
                    boolean noOtherGames = true;
                    if(plugin.getSessionFor(player.getUniqueId()) != null) {
                        for (BlackjackGame game : plugin.getSessionFor(player.getUniqueId()).getGames()) {
                            if (game.getPlayer().getUniqueId().equals(player.getUniqueId()) && game.getResult() == Double.MAX_VALUE) {
                                // they have an ongoing game
                                noOtherGames = false;
                                player.sendMessage(plugin.getString("previous-game", ChatColor.GRAY + "Opening previous game. . ."));
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        player.openInventory(game.getInventory(true));
                                        IntStream.range(0, 25).forEach(i -> player.sendMessage(""));

                                    }
                                }.runTaskLater(plugin, 60);
                            }
                        }
                    }
                    if(noOtherGames) {
                        if(plugin.getSessionFor(player.getUniqueId()) != null){
                            // they have a session already
                            GameSession session= plugin.getSessionFor(player.getUniqueId());
                            session.showEndMenu(session.getGames().get(session.getGames().size()-1));
                            return true;
                        }
                        player.sendMessage(plugin.getString("incorrect-args"));
                    }
                    return true;
                }
            } else {
                plugin.sendMessage(player, "no-permission");
                return true;
            }
        }
        return true;
    }
}
