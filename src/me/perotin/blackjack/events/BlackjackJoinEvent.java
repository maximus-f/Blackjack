package me.perotin.blackjack.events;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.BlackjackPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/* Created by Perotin on 3/25/19 */
public class BlackjackJoinEvent implements Listener {

    private Blackjack plugin;

    public BlackjackJoinEvent(Blackjack blackjack) {
        this.plugin = blackjack;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player joiner = event.getPlayer();
        if(!plugin.getPlayers().contains(plugin.getPlayerFor(joiner.getUniqueId()))){
            plugin.getPlayers().add(BlackjackPlayer.loadPlayer(joiner));
        }
    }
}
