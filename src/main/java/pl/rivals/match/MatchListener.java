package pl.rivals.match;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.GameMode;
import org.bukkit.World;
import pl.rivals.RivalsPlugin;

/** Zdarzenia dotyczące uczestników meczów i świata aren. */
public final class MatchListener implements Listener {

    private final RivalsPlugin plugin;

    public MatchListener(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    private MatchManager mm() {
        return plugin.matches();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        Match m = mm().of(victim);
        if (m != null) {
            m.handleDamage(e, victim);
            return;
        }
        // atrapa (klon) - trafienie ją rozbija, sama nie przyjmuje obrażeń
        for (Match match : mm().all()) {
            var onHit = match.effects().decoy(victim);
            if (onHit != null) {
                e.setCancelled(true);
                Entity attacker = e instanceof EntityDamageByEntityEvent ed ? Match.resolveAttacker(ed.getDamager()) : null;
                onHit.accept(match.combatant(attacker));
                return;
            }
        }
        // uczestnik meczu nie może ranić nikogo spoza swojego meczu
        if (e instanceof EntityDamageByEntityEvent ed) {
            Entity attacker = Match.resolveAttacker(ed.getDamager());
            if (attacker != null && mm().of(attacker) != null) e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity().getShooter() instanceof LivingEntity shooter)) return;
        Match m = mm().of(shooter);
        if (m != null) m.handleLaunch(shooter, e.getEntity());
    }

    /** Strzały wbite w bloki areny znikają po chwili. */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile p = e.getEntity();
        if (e.getHitBlock() == null || !(p instanceof AbstractArrow)) return;
        if (!plugin.arenaWorld().isArenaWorld(p.getWorld())) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isValid()) p.remove();
        }, 10L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.queue().leave(p, true);
        Match m = mm().ofPlayer(p.getUniqueId());
        if (m != null) m.handleQuit(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && mm().ofPlayer(p.getUniqueId()) != null) {
            e.setCancelled(true);
            p.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent e) {
        if (plugin.settings().naturalRegeneration) return;
        EntityRegainHealthEvent.RegainReason r = e.getRegainReason();
        if ((r == EntityRegainHealthEvent.RegainReason.SATIATED || r == EntityRegainHealthEvent.RegainReason.REGEN)
                && mm().of(e.getEntity()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (mm().ofPlayer(e.getPlayer().getUniqueId()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (mm().ofPlayer(e.getPlayer().getUniqueId()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (protectedArena(e.getPlayer(), e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (protectedArena(e.getPlayer(), e.getBlock().getWorld())) e.setCancelled(true);
    }

    private boolean protectedArena(Player p, World w) {
        if (!plugin.arenaWorld().isArenaWorld(w)) return false;
        return !(p.hasPermission("rivals.admin") && p.getGameMode() == GameMode.CREATIVE);
    }

    /** Podczas killcamu nie da się wyjść z kamery zabójcy. */
    @EventHandler(ignoreCancelled = true)
    public void onStopSpectating(PlayerStopSpectatingEntityEvent e) {
        Match m = mm().ofPlayer(e.getPlayer().getUniqueId());
        if (m != null && m.state() == Match.State.KILLCAM) e.setCancelled(true);
    }

    /** Bot celuje tylko w swojego przeciwnika. */
    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        Match m = mm().of(e.getEntity());
        if (m == null || e.getTarget() == null) return;
        if (!m.isOpponent(e.getEntity(), e.getTarget())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent e) {
        if (!(e.getEntity() instanceof Player) && mm().of(e.getEntity()) != null) e.setCancelled(true);
    }
}
