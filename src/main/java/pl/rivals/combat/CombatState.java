package pl.rivals.combat;

import org.bukkit.Bukkit;
import pl.rivals.item.Kit;
import pl.rivals.shared.RivalsItem;

import java.util.EnumMap;
import java.util.Map;

/** Stan bojowy uczestnika: zestaw, amunicja, cooldowny (liczone w tickach serwera). */
public final class CombatState {

    public Kit kit = Kit.FALLBACK;
    /** Zestaw zatwierdzony przyciskiem "Gotowe" w GUI. */
    public boolean kitReady;
    /** Dodatkowe ładunki gadżetu (bonus z kapsuły) - można użyć mimo cooldownu. */
    public int bonusCharges;

    private final Map<RivalsItem, Integer> ammo = new EnumMap<>(RivalsItem.class);
    private final Map<RivalsItem, Long> readyAt = new EnumMap<>(RivalsItem.class);
    private final Map<RivalsItem, Integer> total = new EnumMap<>(RivalsItem.class);
    private final Map<RivalsItem, Boolean> reloading = new EnumMap<>(RivalsItem.class);
    private final Map<RivalsItem, Long> lastShot = new EnumMap<>(RivalsItem.class);

    public static long now() {
        return Bukkit.getCurrentTick();
    }

    public boolean ready(RivalsItem item) {
        return now() >= readyAt.getOrDefault(item, 0L);
    }

    public int remaining(RivalsItem item) {
        return (int) Math.max(0, readyAt.getOrDefault(item, 0L) - now());
    }

    /** 0 = gotowy, 1 = dopiero rozpoczęty cooldown. */
    public float fraction(RivalsItem item) {
        int t = total.getOrDefault(item, 0);
        return t <= 0 ? 0 : Math.min(1f, remaining(item) / (float) t);
    }

    public void cooldown(RivalsItem item, int ticks) {
        readyAt.put(item, now() + ticks);
        total.put(item, ticks);
    }

    public int ammo(RivalsItem item, int magazine) {
        return ammo.computeIfAbsent(item, k -> magazine);
    }

    public void ammo(RivalsItem item, int value, boolean reload) {
        ammo.put(item, value);
        reloading.put(item, reload);
    }

    public boolean reloading(RivalsItem item) {
        return reloading.getOrDefault(item, false);
    }

    public long lastShot(RivalsItem item) {
        return lastShot.getOrDefault(item, -1000L);
    }

    public void shot(RivalsItem item) {
        lastShot.put(item, now());
    }

    /** Nowa runda: pełne magazynki, zero cooldownów, bez bonusów. */
    public void resetRound() {
        ammo.clear();
        readyAt.clear();
        total.clear();
        reloading.clear();
        lastShot.clear();
        bonusCharges = 0;
    }
}
