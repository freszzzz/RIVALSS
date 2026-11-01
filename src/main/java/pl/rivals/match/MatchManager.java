package pl.rivals.match;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import pl.rivals.RivalsPlugin;
import pl.rivals.arena.ArenaInstance;
import pl.rivals.arena.ArenaTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Wszystkie trwające mecze; jedno zadanie tickuje je wszystkie. */
public final class MatchManager {

    private final RivalsPlugin plugin;
    private final List<Match> matches = new ArrayList<>();
    private final Map<UUID, Match> byEntity = new HashMap<>();
    private BukkitTask task;

    public MatchManager(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        for (Match m : new ArrayList<>(matches)) {
            try {
                m.tick();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Błąd w meczu #" + m.id() + " – przerywam mecz", t);
                try {
                    m.abort();
                } catch (Throwable ignored) {
                    remove(m);
                }
            }
        }
    }

    public boolean hasFreeSlot() {
        return matches.size() < plugin.settings().maxParallel && plugin.arenaWorld().hasFree();
    }

    /** Tworzy i startuje mecz; null, gdy nie ma wolnej areny albo ktoś już gra. */
    public Match create(Combatant a, Combatant b, ArenaTemplate template, boolean training) {
        if (template == null || !hasFreeSlot()) return null;
        if (byEntity.containsKey(a.id()) || byEntity.containsKey(b.id())) return null;
        ArenaInstance arena = plugin.arenaWorld().acquire(template);
        if (arena == null) return null;
        Match m = new Match(plugin, this, arena, a, b, training);
        matches.add(m);
        if (!a.isBot()) register(a.id(), m);
        if (!b.isBot()) register(b.id(), m);
        plugin.queue().remove(a.id());
        plugin.queue().remove(b.id());
        m.start();
        return m;
    }

    void register(UUID entityId, Match m) {
        byEntity.put(entityId, m);
    }

    void remove(Match m) {
        matches.remove(m);
        byEntity.values().removeIf(x -> x == m);
        plugin.arenaWorld().release(m.arena());
    }

    public Match of(Entity e) {
        return e == null ? null : byEntity.get(e.getUniqueId());
    }

    public Match ofPlayer(UUID id) {
        return byEntity.get(id);
    }

    public List<Match> all() {
        return Collections.unmodifiableList(matches);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (Match m : new ArrayList<>(matches)) {
            try {
                m.abort();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Błąd przy przerywaniu meczu #" + m.id(), t);
            }
        }
        matches.clear();
        byEntity.clear();
    }
}
