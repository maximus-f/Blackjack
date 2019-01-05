package me.perotin.blackjack.commands;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.BlackjackGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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


                    BlackjackGame game = new BlackjackGame(player, betAmount);
                    plugin.getCurrentGames().add(game);
                    player.openInventory(game.getInventory(true));

                } else {
                    player.sendMessage(plugin.getString("incorrect-args"));
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
