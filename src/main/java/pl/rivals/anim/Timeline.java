package pl.rivals.anim;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Prosta oś czasu: akcje przypisane do ticków, wykonywane przez jedno zadanie. */
public final class Timeline {

    private final TreeMap<Integer, List<Runnable>> events = new TreeMap<>();
    private BukkitTask task;
    private Runnable onDone;
    private int tick;
    private boolean cancelled;

    public Timeline at(int t, Runnable action) {
        events.computeIfAbsent(Math.max(0, t), k -> new ArrayList<>()).add(action);
        return this;
    }

    public Timeline start(Plugin plugin, Runnable onDone) {
        this.onDone = onDone;
        if (events.isEmpty()) {
            if (onDone != null) onDone.run();
            return this;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::step, 0L, 1L);
        return this;
    }

    private void step() {
        List<Runnable> actions = events.remove(tick++);
        if (actions != null) {
            for (Runnable r : actions) {
                r.run();
                if (cancelled) return;
            }
        }
        if (events.isEmpty()) {
            if (task != null) task.cancel();
            task = null;
            if (onDone != null) onDone.run();
        }
    }

    public void cancel() {
        cancelled = true;
        events.clear();
        if (task != null) task.cancel();
        task = null;
    }
}
