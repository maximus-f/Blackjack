package me.perotin.blackjack.objects;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.util.ItemBuilder;
import me.perotin.blackjack.util.XMaterial;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;


import java.util.*;

/* Created by Perotin on 12/24/18 */
public class BlackjackGame {

    private final Player player;
    private final double betAmount;
    private ArrayList<String> playerCards;
    private ArrayList<String> houseCards;
    // true if player is next, false if house is next
    private boolean playerTurn;
    private ArrayList<String> cardsAvailible;

    public BlackjackGame(Player player, double betAmount) {
        this.player = player;
        this.betAmount = betAmount;
        this.playerTurn = true;
        this.cardsAvailible = new ArrayList<>(Arrays.asList(Blackjack.cards));

        this.playerCards = new ArrayList<>();
        this.houseCards = new ArrayList<>();
        getNextCard();
        getNextCard();
        setPlayerTurn(false);
        getNextCard();
        getNextCard();
        setPlayerTurn(true);
//        this.playerScore = getNextCard();
//        this.houseScore = houseScore;
    }

    public void getNextCard() {
        int random = new Random().nextInt(cardsAvailible.size());
        String card = cardsAvailible.get(random);
        if (isPlayerTurn()) {
            playerCards.add(card);
        } else {
            houseCards.add(card);
        }
        cardsAvailible.remove(random);

    }

    public void updateInventoryForHouse() {
        player.openInventory(getInventory(false));
    }

    public ArrayList<String> getPlayerCards() {
        return playerCards;
    }

    public ArrayList<String> getHouseCards() {
        return houseCards;
    }

    public int getScoreUnder21(List<String> cards) {
        List<String> cardsToRemoveAndAdd = new ArrayList<>();

        for (String s : cards) {
            if (s.startsWith("A")) {
                cardsToRemoveAndAdd.add(s);
            }
        }
        cards.removeAll(cardsToRemoveAndAdd);
        cards.addAll(cardsToRemoveAndAdd);

        int score = 0;
        for (String s : cards) {
            if (!s.startsWith("A")) {
                score += valueOfCard(s);
            } else {
                if (score + 11 <= 21) {
                    score += 11;
                } else if (score + 11 > 21) {
                    score += 1;
                }
            }
        }
        return score;

    }


    public Inventory getInventory(boolean hideHouse) {
        Blackjack plugin = Blackjack.getInstance();
        Inventory inventory = Bukkit.createInventory(null, 54, Blackjack.getInstance().getString("menu-title").replace("$number$", "" + betAmount));
        ItemBuilder tutorial = new ItemBuilder(XMaterial.PAPER.parseItem());
        tutorial.name(plugin.getString("tutorial-name"));
        inventory.setItem(0, tutorial.build());
        ItemBuilder dealer = new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem());
        dealer.name(Blackjack.getInstance().getString("blackjack-dealer"));
        dealer.lore(Blackjack.getInstance().getString("dealer-lore"));
        inventory.setItem(4, dealer.build());


        ItemBuilder sign = new ItemBuilder(XMaterial.OAK_SIGN.parseMaterial());
        sign.name(plugin.getString("dealer-cards"));
        inventory.setItem(9, sign.build());
        sign.name(plugin.getString("player-cards") + " " + getScoreUnder21(playerCards));
        inventory.setItem(36, sign.build());


        ItemBuilder hit = new ItemBuilder(XMaterial.MAP.parseItem());
        hit.name(Blackjack.getInstance().getString("hit-item"));
        inventory.setItem(22, hit.build());

        ItemBuilder stand = new ItemBuilder(XMaterial.BARRIER.parseItem());
        stand.name(Blackjack.getInstance().getString("stand-item"));
        inventory.setItem(31, stand.build());

        int invSlot = 38;
        for (String card : playerCards) {
            inventory.setItem(invSlot, cardAsItemStack(card, true));
            invSlot++;
        }


        int dealersSlot = 11;
        if (hideHouse) {
            for (String card : houseCards) {
                if (dealersSlot == 11) {
                    inventory.setItem(dealersSlot, cardAsItemStack(card, true));
                    dealersSlot++;
                } else {
                    inventory.setItem(dealersSlot, cardAsItemStack(card, false));
                    dealersSlot++;
                }
            }
        } else {
            for (String card : houseCards) {
                inventory.setItem(dealersSlot, cardAsItemStack(card, true));
                dealersSlot++;
            }
        }

