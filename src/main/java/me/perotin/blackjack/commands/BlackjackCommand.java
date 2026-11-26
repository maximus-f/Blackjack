package me.perotin.blackjack.commands;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.BlackjackGame;
import me.perotin.blackjack.objects.BlackjackPlayer;
import me.perotin.blackjack.objects.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.perotin.blackjack.objects.PendingPayout;

import java.text.DecimalFormat;


/* Created by Perotin on 12/27/18 */
public class BlackjackCommand implements CommandExecutor {


    private Blackjack plugin;

    private boolean secondaryBetOverride = false;

    public BlackjackCommand(Blackjack plugin){
        this.plugin = plugin;
        this.secondaryBetOverride = plugin.isSecondaryBetOverride();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) return true;
        Player player = (Player) commandSender;
        if (!player.hasPermission("blackjack.play")) {
            plugin.sendMessage(player, "no-permission");
            return true;
        }
        if (!PendingPayout.retry(plugin, player)) return true;
        GameSession session = plugin.getSessionFor(player.getUniqueId());
        if (args.length == 0) {
            if (session == null) plugin.sendMessage(player, "incorrect-args");
            else reopen(session);
            return true;
        }
        double betAmount;
        try {
            betAmount = Double.parseDouble(args[0]);
        } catch (NumberFormatException ex) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.sendMessage(player, "number-or-player");
                return true;
            }
            BlackjackPlayer stats = plugin.getPlayerFor(target);
            player.sendMessage(ChatColor.BLACK + "------------- " + ChatColor.RED + args[0] + ChatColor.BLACK + " -------------");
            player.sendMessage(plugin.getString("wins-stat").replace("$amount$", "" + stats.getWins()));
            player.sendMessage(plugin.getString("loss-stat").replace("$amount$", "" + stats.getLosses()));
            double ratio = stats.getLosses() == 0 ? stats.getWins() : (double) stats.getWins() / stats.getLosses();
            player.sendMessage(plugin.getString("ratio-stat").replace("$amount$", new DecimalFormat("#.##").format(ratio)));
            return true;
        }

        if (!plugin.validStake(betAmount)) {
            player.sendMessage("Enter a finite, nonnegative wager in the selected currency.");
            return true;
        }
        if (plugin.getBetMax() > 0 && betAmount > plugin.getBetMax()) {
            player.sendMessage(plugin.getString("bet-max-message").replace("$amount$", "" + plugin.getBetMax()));
            return true;
        }
        if (betAmount < plugin.getBetMin()) {
            player.sendMessage(plugin.getString("bet-min-message").replace("$amount$", "" + plugin.getBetMin()));
            return true;
        }
        if (session != null) {
            BlackjackGame active = session.hasOngoingGame();
            if (!secondaryBetOverride || active == null || active.getBetAmount() == betAmount) {
                reopen(session);
                return true;
            }
        }
        double balance = plugin.getPlayerFor(player).getBalance();
        int overflow = plugin.isOverflow() ? plugin.getConfig().getInt("bet-overflow-max") : 0;
        if (betAmount > balance + overflow) {
            String key = plugin.isOverflow() ? "can-only-bet" : "cannot-bet-that-much";
            player.sendMessage(plugin.getString(key).replace("$amount$", "" + overflow));
            return true;
        }
        if (!plugin.withdraw(betAmount, player)) {
            plugin.sendMessage(player, "cannot-bet-that-much");
            return true;
        }
        if (session != null) session.forfeitAndClose();
        BlackjackGame game = new BlackjackGame(player, betAmount);
        GameSession next = new GameSession(player.getUniqueId(), game);
        plugin.getSessions().add(next);
        next.openGame();
        if (plugin.getConfig().getBoolean("custom-command")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getConfig().getString("command")
                    .replace("$amount$", "" + betAmount));
        }
        return true;
    }

    private void reopen(GameSession session) {
        if (session.hasOngoingGame() == null) session.showEndMenu(session.latestGame());
        else session.openGame();
    }
}
