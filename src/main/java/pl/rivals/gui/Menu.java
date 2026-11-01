package pl.rivals.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Bazowe menu (skrzynia 6 rzędów) z tłem z resource packa w tytule. */
public abstract class Menu implements InventoryHolder {

    protected final Inventory inventory;
    protected final Player viewer;

    protected Menu(Player viewer, Component title) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    public abstract void click(int slot, ClickType type);

    public void onClose() {
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
