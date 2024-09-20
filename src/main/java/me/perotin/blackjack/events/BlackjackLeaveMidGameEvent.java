package me.perotin.blackjack.events;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.BlackjackGame;
import me.perotin.blackjack.objects.BlackjackPlayer;
import me.perotin.blackjack.objects.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;

/* Created by Perotin on 8/13/19 */
public class BlackjackLeaveMidGameEvent implements Listener {

    private Blackjack plugin;

    public BlackjackLeaveMidGameEvent(Blackjack blackjack) {
        this.plugin = blackjack;
    }

//    @EventHandler
//    public void closeMenu(InventoryCloseEvent event) {
//        InventoryView view = event.getView();
//        Player clicker = (Player) event.getPlayer();
//
//        BlackjackPlayer player = plugin.getPlayerFor(clicker);
//
//        BlackjackGame currentGame = null;
//        GameSession session = null;
//        boolean found = false;
//        if (plugin.getSessionFor(clicker.getUniqueId()) != null) {
//            for (BlackjackGame game : plugin.getSessionFor(clicker.getUniqueId()).getGames()) {
//                if(found) break;
//                if (game.getPlayer().getUniqueId().equals(clicker.getUniqueId())) {
//                    // same player
//                    String name = plugin.getString("menu-title").replace("$number$", "" + game.getBetAmount());
//                    if (event.getView().getTitle().equals(name)) {
//                        // same inventory, its safe to say its a game click
//                        currentGame = game;
//                        session = plugin.getSessionFor(clicker.getUniqueId());
//                        found = true;
//                    }
//
//
//                }
//            }
//        }
//
//        if (currentGame != null && session != null) {
//            session.endGame(currentGame, BlackjackGame.Ending.LOSE);
//            player.addLoss();
//        }
//    }

    @EventHandler
    public void leaveMidGame(PlayerQuitEvent event){
        exitMidSession(event.getPlayer());
    }

    @EventHandler
    public void leaveMidGame(PlayerKickEvent event){
        exitMidSession(event.getPlayer());
    }

    private void exitMidSession(Player player2) {
        Player quitter = player2;
        BlackjackPlayer player = plugin.getPlayerFor(quitter);

        BlackjackGame currentGame = null;
        GameSession session = null;
        if (plugin.getSessionFor(quitter.getUniqueId()) != null) {

            Bukkit.getConsoleSender().sendMessage("Deleting instance");

            for (BlackjackGame game : plugin.getSessionFor(quitter.getUniqueId()).getGames()) {
                if (game.getPlayer().getUniqueId().equals(quitter.getUniqueId())) {
                    // same player
                    String name = plugin.getString("menu-title").replace("$number$", "" + game.getBetAmount());


                    currentGame = game;
                    session = plugin.getSessionFor(quitter.getUniqueId());

                }
            }
        }
        if(currentGame != null && session != null){
            session.endGame(currentGame, BlackjackGame.Ending.LOSE);
            player.addLoss();
            session.endSession();
        }
    }

}
