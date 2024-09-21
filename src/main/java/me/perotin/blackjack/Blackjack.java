package me.perotin.blackjack;

import me.perotin.blackjack.commands.BlackjackAdminCommand;
import me.perotin.blackjack.commands.BlackjackCommand;
import me.perotin.blackjack.events.BlackjackInventoryClickEvent;
import me.perotin.blackjack.events.BlackjackJoinEvent;
import me.perotin.blackjack.events.BlackjackLeaveMidGameEvent;
import me.perotin.blackjack.events.BlackjackSessionClickEvent;
import me.perotin.blackjack.objects.BlackFile;
import me.perotin.blackjack.objects.BlackjackGame;
import me.perotin.blackjack.objects.BlackjackPlayer;
import me.perotin.blackjack.objects.GameSession;
import me.perotin.blackjack.util.Metrics;
import me.perotin.blackjack.util.UpdateChecker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static me.perotin.blackjack.objects.BlackFile.BlackFilesType.STATS;

/* Created by Perotin on 12/24/18 */
public class Blackjack extends JavaPlugin {

    /*
    TODO
    new to config.yml

# Should doubling down be enabled (double your bet amount but forced to stand after drawing 1 more card)
enable-double-down: true

# If double down is enabled, and a person doubles down without enough funds to cover the cost of a loss,
# should they still be rewarded the 2x multiplier?
# E.g. player A bets all $100 they own, they then double down making the bet $200, they then lose and hence lose
# $200. However, a lot of economies do not use negative balances so they would really only lose $100.
# I'd recommend keeping this false unless your economy allows for players to have negative balances
double-down-overflow: false

double-down-item: "&eDouble Down"
double-down-lore: "&7&o(Double the bet amount and stand after 1 more card)"
     */

    private static Blackjack instance;
   // private HashSet<BlackjackGame> currentGames;
    private HashSet<GameSession> sessions;
    private static Economy econ = null;
    private boolean overFlow;
    /** @apiNote true if cash, false if exp **/
    private boolean cash;
    //private boolean forceForfeit;
    private Set<BlackjackPlayer> players;
    private double taxPercent, betMin, betMax = 0;
    // stats for admins
    private double serverImpact;
    private double serverWins;
    private double games;
    private double serverLosses;
    private boolean surrender, doubleDown, doubleDownOverFlow, secondaryBetOverride;
    private double surrenderPercentage;
    private double blackJackMultiplier;


    public static String[] cards = {
            "As",  "2s", "3s", "4s", "5s", "6s", "7s", "8s", "9s", "10s", "Js", "Qs", "Ks",
            "Ac",  "2c", "3c", "4c", "5c", "6c", "7c", "8c", "9c", "10c", "Jc", "Qc", "Kc",
            "Ah",  "2h", "3h", "4h", "5h", "6h", "7h", "8h", "9h", "10h", "Jh", "Qh", "Kh",
            "Ad",  "2d", "3d", "4d", "5d", "6d", "7d", "8d", "9d", "10d", "Jd", "Qd", "Kd"};
    @Override
    public void onEnable(){
        Metrics metrics = new Metrics(this, 4614);
        this.serverWins = 0;
        this.sessions = new HashSet<>();
        this.games = 0;
        this.serverLosses = 0;
        this.cash = getConfig().getBoolean("cash-or-xp");
        //this.forceForfeit = getConfig().getBoolean("force-forfeit");



        this.surrender = getConfig().getBoolean("enable-surrender");
        this.doubleDown = getConfig().getBoolean("enable-double-down");
        this.doubleDownOverFlow = getConfig().getBoolean("double-down-overflow");

        this.secondaryBetOverride = getConfig().getBoolean("enable-secondary-bet-override", false);

        this.surrenderPercentage = getConfig().getDouble("surrender-percentage-to-take");
        if(getConfig().getBoolean("enable-multiplier")){
            this.blackJackMultiplier = getConfig().getDouble("multiplier");
        } else this.blackJackMultiplier = 0;
        new UpdateChecker(this).checkForUpdate();
        if(isUsingCash()) {
            setupEconomy();
        }
        //currentGames = new HashSet<>();
        players = new HashSet<>();
        instance = this;
        this.taxPercent = getConfig().getDouble("tax-percent");
        this.betMax = getConfig().getDouble("bet-max");
        this.betMin = getConfig().getDouble("bet-min");
        setServerImpact(new BlackFile(STATS).getConfiguration().getDouble("server-impact"));

        saveDefaultConfig();
        // custom command string, remember that kangarko stuff ye
        getCommand("blackjack").setExecutor(new BlackjackCommand(this));
        getCommand("blackjackadmin").setExecutor(new BlackjackAdminCommand(this));
        Bukkit.getPluginManager().registerEvents(new BlackjackInventoryClickEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new BlackjackJoinEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new BlackjackSessionClickEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new BlackjackLeaveMidGameEvent(this), this);

//        if(false){
//            Bukkit.getPluginManager().registerEvents(new BlackjackLeaveMidGameEvent(this), this);
//        }

        this.overFlow = getConfig().getBoolean("bet-overflow");
        BlackFile.loadFiles();

        for(Player player : Bukkit.getOnlinePlayers()){
            players.add(BlackjackPlayer.loadPlayer(player));
        }

    }

