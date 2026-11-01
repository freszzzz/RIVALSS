package pl.rivals.lobby;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import pl.rivals.data.PlayerProfile;
import pl.rivals.gui.KitMenu;
import pl.rivals.gui.ShopMenu;
import pl.rivals.hud.Hud;
import pl.rivals.item.ItemFactory;
import pl.rivals.item.Kit;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.WorldModel;

import java.util.List;

/** Przedmioty w pasku lobby: graj, zestaw, sklep, profil. */
public final class LobbyItems implements Listener {

    private final RivalsPlugin plugin;
    private final NamespacedKey key;

    public LobbyItems(RivalsPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "lobby_item");
    }

    private ItemStack item(WorldModel model, String id, String name, String desc) {
        ItemStack it = ItemFactory.icon(model.key(), Hud.pixel(name, Hud.ACCENT), List.of(Hud.text(desc, Hud.MUTED)), false);
        ItemMeta meta = it.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        it.setItemMeta(meta);
        return it;
    }

    public void give(Player p) {
        if (!plugin.settings().lobbyItems) return;
        p.getInventory().setItem(0, item(WorldModel.UI_PLAY, "play", "Graj", "PPM – dołącz do kolejki 1v1"));
        p.getInventory().setItem(1, item(WorldModel.UI_KIT, "kit", "Zestaw", "PPM – wybierz bronie i gadżet"));
        p.getInventory().setItem(7, item(WorldModel.UI_SHOP, "shop", "Sklep", "PPM – skórki, efekty, ślady"));
        p.getInventory().setItem(8, item(WorldModel.UI_PROFILE, "profile", "Profil", "PPM – statystyki i ranga"));
        p.getInventory().setHeldItemSlot(0);
    }

    private String id(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        String id = id(e.getItem());
        if (id == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (plugin.matches().ofPlayer(p.getUniqueId()) != null) return;
        switch (id) {
            case "play" -> {
                if (plugin.queue().isQueued(p.getUniqueId())) plugin.queue().leave(p, false);
                else plugin.queue().join(p);
            }
            case "kit" -> openKit(p);
            case "shop" -> new ShopMenu(plugin, p).open();
            case "profile" -> p.performCommand("rivals statystyki");
            default -> {
            }
        }
    }

    /** Zestaw ustawiany w lobby (zapamiętany na kolejne mecze). */
    public void openKit(Player p) {
        PlayerProfile prof = plugin.profiles().get(p.getUniqueId());
        if (prof == null) {
            plugin.messages().send(p, "general.profile-loading");
            return;
        }
        Kit kit = Kit.parse(prof.kit, Kit.parse(plugin.settings().defaultKit, Kit.FALLBACK));
        new KitMenu(plugin, p, kit, k -> {
            prof.kit = k.serialize();
            plugin.profiles().save(prof);
        }, () -> plugin.messages().send(p, "kit.saved")).open();
    }

    private boolean locked(Player p) {
        return plugin.settings().lobbyItems && plugin.matches().ofPlayer(p.getUniqueId()) == null
                && p.getGameMode() != GameMode.CREATIVE;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && locked(p) && (id(e.getCurrentItem()) != null || id(e.getCursor()) != null)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (locked(e.getPlayer()) && id(e.getItemDrop().getItemStack()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }
}
