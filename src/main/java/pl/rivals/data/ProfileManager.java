package pl.rivals.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.rivals.RivalsPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Cache profili graczy online. Wczytywanie asynchroniczne, zapisy przez {@link Database}. */
public final class ProfileManager implements Listener {

    private final RivalsPlugin plugin;
    private final Map<UUID, PlayerProfile> cache = new HashMap<>();

    public ProfileManager(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(Player player) {
        UUID id = player.getUniqueId();
        plugin.database().load(id, player.getName(), plugin.settings().eloStart)
                .whenComplete((profile, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null) {
                        plugin.getLogger().log(Level.WARNING, "Nie wczytano profilu " + player.getName(), error);
                        return;
                    }
                    if (!player.isOnline()) return;
                    profile.name = player.getName();
                    cache.put(id, profile);
                }));
    }

    public PlayerProfile get(UUID id) {
        return cache.get(id);
    }

    public void save(PlayerProfile profile) {
        if (profile != null) plugin.database().save(profile.copy());
    }

    public void saveAll() {
        for (PlayerProfile p : cache.values()) save(p);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        load(e.getPlayer());
    }

    /** MONITOR - mecz (LOWEST) zdąży jeszcze rozliczyć walkower na profilu. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        PlayerProfile p = cache.remove(e.getPlayer().getUniqueId());
        save(p);
    }
}
