package me.perotin.blackjack.objects;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.util.ItemBuilder;
import me.perotin.blackjack.util.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/* Created by Perotin on 6/28/19 */
public class GameSession {

    private UUID uuid;
    private List<BlackjackGame> games;
    private double earnings;


    public GameSession(UUID uuid, BlackjackGame game) {
        this.uuid = uuid;
        this.earnings = 0;
        this.games = new ArrayList<>();
        games.add(game);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public List<BlackjackGame> getGames() {
        return games;
    }

    public void addGame(List<BlackjackGame> games) {
        this.games.addAll(games);
    }

    public void showEndMenu(BlackjackGame game){
        Blackjack plugin = Blackjack.getInstance();
        Inventory menu = Bukkit.createInventory(null, 54, "Continue playing?");
        ItemBuilder gameEarnings = new ItemBuilder(XMaterial.ENCHANTED_BOOK.parseItem());
        gameEarnings.name(plugin.getString("earnings-gui")
        .replace("$amount$", game.getResult()+""));
        menu.setItem(32, gameEarnings.build());

        ItemBuilder keepPlaying = new ItemBuilder(XMaterial.EMERALD.parseItem());
        keepPlaying.name(plugin.getString("keep-playing"));
        menu.setItem(33, keepPlaying.build());

        ItemBuilder stopPlaying = new ItemBuilder(XMaterial.REDSTONE.parseItem());
        keepPlaying.name(plugin.getString("stop-playing"));
        menu.setItem(31, stopPlaying.build());

        ItemBuilder totalEarnings = new ItemBuilder(XMaterial.PAPER.parseItem());
        keepPlaying.name(plugin.getString("total-earnings")
        .replace("$amount$", earnings+""));
        menu.setItem(41, totalEarnings.build());
    }

}
