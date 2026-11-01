package pl.rivals.anim;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.RivalsPlugin;
import pl.rivals.config.AnimationSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Globalna pula odłamków dla wszystkich meczów: jedno zadanie tickujące, twardy limit
 * (animation.max-fragments) i sprzątanie po animation.fragment-cleanup-ticks.
 */
public final class FragmentManager {

    private final RivalsPlugin plugin;
    private final List<Fragment> active = new ArrayList<>();
    private int rebuildDisplays;
    private BukkitTask task;

    public FragmentManager(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public int available() {
        return Math.max(0, plugin.settings().anim.maxFragments() - active.size());
    }

    /**
     * Wystrzeliwuje odłamek ze środka bloku.
     * @return false, jeśli limit odłamków jest wyczerpany.
     */
    public boolean spawn(Location blockCenter, BlockData data, Vector3f velocity, Collection<? extends Player> viewers) {
        AnimationSettings s = plugin.settings().anim;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        float size = s.sizeMin() + r.nextFloat() * Math.max(0f, s.sizeMax() - s.sizeMin());
        int life = Math.min(s.fragmentLife() + r.nextInt(5), s.cleanupTicks());
        return spawn(blockCenter, data, velocity, viewers, size, s.gravity(), life);
    }

    /** Odłamek o własnym rozmiarze, grawitacji (ujemna = unosi się) i czasie życia. */
    public boolean spawn(Location blockCenter, BlockData data, Vector3f velocity, Collection<? extends Player> viewers,
                         float size, float gravity, int lifeTicks) {
        if (available() <= 0 || viewers.isEmpty()) return false;
        AnimationSettings s = plugin.settings().anim;
        ThreadLocalRandom r = ThreadLocalRandom.current();

        Vector3f offset = new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f).mul(0.5f);
        Quaternionf rot = new Quaternionf().rotateXYZ(r.nextFloat() * 6.283f, r.nextFloat() * 6.283f, r.nextFloat() * 6.283f);
        Vector3f axis = new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f);
        if (axis.lengthSquared() < 1.0e-4f) axis.set(0, 1, 0);
        axis.normalize();
        float spin = (0.15f + r.nextFloat() * 0.35f) * (r.nextBoolean() ? 1 : -1);
        int life = Math.max(2, Math.min(lifeTicks, s.cleanupTicks()));
        Transformation initial = Fragment.transform(offset, rot, size);

        BlockDisplay display = Fx.spawnFor(plugin, blockCenter, BlockDisplay.class, viewers, d -> {
            d.setBlock(data);
            d.setInterpolationDuration(0);
            d.setTeleportDuration(0);
            d.setTransformation(initial);
        });
        active.add(new Fragment(display, offset, velocity, rot, axis, spin, size, life, s.keyframeTicks(), gravity));
        return true;
    }

    // licznik "rosnących" bloków przy odbudowie (osobny limit)
    public boolean tryAcquireRebuild() {
        if (rebuildDisplays >= plugin.settings().anim.maxRebuildDisplays()) return false;
        rebuildDisplays++;
        return true;
    }

    public void releaseRebuild() {
        rebuildDisplays = Math.max(0, rebuildDisplays - 1);
    }

    private void tick() {
        Iterator<Fragment> it = active.iterator();
        while (it.hasNext()) {
            Fragment f = it.next();
            boolean alive;
            try {
                alive = f.tick();
            } catch (RuntimeException ex) {
                f.remove();
                alive = false;
            }
            if (!alive) it.remove();
        }
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (Fragment f : active) f.remove();
        active.clear();
    }
}
