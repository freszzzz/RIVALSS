package pl.rivals.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.rivals.config.Messages;
import pl.rivals.cosmetic.Cosmetics;
import pl.rivals.data.PlayerProfile;
import pl.rivals.hud.Hud;
import pl.rivals.item.ItemFactory;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.Cosmetic;
import pl.rivals.shared.Glyphs;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Sklep: zakładki (skórki, efekty zabójstwa, ślady doskoku), zakup z potwierdzeniem drugim kliknięciem. */
public final class ShopMenu extends Menu {

    private static final int BALANCE_SLOT = 8;
    private static final int CONFIRM_TICKS = 60;

    private final RivalsPlugin plugin;
    private Cosmetic.Type tab = Cosmetic.Type.SKIN;
    private final Map<Integer, Cosmetic> slots = new HashMap<>();
    private Cosmetic pending;
    private long pendingUntil;

    public ShopMenu(RivalsPlugin plugin, Player viewer) {
        super(viewer, Hud.guiTitle(Glyphs.GUI_SHOP, "SKLEP"));
        this.plugin = plugin;
        render();
    }

    private PlayerProfile profile() {
        return plugin.profiles().get(viewer.getUniqueId());
    }

    private void render() {
        inventory.clear();
        slots.clear();
        PlayerProfile p = profile();
        inventory.setItem(0, tab(WorldModel.UI_TAB_SKINS, Cosmetic.Type.SKIN));
        inventory.setItem(1, tab(WorldModel.UI_TAB_EFFECTS, Cosmetic.Type.KILL_EFFECT));
        inventory.setItem(2, tab(WorldModel.UI_TAB_TRAILS, Cosmetic.Type.TRAIL));
        int coins = p == null ? 0 : p.coins;
        inventory.setItem(BALANCE_SLOT, ItemFactory.icon(WorldModel.COIN.key(),
                Component.text().append(Hud.glyph(Glyphs.COIN_0)).append(Hud.text(" Monety: " + coins, Hud.GOLD)).build(),
                List.of(Hud.text("Zarabiasz je w meczach.", Hud.MUTED)), false));
        int slot = 9;
        for (Cosmetic c : Cosmetic.of(tab)) {
            if (slot > 44) break;
            inventory.setItem(slot, icon(c, p));
            slots.put(slot, c);
            slot++;
        }
    }

    private ItemStack tab(WorldModel model, Cosmetic.Type type) {
        boolean active = tab == type;
        return ItemFactory.icon(model.key(), Hud.pixel(type.label, active ? Hud.ACCENT : Hud.MUTED),
                List.of(Hud.text(active ? "Otwarta zakładka" : "Kliknij, aby przejść", Hud.MUTED)), active);
    }

    private ItemStack icon(Cosmetic c, PlayerProfile p) {
        ItemStack it = switch (c.type) {
            case SKIN -> ItemFactory.create(plugin, c.weapon, c);
            case KILL_EFFECT -> ItemFactory.icon(WorldModel.UI_TAB_EFFECTS.key(), Component.empty(), null, false);
            case TRAIL -> ItemFactory.icon(WorldModel.UI_TAB_TRAILS.key(), Component.empty(), null, false);
        };
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Hud.text(c.displayName, TextColor.color(c.color)));
        List<Component> lore = new ArrayList<>();
        lore.add(Hud.header(c.type.label));
        lore.add(Component.empty());
        boolean owned = Cosmetics.owns(p, c);
        boolean equipped = Cosmetics.equipped(p, c);
        if (equipped) {
            lore.add(Component.text().append(Hud.glyph(Glyphs.CHECK)).append(Hud.text(" Założone – kliknij, aby zdjąć", Hud.GREEN)).build());
        } else if (owned) {
            lore.add(Hud.text("Posiadane – kliknij, aby założyć", Hud.ACCENT));
        } else {
            int price = plugin.settings().price(c.id);
            lore.add(Component.text().append(Hud.text("Cena: ", Hud.MUTED)).append(Hud.glyph(Glyphs.COIN_0))
                    .append(Hud.text(" " + price, Hud.GOLD)).build());
            boolean confirm = pending == c && Bukkit.getCurrentTick() < pendingUntil;
            lore.add(Hud.text(confirm ? "Kliknij ponownie, aby kupić!" : "Kliknij, aby kupić", confirm ? Hud.GOLD : Hud.MUTED));
        }
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(equipped);
        it.setItemMeta(meta);
        return it;
    }

    @Override
    public void click(int slot, ClickType type) {
        switch (slot) {
            case 0 -> switchTab(Cosmetic.Type.SKIN);
            case 1 -> switchTab(Cosmetic.Type.KILL_EFFECT);
            case 2 -> switchTab(Cosmetic.Type.TRAIL);
            default -> {
                Cosmetic c = slots.get(slot);
                if (c != null) clickCosmetic(c);
            }
        }
    }

    private void switchTab(Cosmetic.Type t) {
        tab = t;
        pending = null;
        plugin.sounds().play(viewer, SoundId.UI_CLICK);
        render();
    }

    private void clickCosmetic(Cosmetic c) {
        PlayerProfile p = profile();
        if (p == null) return;
        if (Cosmetics.owns(p, c)) {
            Cosmetics.toggle(p, c);
            plugin.profiles().save(p);
            plugin.sounds().play(viewer, SoundId.UI_CLICK);
            render();
            return;
        }
        long now = Bukkit.getCurrentTick();
        if (pending != c || now >= pendingUntil) {
            pending = c;
            pendingUntil = now + CONFIRM_TICKS;
            plugin.sounds().play(viewer, SoundId.UI_CLICK, 0.8f);
            render();
            return;
        }
        pending = null;
        switch (Cosmetics.buy(plugin, p, c)) {
            case OK -> {
                plugin.sounds().play(viewer, SoundId.UI_BUY);
                plugin.messages().send(viewer, "shop.bought", Messages.ph("item", c.displayName),
                        Messages.ph("price", plugin.settings().price(c.id)));
            }
            case NOT_ENOUGH -> {
                plugin.sounds().play(viewer, SoundId.UI_ERROR);
                plugin.messages().send(viewer, "shop.not-enough");
            }
            default -> {
            }
        }
        render();
    }
}
