package me.perotin.blackjack.objects;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.util.ItemBuilder;
import me.perotin.blackjack.util.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/* Created by Perotin on 12/24/18 */
public class BlackjackGame {

    private final Player player;
    private double betAmount;
    private ArrayList<String> playerCards;
    private ArrayList<String> houseCards;
    // true if player is next, false if house is next
    private boolean playerTurn;
    private ArrayList<String> cardsAvailible;
    private RoundResult result;
    private boolean doubled;
    private final UUID uuid;

    public BlackjackGame(Player player, double betAmount) {
        this.player = player;
        this.betAmount = betAmount;
        this.playerTurn = true;
        this.cardsAvailible = new ArrayList<>(Arrays.asList(Blackjack.cards));
        this.uuid = UUID.randomUUID();
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

    /**
     * Gets the next availible card
     */

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

    public RoundResult getSettlement() {
        return result;
    }

    public RoundResult finish(Ending outcome, double multiplier, double tax, double surrender, boolean cash) {
        if (result != null) return null;
        boolean natural = playerCards.size() == 2 && getScoreUnder21(playerCards) == 21;
        result = new RoundResult(outcome, betAmount, natural, multiplier, tax, surrender, cash);
        return result;
    }

    public boolean isDoubled() {
        return doubled;
    }

    public void doubleStake() {
        doubled = true;
        betAmount *= 2;
    }

    public Ending playDealer() {
        setPlayerTurn(false);
        while (getScoreUnder21(houseCards) < 17) {
            getNextCard();
        }
        int dealer = getScoreUnder21(houseCards);
        int playerScore = getScoreUnder21(playerCards);
        if (dealer > 21 || playerScore > dealer) return Ending.WIN;
        if (playerScore == dealer) return Ending.TIE;
        return Ending.LOSE;
    }

    public UUID getUuid() {
        return uuid;
    }

    public ArrayList<String> getPlayerCards() {
        return playerCards;
    }

    public ArrayList<String> getHouseCards() {
        return houseCards;
    }

    /**
     *
     * @param cards
     * @return most optimal score under 21 for the cards
     */
    public int getScoreUnder21(List<String> cards) {
        int score = 0;
        boolean hasAce = false;
        for (String card : cards) {
            if (card.startsWith("A")) {
                hasAce = true;
                score++;
            } else {
                score += valueOfCard(card);
            }
        }
        return hasAce && score <= 11 ? score + 10 : score;
    }


    public void fillInventory(Inventory inventory) {
        Blackjack plugin = Blackjack.getInstance();
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


        if(!doubled) {
            ItemBuilder hit = new ItemBuilder(XMaterial.MAP.parseItem());
            hit.name(Blackjack.getInstance().getString("hit-item"));
            inventory.setItem(22, hit.build());
        }

        ItemBuilder stand = new ItemBuilder(XMaterial.BARRIER.parseItem());
        stand.name(Blackjack.getInstance().getString("stand-item"));
        inventory.setItem(31, stand.build());

        if(plugin.isSurrenderEnabled() && !doubled) {
            ItemBuilder surrender = new ItemBuilder(XMaterial.OAK_DOOR.parseItem());
            surrender.name(Blackjack.getInstance().getString("surrender-item"));
            surrender.lore(Blackjack.getInstance().getString("surrender-lore"));
            inventory.setItem(32, surrender.build());
        }
        if(plugin.isDoubleDownEnabled() && playerCards.size() == 2) {
            if (plugin.getPlayerFor(player).getBalance() >= betAmount  || plugin.isDoubleDownOverFlow()){
                ItemBuilder doubleDown = new ItemBuilder(XMaterial.NETHER_STAR.parseItem());
                doubleDown.name(Blackjack.getInstance().getString("double-down-item"));
                doubleDown.lore(Blackjack.getInstance().getString("double-down-lore"));
                inventory.setItem(30, doubleDown.build());
            }
        }

        int invSlot = 38;
        for (String card : playerCards) {
            inventory.setItem(invSlot, cardAsItemStack(card, true));
            invSlot++;
        }


        int dealersSlot = 11;
        if (isPlayerTurn()) {
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

    public void setBetAmount(double betAmount) {
        this.betAmount = betAmount;
    }

    public void setCardsAvailible(ArrayList<String> cardsAvailible) {
        this.cardsAvailible = cardsAvailible;
    }

    public static String convertToFullText(String card) {
        Blackjack plugin = Blackjack.getInstance();
        String identifier = "";
        if (card.length() == 2) {
            identifier = card.substring(0, 1);
        } else if (card.length() == 3) {
            identifier = card.substring(0, 2);
        }

        String suit = card.substring(card.length() - 1);
        switch (suit) {
            case "s":
                suit = plugin.getString("spades");
                break;
            case "d":
                suit = plugin.getString("diamonds");;
                break;
            case "h":
                suit = plugin.getString("hearts");;
                break;
            case "c":
                suit = plugin.getString("clubs");;
        }
        switch (identifier) {
            case "J":
                identifier = plugin.getString("jack");
                break;
            case "Q":
                identifier = plugin.getString("queen");
                break;
            case "K":
                identifier = plugin.getString("king");
                break;
            case "A":
                identifier = plugin.getString("ace");
        }

        return identifier + " of " + suit;
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
        WIN, LOSE, TIE, SURRENDER
    }
}
