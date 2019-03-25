package me.perotin.blackjack.objects;

import org.bukkit.entity.Player;

import java.util.UUID;

/* Created by Perotin on 2/25/19 */
public class BlackjackPlayer  {

    private final UUID uuid;
    private int wins;
    private int losses;


    public BlackjackPlayer(UUID uuid, int wins, int losses) {
        this.uuid = uuid;
        this.wins = wins;
        this.losses = losses;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getWins() {
        return wins;
    }
    public void addWin(){
        wins +=1;
    }

    public void addLoss(){
        losses +=1;
    }
    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public static BlackjackPlayer loadPlayer(Player player){
        BlackFile file = new BlackFile(BlackFile.BlackFilesType.STATS);
        if(file.getConfiguration().contains(player.getUniqueId().toString())) {
            int wins = file.getConfiguration().getInt(player.getUniqueId().toString() + ".wins");
            int losses = file.getConfiguration().getInt(player.getUniqueId().toString() + ".losses");
            return new BlackjackPlayer(player.getUniqueId(), wins, losses);
        } else {
            return new BlackjackPlayer(player.getUniqueId(), 0, 0);
        }

    }
}
