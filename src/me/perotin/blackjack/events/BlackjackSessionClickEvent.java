package me.perotin.blackjack.events;

import me.perotin.blackjack.Blackjack;
import me.perotin.blackjack.objects.GameSession;
import me.perotin.blackjack.util.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

/* Created by Perotin on 7/3/19 */
public class BlackjackSessionClickEvent implements Listener {

    private Blackjack plugin;

    public BlackjackSessionClickEvent(Blackjack blackjack) {
        this.plugin = blackjack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory menu = event.getClickedInventory();
        if (event.getWhoClicked() instanceof Player) {
            Player clicker = (Player) event.getWhoClicked();
            if (plugin.getSessionFor(clicker.getUniqueId()) != null) {
                GameSession session = plugin.getSessionFor(clicker.getUniqueId());
                if (event.getView().getTitle().equals("Continue playing, " + clicker.getName() + "?")) {
                    // new game-- not the best to hardcode the strings like this but oh well... doing it for now.
                    event.setCancelled(true);
                    double betMax = plugin.getBetMax();
                    double betMin = plugin.getBetMin();
                    ItemStack clicked = event.getCurrentItem();
                    if(clicked != null && clicked.hasItemMeta()) {
                        if (clicked.getType() == XMaterial.REDSTONE.parseItem().getType()) {
                            session.endSession();
                            return;
                        } else if (clicked.getType() == XMaterial.EMERALD.parseItem().getType()) {
                            session.startNewGame();
                        } else if (clicked.getType() == XMaterial.DANDELION.parseMaterial()) {
                            String changeAmount = event.getCurrentItem().getItemMeta().getDisplayName();
                            changeAmount = ChatColor.stripColor(changeAmount);
                            changeAmount = changeAmount.charAt(0) == '+' ? ChatColor.stripColor(changeAmount.substring(1)) : ChatColor.stripColor(changeAmount);
                            double change = Double.parseDouble(changeAmount);
                            if(session.getBetAmount() + change > Blackjack.getEconomy().getBalance(clicker)) return;
                                if(session.getBetAmount() + change < 1) return;
                            if(betMax > 0 && session.getBetAmount() + change > betMax) return;

                            if(betMin > 0 &&session.getBetAmount()+change < betMin) return;

                            session.setBetAmount(session.getBetAmount() + change);
                            session.showEndMenu(session.getGames().get(session.getGames().size()-1));
                        }

                    }
                }
            }
        }
    }


}
