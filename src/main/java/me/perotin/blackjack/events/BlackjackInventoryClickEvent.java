package me.perotin.blackjack.events;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.BlackjackGame;
import me.perotin.blackjack.objects.BlackjackMenu;
import me.perotin.blackjack.objects.GameSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class BlackjackInventoryClickEvent implements Listener {
    private final Blackjack plugin;

    public BlackjackInventoryClickEvent(Blackjack plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BlackjackMenu) event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof BlackjackMenu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        GameSession session = plugin.getSessionFor(player.getUniqueId());
        if (session == null || !session.ownsMenu(inventory)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize() || inventory.getItem(slot) == null) return;
        BlackjackGame game = session.hasOngoingGame();
        if (game == null) {
            clickResults(session, player, slot);
            return;
        }
        switch (slot) {
            case 0:
                player.closeInventory();
                for (String line : plugin.getConfig().getStringList("tutorial")) {
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                break;
            case 22:
                if (!game.isDoubled()) draw(session, game);
                break;
            case 30:
                double stake = game.getBetAmount();
                if (game.isDoubled() || game.getPlayerCards().size() != 2 || !plugin.isDoubleDownEnabled()) return;
                if (!plugin.validStake(stake * 2)
                        || (!plugin.isDoubleDownOverFlow() && plugin.getPlayerFor(player).getBalance() < stake)
                        || !plugin.withdraw(stake, player)) {
                    player.sendMessage(plugin.getString("cannot-bet-that-much"));
                    return;
                }
                game.doubleStake();
                draw(session, game);
                break;
            case 31:
                session.endGame(game, game.playDealer());
                break;
            case 32:
                if (!game.isDoubled() && plugin.isSurrenderEnabled()) session.endGame(game, BlackjackGame.Ending.SURRENDER);
                break;
            default:
                break;
        }
    }

    private void draw(GameSession session, BlackjackGame game) {
        game.getNextCard();
        if (game.getScoreUnder21(game.getPlayerCards()) > 21) {
            session.endGame(game, BlackjackGame.Ending.LOSE);
        } else {
            session.openGame();
        }
    }

    private void clickResults(GameSession session, Player player, int slot) {
        if (slot == 12) {
            session.endSession();
        } else if (slot == 14) {
            session.startNewGame();
        } else if (plugin.getConfig().getBoolean("enable-change-bet")) {
            double change;
            if (slot >= 19 && slot <= 21) {
                change = -plugin.getConfig().getInt("bet-modifiers.decreaseValue" + (22 - slot));
            } else if (slot >= 23 && slot <= 25) {
                change = plugin.getConfig().getInt("bet-modifiers.increaseValue" + (slot - 22));
            } else {
                return;
            }
            double next = session.getBetAmount() + change;
            if (!plugin.validStake(next) || next < 1 || next < plugin.getBetMin()
                    || (plugin.getBetMax() > 0 && next > plugin.getBetMax())
                    || next > plugin.getPlayerFor(player).getBalance()) return;
            session.setBetAmount(next);
            session.showEndMenu(session.latestGame());
        }
    }
}
