package me.perotin.blackjack.objects;

import me.perotin.blackjack.Blackjack;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

/** A declined payout survives logout or restart. Only the main server thread uses this journal. */
public final class PendingPayout {
    private static Path path(Blackjack plugin, Player player) {
        return plugin.getDataFolder().toPath().resolve("pending-payouts").resolve(player.getUniqueId() + ".properties");
    }

    public static boolean save(Blackjack plugin, Player player, UUID round, RoundResult result) {
        Properties entry = new Properties();
        entry.setProperty("round", round.toString());
        entry.setProperty("cash", Boolean.toString(plugin.isUsingCash()));
        entry.setProperty("amount", Double.toString(result.payout));
        entry.setProperty("net", Double.toString(result.net));
        entry.setProperty("outcome", result.outcome.name());
        Path target = path(plugin, player);
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), "payout-", ".tmp");
            try (Writer out = Files.newBufferedWriter(temp)) {
                entry.store(out, "Blackjack pending payout");
            }
            Files.move(temp, target);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save unpaid round " + round + " for "
                    + player.getUniqueId() + " (" + entry + "). Manual reconciliation required.", ex);
            return false;
        }
    }

    public static boolean retry(Blackjack plugin, Player player) {
        Path pending = path(plugin, player);
        Path claimed = pending.resolveSibling(pending.getFileName() + ".processing");
        if (Files.exists(claimed)) {
            player.sendMessage("Your previous payout requires review by a server administrator.");
            return false;
        }
        if (!Files.exists(pending)) return true;
        try {
            Properties entry = new Properties();
            try (Reader in = Files.newBufferedReader(pending)) {
                entry.load(in);
            }
            if (plugin.isUsingCash() != Boolean.parseBoolean(entry.getProperty("cash"))) {
                player.sendMessage("Your previous payout is pending in a different currency.");
                return false;
            }
            double amount = Double.parseDouble(entry.getProperty("amount"));
            double net = Double.parseDouble(entry.getProperty("net"));
            BlackjackGame.Ending outcome = BlackjackGame.Ending.valueOf(entry.getProperty("outcome"));
            // Vault has no idempotency key. An interrupted transfer must be checked before retrying.
            Files.move(pending, claimed);
            if (!plugin.deposit(amount, player)) {
                Files.move(claimed, pending);
                player.sendMessage("Your payout is still pending. Try /blackjack again later.");
                return false;
            }
            plugin.recordOutcome(player, outcome, net);
            Files.delete(claimed);
            player.sendMessage("Your pending Blackjack payout was credited: " + amount);
            return true;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not reconcile payout for " + player.getUniqueId(), ex);
            player.sendMessage("Your previous payout requires review by a server administrator.");
            return false;
        }
    }

    private PendingPayout() {}
}
