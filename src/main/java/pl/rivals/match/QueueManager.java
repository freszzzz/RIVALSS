package pl.rivals.match;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.rivals.arena.ArenaTemplate;
import pl.rivals.config.RivalsConfig;
import pl.rivals.data.PlayerProfile;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.SoundId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static pl.rivals.config.Messages.ph;

/** Kolejka 1v1 z dobieraniem po ELO; dopuszczalna różnica rośnie z czasem czekania. */
public final class QueueManager {

    private final RivalsPlugin plugin;
    private final Map<UUID, Long> queue = new LinkedHashMap<>();
    private BukkitTask task;

    public QueueManager(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public boolean join(Player p) {
        if (plugin.matches().ofPlayer(p.getUniqueId()) != null) {
            plugin.messages().send(p, "queue.in-match");
            return false;
        }
        if (queue.containsKey(p.getUniqueId())) {
            plugin.messages().send(p, "queue.already");
            return false;
        }
        queue.put(p.getUniqueId(), System.currentTimeMillis());
        plugin.messages().send(p, "queue.joined", ph("count", queue.size()));
        plugin.sounds().play(p, SoundId.QUEUE_JOIN);
        return true;
    }

    public boolean leave(Player p, boolean silent) {
        boolean was = queue.remove(p.getUniqueId()) != null;
        if (!silent) plugin.messages().send(p, was ? "queue.left" : "queue.not-queued");
        return was;
    }

    void remove(UUID id) {
        queue.remove(id);
    }

    public boolean isQueued(UUID id) {
        return queue.containsKey(id);
    }

    public long waitSeconds(UUID id) {
        Long since = queue.get(id);
        return since == null ? 0 : (System.currentTimeMillis() - since) / 1000;
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        if (task != null) task.cancel();
        queue.clear();
    }

    private int elo(UUID id) {
        PlayerProfile p = plugin.profiles().get(id);
        return p == null ? plugin.settings().eloStart : p.elo;
    }

    private void tick() {
        queue.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        RivalsConfig cfg = plugin.settings();
        long now = System.currentTimeMillis();
        List<UUID> ids = new ArrayList<>(queue.keySet());
        Set<UUID> paired = new HashSet<>();

        for (int i = 0; i < ids.size(); i++) {
            UUID first = ids.get(i);
            if (paired.contains(first)) continue;
            if (!plugin.matches().hasFreeSlot()) break;
            UUID best = null;
            int bestDiff = Integer.MAX_VALUE;
            for (int j = i + 1; j < ids.size(); j++) {
                UUID second = ids.get(j);
                if (paired.contains(second)) continue;
                double waitSec = (now - Math.min(queue.get(first), queue.get(second))) / 1000.0;
                double range = cfg.eloRangeStart + cfg.eloRangeGrowth * waitSec;
                int diff = Math.abs(elo(first) - elo(second));
                if (diff <= range && diff < bestDiff) {
                    best = second;
                    bestDiff = diff;
                }
            }
            if (best == null) continue;
            Player p1 = Bukkit.getPlayer(first);
            Player p2 = Bukkit.getPlayer(best);
            ArenaTemplate template = plugin.arenas().random();
            if (p1 == null || p2 == null || template == null) continue;
            Match m = plugin.matches().create(Combatant.of(p1), Combatant.of(p2), template, false);
            if (m != null) {
                paired.add(first);
                paired.add(best);
            }
        }

        boolean free = plugin.matches().hasFreeSlot();
        for (Map.Entry<UUID, Long> e : queue.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null) continue;
            long sec = (now - e.getValue()) / 1000;
            String time = String.format("%d:%02d", sec / 60, sec % 60);
            p.sendActionBar(plugin.messages().get(free ? "queue.searching" : "queue.no-arena", ph("time", time)));
        }
    }
}