        return inventory;

    }

    private ItemStack cardAsItemStack(String card, boolean show) {
        if (show) {
            ItemBuilder builder = new ItemBuilder(XMaterial.GREEN_STAINED_GLASS_PANE.parseItem());
            builder.name(ChatColor.YELLOW + convertToFullText(card));
            return builder.build();
        } else {
            ItemBuilder build = new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE.parseItem());
            build.name(Blackjack.getInstance().getString("unknown-card"));
            return build.build();
        }
    }

    public Player getPlayer() {
        return player;
    }


    public double getBetAmount() {
        return betAmount;
    }


    public boolean isPlayerTurn() {
        return playerTurn;
    }

    public void setPlayerTurn(boolean playerTurn) {
        this.playerTurn = playerTurn;
    }

    public ArrayList<String> getCardsAvailible() {
        return cardsAvailible;
    }

    public void setCardsAvailible(ArrayList<String> cardsAvailible) {
        this.cardsAvailible = cardsAvailible;
    }

    public static String convertToFullText(String card) {
        String identifier = "";
        if (card.length() == 2) {
            identifier = card.substring(0, 1);
        } else if (card.length() == 3) {
            identifier = card.substring(0, 2);
        }

        String suit = card.substring(card.length() - 1);
        switch (suit) {
            case "s":
                suit = "Spades";
                break;
            case "d":
                suit = "Diamonds";
                break;
            case "h":
                suit = "Hearts";
                break;
            case "c":
                suit = "Clubs";
        }
        switch (identifier) {
            case "J":
                identifier = "Jack";
                break;
            case "Q":
                identifier = "Queen";
                break;
            case "K":
                identifier = "King";
                break;
            case "A":
                identifier = "Ace";
        }

        return identifier + " of " + suit;
    }


    public void endGame(Ending end) {
        player.closeInventory();
        Blackjack plugin = Blackjack.getInstance();
        player.sendMessage(plugin.getString("end-game")
                .replace("$score$", getScoreUnder21(getPlayerCards()) + "")
                .replace("$score2$", getScoreUnder21(getHouseCards()) + ""));
        if (end == Ending.WIN) {

            EconomyResponse er;
            boolean taxxed = false;
            double earnings = 0;
            if(plugin.getTaxPercent() != 0.0 && plugin.getTaxPercent() <= 100.0) {
                double tax = plugin.getTaxPercent() / 100.0;
                double postTax = betAmount - (tax * betAmount);
                earnings = postTax;
                 er = Blackjack.getEconomy().depositPlayer(player, postTax);
                 taxxed = true;
            } else {
                earnings = betAmount;
                 er = Blackjack.getEconomy().depositPlayer(player, betAmount);

            }


            player.sendMessage(plugin.getString("earnings")
                    .replace("$result$", plugin.getString("won"))
                    .replace("$number$", earnings + ""));
            if (er.transactionSuccess()) {
                if(taxxed) {
                    double tax = plugin.getTaxPercent() / 100.0;
                    double postTax = tax * betAmount;
                    player.sendMessage(plugin.getString("taxxed").replace("$amount$", postTax+""));
                } else {
                    player.sendMessage(ChatColor.GREEN + "+" + betAmount);

                }
            }
        } else if (end == Ending.LOSE) {
            EconomyResponse er = Blackjack.getEconomy().withdrawPlayer(player, betAmount);

            player.sendMessage(plugin.getString("earnings")
                    .replace("$result$", plugin.getString("lost"))
                    .replace("$number$", getBetAmount() + ""));

            if (er.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "-" + betAmount);
            }

        } else {
            // tie
            player.sendMessage(plugin.getString("tied"));
        }
        // add or remove coins with vault
        // ask if they want to play another game perhaps, maybe implement sessions as a thing

        Blackjack.getInstance().getCurrentGames().remove(this);

    }

    private int valueOfCard(String card) {
        // make sure this is right and its not 1 because starting at 0
        if (card.length() == 2) {
            card = card.substring(0, 1);
            switch (card) {
                case "K":
                    return 10;
                case "J":
                    return 10;
                case "Q":
                    return 10;
                case "A":
                    return 0;
                default:
                    return Integer.parseInt(card);
            }
        } else if (card.length() == 3) {
            return 10;
        } else {
            return 0;
        }
    }

    public enum Ending {
        WIN, LOSE, TIE
    }
}
