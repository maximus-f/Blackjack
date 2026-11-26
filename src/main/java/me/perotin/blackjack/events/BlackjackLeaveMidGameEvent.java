package me.perotin.blackjack.events;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.BlackjackGame;
import me.perotin.blackjack.objects.GameSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class BlackjackLeaveMidGameEvent implements Listener {
    private final Blackjack plugin;

    public BlackjackLeaveMidGameEvent(Blackjack plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getSessionFor(player.getUniqueId());
        if (session == null) return;
        BlackjackGame active = session.hasOngoingGame();
        if (active != null) {
            BlackjackJoinEvent.quitterMidGame.put(player.getUniqueId(), active.getBetAmount());
        }
        session.forfeitAndClose();
    }
}
