package me.perotin.blackjack.commands;

import me.perotin.blackjack.Blackjack;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.stream.IntStream;

/* Created by Perotin on 6/25/19 */
public class BlackjackAdminCommand implements CommandExecutor {

    private Blackjack plugin;

    public BlackjackAdminCommand(Blackjack blackjack) {
        this.plugin = blackjack;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(sender.hasPermission("blackjack.admin")){
            if(args.length == 0){
                // default
                IntStream.range(0, 3).forEach(x -> sender.sendMessage(" "));
                double impact = plugin.getServerImpact();
                if(impact > 0)
                    sender.sendMessage(ChatColor.YELLOW + "Server impact: " + ChatColor.GREEN + "+"+impact);
                else if(impact < 0)
                    sender.sendMessage(ChatColor.YELLOW + "Server impact: " + ChatColor.RED + "-"+impact);
                else
                    sender.sendMessage(ChatColor.YELLOW + "Server impact: " + ChatColor.WHITE + impact);

                double ties = plugin.getGames() - plugin.getServerLosses() - plugin.getServerWins();
                double winRate = 100 * (plugin.getServerWins() / plugin.getGames());

                double tWins, tLosses, tGames, tWinRate, tLossRate;
                tWins = plugin.getTotalServerWins();
                tLosses = plugin.getTotalServerLosses();
                tGames = plugin.getTotalServerGames();
                tWinRate = tWins / tGames;
                tLossRate = tLosses / tGames;


                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',  "&7&oSince last restart, the server has recorded these stats..."));
                sender.sendMessage(ChatColor.YELLOW + "Server Wins: " + ChatColor.GREEN+plugin.getServerWins() + ChatColor.GRAY + "("+winRate+"% win rate)");
                sender.sendMessage(ChatColor.YELLOW + "Server Losses: " + ChatColor.RED+plugin.getServerLosses());
                sender.sendMessage(ChatColor.YELLOW + "Server Ties: " + ChatColor.WHITE+ties);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',  "&7&oThe server has recorded these stats over "+ tGames+ " of BlackJack ..."));
                sender.sendMessage(ChatColor.YELLOW + "Total Server Wins: " + ChatColor.GREEN+tWins + ChatColor.GRAY + "("+tWinRate+"% win rate)");
                sender.sendMessage(ChatColor.YELLOW + "Total Server Losses: " + ChatColor.RED+tLosses + ChatColor.GRAY + "("+tLossRate+"% loss rate)");
                sender.sendMessage(ChatColor.YELLOW + "Total Server Ties: " + ChatColor.WHITE+ties);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',  "On average, the server should win 52% of games."));
                sender.sendMessage(" ");

                sender.sendMessage(ChatColor.RED + "You are running version " + plugin.getDescription().getVersion() + " made by Perotin");
            }

        } else {
             sender.sendMessage(plugin.getString("no-permission"));
             return true;
        }
        return true;
    }
}
