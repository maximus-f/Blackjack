package me.perotin.blackjack;

import me.perotin.blackjack.commands.BlackjackCommand;
import me.perotin.blackjack.events.BlackjackInventoryClickEvent;
import me.perotin.blackjack.events.BlackjackJoinEvent;
import me.perotin.blackjack.objects.BlackFile;
import me.perotin.blackjack.objects.BlackjackGame;
import me.perotin.blackjack.objects.BlackjackPlayer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/* Created by Perotin on 12/24/18 */
public class Blackjack extends JavaPlugin {

    private static Blackjack instance;
    private HashSet<BlackjackGame> currentGames;
    private static Economy econ = null;
    private boolean overFlow;
    private boolean cash;
    private Set<BlackjackPlayer> players;

    public static String[] cards = {
            "As",  "2s", "3s", "4s", "5s", "6s", "7s", "8s", "9s", "10s", "Js", "Qs", "Ks",
            "Ac",  "2c", "3c", "4c", "5c", "6c", "7c", "8c", "9c", "10c", "Jc", "Qc", "Kc",
            "Ah",  "2h", "3h", "4h", "5h", "6h", "7h", "8h", "9h", "10h", "Jh", "Qh", "Kh",
            "Ad",  "2d", "3d", "4d", "5d", "6d", "7d", "8d", "9d", "10d", "Jd", "Qd", "Kd"};
    @Override
    public void onEnable(){
        setupEconomy();
        currentGames = new HashSet<>();
        players = new HashSet<>();
        instance = this;
        saveDefaultConfig();
        // custom command string, remember that kangarko stuff ye
        getCommand("blackjack").setExecutor(new BlackjackCommand(this));
        Bukkit.getPluginManager().registerEvents(new BlackjackInventoryClickEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new BlackjackJoinEvent(this), this);


        this.overFlow = getConfig().getBoolean("bet-overflow");
        BlackFile.loadFiles();

        for(Player player : Bukkit.getOnlinePlayers()){
            players.add(BlackjackPlayer.loadPlayer(player));
        }


    }

    @Override
    public void onDisable(){
        BlackFile file = new BlackFile(BlackFile.BlackFilesType.STATS);
        players.stream().forEach(player ->{
            file.set(player.getUuid().toString()+".wins", player.getWins());
            file.set(player.getUuid().toString()+".losses", player.getLosses());
            file.save();

        });
    }


    public BlackjackPlayer getPlayerFor(Player p){

       //return players.stream().filter(p -> uuid.equals(p.getUuid())).collect(Collectors.toList()).get(0);
        if(!players.isEmpty()){
            for(BlackjackPlayer player : players){
                if(player.getUuid().equals(p.getUniqueId())) return player;
            }
        }
        return null;

    }
    public Set<BlackjackPlayer> getPlayers() {
        return players;
    }

    public boolean isOverflow() {
        return this.overFlow;
    }

    public static Economy getEconomy(){
        return econ;
    }
    public static Blackjack getInstance() {return instance;}

    public HashSet<BlackjackGame> getCurrentGames(){
        return this.currentGames;
    }
    public  String getString(String path){
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(path));
    }
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public void sendMessage(Player player, String pathTomessage){
        player.sendMessage(getString(pathTomessage));
    }
}
