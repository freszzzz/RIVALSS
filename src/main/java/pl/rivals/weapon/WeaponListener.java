package pl.rivals.weapon;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import pl.rivals.cosmetic.Cosmetics;
import pl.rivals.item.ItemFactory;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;

/** Wejścia graczy w meczu -> bronie i gadżety. */
public final class WeaponListener implements Listener {

    private final RivalsPlugin plugin;

    public WeaponListener(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    private record Ctx(Match match, Combatant combatant) {
    }

    private Ctx ctx(Player p) {
        Match m = plugin.matches().ofPlayer(p.getUniqueId());
        if (m == null) return null;
        Combatant c = m.combatant(p);
        return c == null ? null : new Ctx(m, c);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        RivalsItem item = ItemFactory.read(e.getItem());
        if (item == null) return;
        Ctx c = ctx(e.getPlayer());
        if (c == null) return;
        if (item == RivalsItem.HAMMER) {
            if (c.match().fighting() && c.combatant().state.ready(item)) {
                plugin.sounds().play(e.getPlayer(), SoundId.HAMMER_CHARGE);
            }
            return; // ładowanie robi vanilla (trójząb)
        }
        if (item == RivalsItem.ENERGY_BOW || item == RivalsItem.CROSSBOW) {
            Weapons.ensureArrow(e.getPlayer());
            return; // naciąganie robi vanilla
        }
        if (plugin.weapons().rightClick(c.match(), c.combatant(), e.getPlayer(), item)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Ctx c = ctx(p);
        if (c == null) return;
        RivalsItem item = ItemFactory.read(e.getBow());
        e.setCancelled(true);
        if (item != RivalsItem.ENERGY_BOW && item != RivalsItem.CROSSBOW) return;
        plugin.weapons().bowShot(c.match(), c.combatant(), p, item, e.getForce());
        if (item == RivalsItem.CROSSBOW) {
            // anulowany strzał - kusza musi się rozładować ręcznie
            ItemStack hand = e.getHand() == EquipmentSlot.OFF_HAND ? p.getInventory().getItemInOffHand() : p.getInventory().getItemInMainHand();
            if (hand.getItemMeta() instanceof CrossbowMeta cm) {
                cm.setChargedProjectiles(null);
                hand.setItemMeta(cm);
            }
        }
        Weapons.ensureArrow(p);
        plugin.getServer().getScheduler().runTask(plugin, p::updateInventory);
    }

    @EventHandler
    public void onStopUsing(PlayerStopUsingItemEvent e) {
        if (ItemFactory.read(e.getItem()) != RivalsItem.HAMMER) return;
        Ctx c = ctx(e.getPlayer());
        if (c == null) return;
        Player p = e.getPlayer();
        plugin.weapons().hammerRelease(c.match(), c.combatant(), p, e.getTicksHeldFor());
        // zabezpieczenie: gdyby vanilla mimo anulowania zabrała trójząb - młot wraca do slotu 3
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || c.combatant().state.kit.melee() != RivalsItem.HAMMER) return;
            if (findHammer(p) == null && plugin.matches().ofPlayer(p.getUniqueId()) == c.match()) {
                p.getInventory().setItem(2, ItemFactory.create(plugin, RivalsItem.HAMMER,
                        Cosmetics.skinFor(plugin.profiles().get(p.getUniqueId()), RivalsItem.HAMMER)));
            }
        }, 2L);
    }

    private static ItemStack findHammer(Player p) {
        for (ItemStack s : p.getInventory().getContents()) {
            if (ItemFactory.read(s) == RivalsItem.HAMMER) return s;
        }
        return null;
    }

    /** Vanilla rzut trójzębem (młot) jest blokowany - zamiast niego fala uderzeniowa. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity() instanceof Trident t && t.getShooter() instanceof Player p && ctx(p) != null) {
            e.setCancelled(true);
        }
    }

    /** Paper: blokada rzutu trójzębem jeszcze przed zużyciem przedmiotu. */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerLaunch(PlayerLaunchProjectileEvent e) {
        if (e.getProjectile() instanceof Trident && ctx(e.getPlayer()) != null) {
            e.setShouldConsume(false);
            e.setCancelled(true);
        }
    }

    /** Klawisz F = przeładowanie trzymanej broni palnej. */
    @EventHandler(priority = EventPriority.LOW)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Ctx c = ctx(e.getPlayer());
        if (c == null) return;
        e.setCancelled(true);
        RivalsItem item = ItemFactory.read(e.getPlayer().getInventory().getItemInMainHand());
        if (item != null && c.match().fighting()) plugin.weapons().reload(c.combatant(), e.getPlayer(), item);
    }

    /** Katana: bonus obrażeń przy sprincie. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMelee(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (ItemFactory.read(p.getInventory().getItemInMainHand()) != RivalsItem.KATANA || !p.isSprinting()) return;
        if (ctx(p) == null) return;
        e.setDamage(e.getDamage() * plugin.settings().stat(RivalsItem.KATANA, "sprint-bonus"));
    }
}