    public static void withdraw(double amount, Player player){
        if(Blackjack.getInstance().isUsingCash()){
           Blackjack.getEconomy().withdrawPlayer(player, amount);

        } else {
            // using exp

            player.setLevel(player.getLevel() - (int) amount);
        }
    }

    public static void deposit(double amount, Player player){
        if(Blackjack.getInstance().isUsingCash()){
         Blackjack.getEconomy().depositPlayer(player, amount);

        } else {
            // using exp
            player.setLevel(player.getLevel() + (int) amount);
        }
    }

//
//    public boolean isForceForfeit() {
//        return forceForfeit;
//    }

    public double getBlackJackMultiplier() {
        return blackJackMultiplier;
    }

    /**
     * @return true if using cash, false is using exp
     */
    public  boolean isUsingCash(){
        return cash;
    }

    public HashSet<GameSession> getSessions() {
        return sessions;
    }

    public boolean isSurrenderEnabled() {
        return surrender;
    }

    public boolean isDoubleDownEnabled() {
        return doubleDown;
    }


    public double getSurrenderPercentage() {
        return surrenderPercentage;
    }


    public double getServerLosses() {
        return serverLosses;
    }

    public double getTotalServerLosses() {
        return new BlackFile(STATS).getConfiguration().getInt("server-losses") + getServerLosses();
    }

    public boolean isSecondaryBetOverride() {
        return secondaryBetOverride;
    }

    public void increaseServerLosses() {
        this.serverLosses++;
    }

    public void increaseGamesPlayed(){
        this.games++;
    }

    public double getGames() {
        return games;
    }

    public double getTotalServerGames() {
        return new BlackFile(STATS).getConfiguration().getInt("server-games") + getGames();
    }


    public double getServerWins() {
        return serverWins;
    }
    public double getTotalServerWins() {
        return new BlackFile(STATS).getConfiguration().getInt("server-wins") + getServerWins();
    }

    public void increaseServerWins() {
        this.serverWins++;
    }

    public void setServerImpact(double serverImpact) { this.serverImpact = serverImpact; }

    public double getServerImpact() {
        return this.serverImpact;
    }

    public double getBetMin() {
        return betMin;
    }

    public double getBetMax() {
        return betMax;
    }

    @Override
    public void onDisable(){
        BlackFile file = new BlackFile(STATS);
        sessions.forEach(GameSession::endSession);

        players.stream().forEach(player ->{
            file.set(player.getUuid().toString()+".wins", player.getWins());
            file.set(player.getUuid().toString()+".losses", player.getLosses());
        });
        players.clear();
        sessions.clear();


        file.set("server-impact", serverImpact);
        file.set("server-games", getTotalServerGames());
        file.set("server-wins", getTotalServerWins());
        file.set("server-losses", getTotalServerLosses());
        file.save();

    }

    public double getTaxPercent(){
        return this.taxPercent;
    }


    public BlackjackPlayer getPlayerFor(Player p){

       //return players.stream().filter(p -> uuid.equals(p.getUuid())).collect(Collectors.toList()).get(0);
        if(!players.isEmpty()){
            for(BlackjackPlayer player : players){
                if(player.getUuid().equals(p.getUniqueId())) return player;
            }
        }
        return BlackjackPlayer.loadPlayer(p);

    }

    public GameSession getSessionFor(BlackjackGame game){
        for(GameSession s : getSessions()){
            for(BlackjackGame g : s.getGames()){
                if(g.getUuid().equals(game.getUuid())){
                    return s;
                }

            }
        }
        return null;
    }

    public GameSession getSessionFor(UUID uuid){
        for(GameSession game : getSessions()){
            if(game.getUuid().equals(uuid)) return game;
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


    public boolean isDoubleDownOverFlow() {
        return doubleDownOverFlow;
    }

    public  String getString(String path, String alternative){
        return  getConfig().getString(path) != null ? ChatColor.translateAlternateColorCodes('&', getConfig().getString(path)) : alternative;
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
