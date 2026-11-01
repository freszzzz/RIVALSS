package pl.rivals.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import pl.rivals.config.RivalsConfig;
import pl.rivals.hud.Hud;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.Cosmetic;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.WorldModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tworzy przedmioty RIVALS: materiał bazowy + komponent item_model (assets/rivals/items),
 * lore w stylu UI, grupa cooldownu (wariant modelu przy cooldownie) i znacznik PDC.
 */
public final class ItemFactory {

    private static NamespacedKey ITEM_KEY;
    private static NamespacedKey ATTR_DAMAGE;
    private static NamespacedKey ATTR_SPEED;

    private ItemFactory() {
    }

    public static void init(RivalsPlugin plugin) {
        ITEM_KEY = new NamespacedKey(plugin, "item");
        ATTR_DAMAGE = new NamespacedKey(plugin, "weapon_damage");
        ATTR_SPEED = new NamespacedKey(plugin, "weapon_speed");
    }

    /** Materiał bazowy - decyduje o zachowaniu vanilla (naciąganie łuku, ładowanie trójzęba itd.). */
    public static Material base(RivalsItem item) {
        return switch (item) {
            case ENERGY_BOW -> Material.BOW;
            case CROSSBOW -> Material.CROSSBOW;
            case HAMMER -> Material.TRIDENT;
            case SWORD, KATANA -> Material.IRON_SWORD;
            default -> Material.PAPER;
        };
    }

    /** Czy cooldown jest liczony per materiał (przedmioty z vanilla "użyciem"). */
    public static boolean materialCooldown(RivalsItem item) {
        Material m = base(item);
        return m == Material.BOW || m == Material.CROSSBOW || m == Material.TRIDENT;
    }

    public static ItemStack create(RivalsPlugin plugin, RivalsItem item, Cosmetic skin) {
        return create(plugin, item, skin, item.modelKey());
    }

    /** @param modelKey klucz item_model (np. aktywny kryształ powrotu ma inny model) */
    public static ItemStack create(RivalsPlugin plugin, RivalsItem item, Cosmetic skin, String modelKey) {
        RivalsConfig cfg = plugin.settings();
        ItemStack it = new ItemStack(base(item));
        ItemMeta meta = it.getItemMeta();
        String model = skin != null && skin.weapon == item && modelKey.equals(item.modelKey()) ? skin.modelKey() : modelKey;
        meta.setItemModel(NamespacedKey.fromString(model));

        Component name = Component.text().append(Hud.icon(item)).append(Component.text(" "))
                .append(Hud.text(item.displayName, Hud.TEXT)).build().decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);
        meta.lore(lore(cfg, item, skin));

        meta.setUnbreakable(true);
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, item.id);

        if (base(item) == Material.PAPER) {
            UseCooldownComponent cd = meta.getUseCooldown();
            cd.setCooldownSeconds(1f);
            cd.setCooldownGroup(new NamespacedKey("rivals", item.id));
            meta.setUseCooldown(cd);
        }
        if (item.slot == RivalsItem.Slot.MELEE) {
            double dmg = cfg.stat(item, "damage");
            double spd = cfg.stat(item, "attack-speed");
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                    new AttributeModifier(ATTR_DAMAGE, dmg - 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            meta.addAttributeModifier(Attribute.ATTACK_SPEED,
                    new AttributeModifier(ATTR_SPEED, spd - 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }
        if (item == RivalsItem.ENERGY_BOW) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.setEnchantmentGlintOverride(false);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        it.setItemMeta(meta);
        return it;
    }

    private static List<Component> lore(RivalsConfig cfg, RivalsItem item, Cosmetic skin) {
        List<Component> lore = new ArrayList<>();
        String head = item.slot.label + (item.group != null ? " · " + item.group.label : "");
        lore.add(Hud.header(head));
        lore.add(Hud.text(item.description, Hud.MUTED));
        lore.add(Component.empty());
        for (String[] stat : stats(cfg, item)) {
            lore.add(Component.text().append(Hud.text(stat[0] + ": ", Hud.MUTED)).append(Hud.text(stat[1], Hud.TEXT))
                    .build().decoration(TextDecoration.ITALIC, false));
        }
        if (skin != null && skin.weapon == item) {
            lore.add(Component.empty());
            lore.add(Hud.text("Skórka: " + skin.displayName, TextColor.color(skin.color)));
        }
        return lore;
    }

    private static List<String[]> stats(RivalsConfig cfg, RivalsItem item) {
        List<String[]> s = new ArrayList<>();
        switch (item.slot) {
            case PRIMARY, SECONDARY -> {
                if (item == RivalsItem.SHOTGUN) {
                    s.add(new String[]{"Obrażenia", fmt(cfg.stat(item, "pellet-damage")) + " × " + cfg.statInt(item, "pellets")});
                } else {
                    s.add(new String[]{"Obrażenia", fmt(cfg.stat(item, "damage"))});
                }
                if (cfg.stat(item, "magazine") > 0) {
                    s.add(new String[]{"Magazynek", cfg.statInt(item, "magazine") + " · przeładowanie " + fmt(cfg.stat(item, "reload")) + " s"});
                }
            }
            case MELEE -> {
                s.add(new String[]{"Obrażenia", fmt(cfg.stat(item, "damage"))});
                s.add(new String[]{"Szybkość ataku", fmt(cfg.stat(item, "attack-speed"))});
                if (item == RivalsItem.HAMMER) s.add(new String[]{"Fala (PPM)", "cooldown " + fmt(cfg.stat(item, "cooldown")) + " s"});
            }
            case GADGET -> s.add(new String[]{"Cooldown", fmt(cfg.stat(item, "cooldown")) + " s"});
        }
        return s;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    /** Odczyt przedmiotu RIVALS z ItemStacka (null, gdy to nie nasz przedmiot). */
    public static RivalsItem read(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        String id = it.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
        return RivalsItem.byId(id);
    }

    /** Przedmiot bez znaczenia w grze - tylko model (display entities, GUI). */
    public static ItemStack model(String modelKey) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta meta = it.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString(modelKey));
        meta.setHideTooltip(true);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack model(WorldModel wm) {
        return model(wm.key());
    }

    /** Ikona GUI z nazwą i opisem. */
    public static ItemStack icon(String modelKey, Component name, List<Component> lore, boolean glint) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta meta = it.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString(modelKey));
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        if (lore != null) meta.lore(lore);
        if (glint) meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        it.setItemMeta(meta);
        return it;
    }
}
