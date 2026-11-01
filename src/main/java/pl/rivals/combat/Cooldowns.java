package pl.rivals.combat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.rivals.item.ItemFactory;
import pl.rivals.match.Combatant;
import pl.rivals.shared.RivalsItem;

/**
 * Cooldown liczony przez plugin (logika) + odwzorowany na kliencie (pasek w hotbarze i wariant
 * modelu "_cd" przez właściwość minecraft:cooldown w definicji przedmiotu).
 */
public final class Cooldowns {

    private Cooldowns() {
    }

    public static void start(Combatant c, RivalsItem item, int ticks) {
        c.state.cooldown(item, ticks);
        Player p = c.player();
        if (p == null || ticks <= 0) return;
        if (ItemFactory.materialCooldown(item)) {
            p.setCooldown(ItemFactory.base(item), ticks);
        } else {
            ItemStack s = find(p, item);
            if (s != null) p.setCooldown(s, ticks);
        }
    }

    public static ItemStack find(Player p, RivalsItem item) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (ItemFactory.read(s) == item) return s;
        }
        return null;
    }

    public static int findSlot(Player p, RivalsItem item) {
        for (int i = 0; i < 9; i++) {
            if (ItemFactory.read(p.getInventory().getItem(i)) == item) return i;
        }
        return -1;
    }
}
