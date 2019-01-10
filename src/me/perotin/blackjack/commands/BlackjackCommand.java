package me.perotin.blackjack.commands;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.BlackjackGame;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
            if(player.hasPermission("blackjack.play")) {
                if (args.length > 0) {
                    Double betAmount;

                    try {
                        betAmount = Double.parseDouble(args[0]);
                    } catch (NumberFormatException ex) {
                        // did not parse a number
                        player.sendMessage(plugin.getString("number-only"));
                        return true;
                    }

                    // search up if they already have an on-going game
                    boolean noOtherGames = true;
                    for(BlackjackGame game : plugin.getCurrentGames()){
                        if(game.getPlayer().getUniqueId().equals(player.getUniqueId())){
                            // they have an ongoing game
                            noOtherGames = false;
                            player.sendMessage(ChatColor.GRAY + "Opening previous game. . .");
                            new BukkitRunnable(){
                                @Override
                                public void run(){
                                    player.openInventory(game.getInventory(true));
                                    IntStream.range(0, 25).forEach(i -> player.sendMessage(""));

                                }
                            }.runTaskLater(plugin, 60);
                        }
                    }
                    if(noOtherGames) {
                        BlackjackGame game = new BlackjackGame(player, betAmount);
                        plugin.getCurrentGames().add(game);
                        player.openInventory(game.getInventory(true));
                    }

                } else {
                    boolean noOtherGames = true;
                    for(BlackjackGame game : plugin.getCurrentGames()){
                        if(game.getPlayer().getUniqueId().equals(player.getUniqueId())){
                            // they have an ongoing game
                            noOtherGames = false;
                            player.sendMessage(ChatColor.GRAY + "Opening previous game. . .");
                            new BukkitRunnable(){
                                @Override
                                public void run(){
                                    player.openInventory(game.getInventory(true));
                                    IntStream.range(0, 25).forEach(i -> player.sendMessage(""));

                                }
                            }.runTaskLater(plugin, 60);
                        }
                    }
                    if(noOtherGames) {
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
