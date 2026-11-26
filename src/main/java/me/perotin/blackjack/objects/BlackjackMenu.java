package me.perotin.blackjack.objects;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class BlackjackMenu implements InventoryHolder {
    private final Inventory inventory;

    BlackjackMenu(int size, String title) {
        inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
