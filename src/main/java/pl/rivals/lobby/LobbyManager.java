package pl.rivals.lobby;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import pl.rivals.RivalsPlugin;
import pl.rivals.match.Status;

/** Lobby: punkt powrotu, reset stanu gracza i ochrona. */
public final class LobbyManager implements Listener {

    private final RivalsPlugin plugin;

    public LobbyManager(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    public Location lobbyLocation() {
        Location l = plugin.getConfig().getLocation("lobby.location");
        if (l == null || l.getWorld() == null) {
            l = Bukkit.getWorlds().get(0).getSpawnLocation().add(0.5, 0, 0.5);
        }
        return l;
    }

    public void setLobby(Location l) {
        plugin.getConfig().set("lobby.location", l);
        plugin.saveConfig();
    }

    /** Teleport do lobby i pełny reset stanu z meczu. */
    public void sendToLobby(Player p) {
        if (p.getGameMode() == GameMode.SPECTATOR) p.setSpectatorTarget(null);
        p.setGameMode(plugin.settings().lobbyGameMode);
        p.teleport(lobbyLocation());
        Status.clearAll(p);
        AttributeInstance mh = p.getAttribute(Attribute.MAX_HEALTH);
        if (mh != null) mh.setBaseValue(20);
        p.setHealth(mh == null ? 20 : Math.min(20, mh.getValue()));
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFreezeTicks(0);
        p.setFallDistance(0);
        p.setVelocity(new Vector());
        for (PotionEffect pe : p.getActivePotionEffects()) p.removePotionEffect(pe.getType());
        p.getInventory().clear();
        p.resetPlayerTime();
        plugin.lobbyItems().give(p);
    }

    private boolean inLobby(Player p) {
        if (plugin.matches().ofPlayer(p.getUniqueId()) != null) return false;
        Location lobby = lobbyLocation();
        return lobby.getWorld() != null && p.getWorld().getUID().equals(lobby.getWorld().getUID());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        boolean inArenaWorld = plugin.arenaWorld().isArenaWorld(p.getWorld());
        if (!plugin.settings().teleportOnJoin && !inArenaWorld) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isOnline() && plugin.matches().ofPlayer(p.getUniqueId()) == null) sendToLobby(p);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!plugin.settings().lobbyProtect || !(e.getEntity() instanceof Player p) || !inLobby(p)) return;
        e.setCancelled(true);
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID) sendToLobby(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (plugin.settings().lobbyProtect && e.getEntity() instanceof Player p && inLobby(p)) e.setCancelled(true);
    }
}
