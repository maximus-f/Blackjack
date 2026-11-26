package me.perotin.blackjack;

import me.perotin.blackjack.commands.BlackjackAdminCommand;
import me.perotin.blackjack.commands.BlackjackCommand;
import me.perotin.blackjack.events.BlackjackInventoryClickEvent;
import me.perotin.blackjack.events.BlackjackJoinEvent;
import me.perotin.blackjack.events.BlackjackLeaveMidGameEvent;
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
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import static me.perotin.blackjack.objects.BlackFile.BlackFilesType.STATS;

/* Created by Perotin on 12/24/18 */
public class Blackjack extends JavaPlugin {

    private static Blackjack instance;
    private HashSet<GameSession> sessions;
    private static Economy econ = null;
    private boolean overFlow;
    /** @apiNote true if cash, false if exp **/
    private boolean cash;
    private final Map<UUID, BlackjackPlayer> players = new HashMap<>();
    private boolean initialized;
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
        instance = this;
        this.serverWins = 0;
        this.sessions = new HashSet<>();
        this.games = 0;
        this.serverLosses = 0;
        this.cash = getConfig().getBoolean("cash-or-xp");
        this.surrender = getConfig().getBoolean("enable-surrender");
        this.doubleDown = getConfig().getBoolean("enable-double-down");
        this.doubleDownOverFlow = getConfig().getBoolean("double-down-overflow");

        this.secondaryBetOverride = getConfig().getBoolean("enable-secondary-bet-override", false);

        this.surrenderPercentage = getConfig().getDouble("surrender-percentage-to-take");
        if(getConfig().getBoolean("enable-multiplier")){
            this.blackJackMultiplier = getConfig().getDouble("multiplier");
        } else this.blackJackMultiplier = 0;
        if (cash && !setupEconomy()) {
            // Other Vault providers may register later in the same server startup.
            Bukkit.getScheduler().runTask(this, () -> {
                if (setupEconomy()) enableGame();
                else {
                    getLogger().severe("Cash betting requires Vault and a registered economy provider.");
                    getServer().getPluginManager().disablePlugin(this);
                }
            });
            return;
        }
        enableGame();
    }

    private void enableGame() {
        if (!Double.isFinite(surrenderPercentage) || surrenderPercentage < 0 || surrenderPercentage > 100
                || !Double.isFinite(blackJackMultiplier) || blackJackMultiplier < 0) {
            getLogger().severe("Invalid surrender percentage or blackjack multiplier in config.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        taxPercent = getConfig().getDouble("tax-percent");
        if (!Double.isFinite(taxPercent) || taxPercent < 0 || taxPercent > 100) {
            getLogger().severe("tax-percent must be between 0 and 100.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        new Metrics(this, 4614);
        new UpdateChecker(this).checkForUpdate();
        this.betMax = getConfig().getDouble("bet-max");
        this.betMin = getConfig().getDouble("bet-min");
        setServerImpact(new BlackFile(STATS).getConfiguration().getDouble("server-impact"));

        saveDefaultConfig();
        getCommand("blackjack").setExecutor(new BlackjackCommand(this));
        getCommand("blackjackadmin").setExecutor(new BlackjackAdminCommand(this));
        Bukkit.getPluginManager().registerEvents(new BlackjackInventoryClickEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new BlackjackJoinEvent(this), this);
        Bukkit.getPluginManager().registerEvents(new BlackjackLeaveMidGameEvent(this), this);

        this.overFlow = getConfig().getBoolean("bet-overflow");
        BlackFile.loadFiles();

        initialized = true;
        for(Player player : Bukkit.getOnlinePlayers()){
            getPlayerFor(player);
        }

    }

    public boolean validStake(double amount) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        if (!Double.isFinite(amount * (1 + Math.max(1, blackJackMultiplier)))) return false;
        return cash || (amount == Math.floor(amount) && amount <= Integer.MAX_VALUE);
    }

    public boolean withdraw(double amount, Player player) {
        if (!validStake(amount)) return false;
        if (amount == 0) return true;
        if (cash) return econ.withdrawPlayer(player, amount).transactionSuccess();
        if (amount > player.getLevel()) return false;
        player.setLevel(player.getLevel() - (int) amount);
        return true;
    }

    public boolean deposit(double amount, Player player) {
        if (!Double.isFinite(amount) || amount < 0) return false;
        if (amount == 0) return true;
        if (cash) return econ.depositPlayer(player, amount).transactionSuccess();
        if (amount > Integer.MAX_VALUE - player.getLevel()) return false;
        player.setLevel(player.getLevel() + (int) amount);
        return true;
    }

    public void recordOutcome(Player player, BlackjackGame.Ending outcome, double net) {
        serverImpact -= net;
        games++;
        switch (outcome) {
            case WIN:
                getPlayerFor(player).addWin();
                serverLosses++;
                break;
            case SURRENDER:
            case LOSE:
                getPlayerFor(player).addLoss();
                serverWins++;
                break;
            case TIE:
                break;
        }
    }

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
        if (!initialized) return;
        initialized = false;
        for (GameSession session : new HashSet<>(sessions)) {
            session.forfeitAndClose();
        }
        BlackFile file = new BlackFile(STATS);

        players.values().forEach(player ->{
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
        econ = null;

    }

    public double getTaxPercent(){
        return this.taxPercent;
    }


    public BlackjackPlayer getPlayerFor(Player p){

        return players.computeIfAbsent(p.getUniqueId(), uuid -> BlackjackPlayer.loadPlayer(p));
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
        if (!getConfig().contains(path)) {
            Bukkit.getLogger().info("[Blackjack] Could not find config path!: " + path);
            return "";
        }
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
