package pl.rivals.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.rivals.RivalsPlugin;
import pl.rivals.cosmetic.Cosmetics;
import pl.rivals.data.PlayerProfile;
import pl.rivals.hud.Hud;
import pl.rivals.item.ItemFactory;
import pl.rivals.item.Kit;
import pl.rivals.shared.Glyphs;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Wybór zestawu: broń główna (rząd 1, turkus), dodatkowa (obok, niebieski), biała (rząd 2, czerwony),
 * gadżety (rzędy 3-4, złoty). Dół: podgląd zestawu i przycisk "Gotowe".
 */
public final class KitMenu extends Menu {

    private static final int READY_SLOT = 53;

    private final RivalsPlugin plugin;
    private final Map<Integer, RivalsItem> slots = new HashMap<>();
    private final Consumer<Kit> onChange;
    private final Runnable onReady;
    private Kit kit;
    private int secondsLeft = -1;

    public KitMenu(RivalsPlugin plugin, Player viewer, Kit current, Consumer<Kit> onChange, Runnable onReady) {
        super(viewer, Hud.guiTitle(Glyphs.GUI_KIT, "WYBÓR ZESTAWU"));
        this.plugin = plugin;
        this.kit = current;
        this.onChange = onChange;
        this.onReady = onReady;
        layout(RivalsItem.Slot.PRIMARY, new int[]{0, 1, 2, 3});
        layout(RivalsItem.Slot.SECONDARY, new int[]{5, 6});
        layout(RivalsItem.Slot.MELEE, new int[]{9, 10, 11});
        layout(RivalsItem.Slot.GADGET, new int[]{18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33});
        render();
    }

    private void layout(RivalsItem.Slot slot, int[] positions) {
        List<RivalsItem> items = RivalsItem.of(slot);
        for (int i = 0; i < items.size() && i < positions.length; i++) slots.put(positions[i], items.get(i));
    }

    public Kit kit() {
        return kit;
    }

    public void secondsLeft(int s) {
        if (s == secondsLeft) return;
        secondsLeft = s;
        inventory.setItem(READY_SLOT, readyItem());
    }

    private void render() {
        PlayerProfile prof = plugin.profiles().get(viewer.getUniqueId());
        for (Map.Entry<Integer, RivalsItem> e : slots.entrySet()) {
            RivalsItem item = e.getValue();
            boolean selected = kit.get(item.slot) == item;
            ItemStack it = ItemFactory.create(plugin, item, Cosmetics.skinFor(prof, item));
            ItemMeta meta = it.getItemMeta();
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(selected
                    ? Component.text().append(Hud.glyph(Glyphs.CHECK)).append(Hud.text(" WYBRANE", Hud.GREEN)).build()
                    : Hud.text("Kliknij, aby wybrać", Hud.MUTED));
            meta.lore(lore);
            meta.setEnchantmentGlintOverride(selected);
            it.setItemMeta(meta);
            inventory.setItem(e.getKey(), it);
        }
        RivalsItem[] chosen = kit.items();
        for (int i = 0; i < 4; i++) {
            inventory.setItem(45 + i, ItemFactory.create(plugin, chosen[i], Cosmetics.skinFor(prof, chosen[i])));
        }
        inventory.setItem(READY_SLOT, readyItem());
    }

    private ItemStack readyItem() {
        String name = secondsLeft >= 0 ? "Gotowe · " + secondsLeft + " s" : "Gotowe";
        return ItemFactory.icon(WorldModel.UI_READY.key(), Hud.pixel(name, Hud.GREEN),
                List.of(Hud.text("Zatwierdź zestaw i zamknij okno.", Hud.MUTED)), false);
    }

    @Override
    public void click(int slot, ClickType type) {
        if (slot == READY_SLOT) {
            plugin.sounds().play(viewer, SoundId.UI_CLICK, 1.3f);
            viewer.closeInventory();
            if (onReady != null) onReady.run();
            return;
        }
        RivalsItem item = slots.get(slot);
        if (item == null) return;
        kit = kit.with(item);
        if (onChange != null) onChange.accept(kit);
        plugin.sounds().play(viewer, SoundId.UI_CLICK);
        render();
    }
}
