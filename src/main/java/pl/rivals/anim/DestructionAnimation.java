package pl.rivals.anim;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.RivalsPlugin;
import pl.rivals.arena.BlockVec;
import pl.rivals.config.AnimationSettings;
import pl.rivals.shared.SoundId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Animacja niszczenia i odbudowy zestawu bloków - używana przez ścianę startową
 * i przez pierścienie "nagłej śmierci".
 *
 * <ol>
 *   <li>{@link #startCrack(int)} - pęknięcia jak przy kopaniu (etapy 0->9), bloki bliżej
 *       środka pękają szybciej (Player#sendBlockDamage, osobne ID źródła dla każdego bloku).</li>
 *   <li>{@link #shatter(Runnable)} - fala od środka na zewnątrz (1 tick na blok odległości):
 *       blok znika, w jego miejscu 4-8 odłamków (block_display) z fizyką liczoną w pluginie.</li>
 *   <li>{@link #rebuild(Runnable)} - odwrotna kolejność (od krawędzi do środka): bloki wyrastają
 *       ze skali 0 do 1 przez interpolację, potem zamieniają się w prawdziwe bloki.</li>
 * </ol>
 */
public final class DestructionAnimation {

    /** ID "kopiących" dla pęknięć - wysoki zakres, żeby nie kolidował z ID bytów. */
    private static final AtomicInteger SOURCE_IDS = new AtomicInteger(0x5A000000);

    private static final class Entry {
        final int x, y, z;
        final double dist;
        final int sourceId;
        final BlockData original;
        int stage = -1;

        Entry(BlockVec v, double dist, int sourceId, BlockData original) {
            this.x = v.x();
            this.y = v.y();
            this.z = v.z();
            this.dist = dist;
            this.sourceId = sourceId;
            this.original = original;
        }
    }

    private final RivalsPlugin plugin;
    private final World world;
    private final Vector origin;
    private final Supplier<Collection<Player>> viewers;
    private final List<Entry> entries = new ArrayList<>();
    private final double maxDist;

    private BukkitTask crackTask;
    private BukkitTask pendingShatter;
    private Timeline shatterLine;
    private Timeline rebuildLine;
    private final Set<BlockDisplay> growing = new HashSet<>();
    private boolean cancelled;

    private int soundTick = -1;
    private int soundsThisTick;

    /**
     * @param blocks  bloki (współrzędne absolutne) i ich docelowy wygląd przy odbudowie
     * @param origin  środek fali (np. środek ściany)
     * @param viewers widzowie efektów (gracze meczu)
     */
    public DestructionAnimation(RivalsPlugin plugin, World world, Map<BlockVec, BlockData> blocks,
                                Vector origin, Supplier<Collection<Player>> viewers) {
        this.plugin = plugin;
        this.world = world;
        this.origin = origin.clone();
        this.viewers = viewers;
        for (Map.Entry<BlockVec, BlockData> e : blocks.entrySet()) {
            BlockVec v = e.getKey();
            double d = new Vector(v.x() + 0.5, v.y() + 0.5, v.z() + 0.5).distance(origin);
            entries.add(new Entry(v, d, SOURCE_IDS.getAndIncrement(), e.getValue()));
        }
        entries.sort(Comparator.comparingDouble(e -> e.dist));
        maxDist = entries.isEmpty() ? 0 : entries.get(entries.size() - 1).dist;
    }

    private AnimationSettings s() {
        return plugin.settings().anim;
    }

    private Location originLoc() {
        return origin.toLocation(world);
    }

    // PĘKANIE
    /** Pęknięcia rosną 0->9 w ciągu durationTicks (środek szybciej). */
    public void startCrack(int durationTicks) {
        stopCrack();
        cancelled = false;
        final double speedup = Math.max(0, Math.min(0.9, s().crackCenterSpeedup()));
        crackTask = new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                t++;
                Collection<Player> vs = viewers.get();
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (Entry e : entries) {
                    Block b = world.getBlockAt(e.x, e.y, e.z);
                    if (b.getType().isAir()) continue;
                    double norm = maxDist <= 0 ? 1 : e.dist / maxDist;
                    double full = Math.max(1, durationTicks * (1.0 - speedup * (1.0 - norm)));
                    int stage = (int) Math.min(9, Math.floor(10.0 * t / full));
                    if (stage > e.stage) {
                        e.stage = stage;
                        sendStage(vs, e, stage);
                        if (r.nextFloat() < 0.18f) {
                            Fx.particle(vs, Particle.BLOCK, new Location(world, e.x + 0.5, e.y + 0.5, e.z + 0.5),
                                    2, 0.35, 0.35, 0.35, 0, e.original);
                        }
                    }
                }
                if (t % 10 == 0) plugin.sounds().play(vs, SoundId.WALL_CRACK, originLoc());
                if (t >= durationTicks) {
                    cancel();
                    crackTask = null;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Pękanie, a po crackTicks automatyczny rozpad (nagła śmierć). */
    public void crackThenShatter(int crackTicks) {
        startCrack(crackTicks);
        pendingShatter = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingShatter = null;
            shatter(null);
        }, crackTicks);
    }

    private void sendStage(Collection<Player> vs, Entry e, int stage) {
        // CraftBukkit: etap = (int)(9 * progress) - dodajemy pół etapu, żeby nie trafić w błąd zaokrąglenia.
        float progress = Math.min(1f, (stage + 0.5f) / 9f);
        Location loc = new Location(world, e.x, e.y, e.z);
        for (Player p : vs) p.sendBlockDamage(loc, progress, e.sourceId);
    }

    /**
     * Zdejmuje pęknięcia. Klient trzyma postęp kopania per ID źródła, więc "przenosimy" każde ID
     * na niewidoczny blok powietrza pod areną (inaczej pęknięcia wróciłyby na odbudowany blok).
     */
    public void clearCracks() {
        Collection<Player> vs = viewers.get();
        for (Entry e : entries) {
            e.stage = -1;
            Location dummy = new Location(world, e.x, world.getMinHeight(), e.z);
            for (Player p : vs) p.sendBlockDamage(dummy, 0f, e.sourceId);
        }
    }

    private void stopCrack() {
        if (crackTask != null) crackTask.cancel();
        crackTask = null;
    }

    // ROZPAD
    /** Fala zniszczenia od środka na zewnątrz. */
    public void shatter(Runnable onDone) {
        stopCrack();
        if (pendingShatter != null) pendingShatter.cancel();
        pendingShatter = null;
        if (shatterLine != null) shatterLine.cancel();
        cancelled = false;

        double perBlock = Math.max(0, s().waveTicksPerBlock());
        int[] remaining = {entries.size()};
        shatterLine = new Timeline();
        shatterLine.at(0, () -> {
            Collection<Player> vs = viewers.get();
            plugin.sounds().play(vs, SoundId.WALL_EXPLODE, originLoc());
            Fx.particle(vs, Particle.EXPLOSION, originLoc(), 1, 0, 0, 0, 0, null);
        });
        for (Entry e : entries) {
            int delay = (int) Math.round(e.dist * perBlock);
            shatterLine.at(delay, () -> breakBlock(e, remaining[0]--));
        }
        shatterLine.start(plugin, onDone);
    }

    private void breakBlock(Entry e, int remainingIncl) {
        Block b = world.getBlockAt(e.x, e.y, e.z);
        if (b.getType().isAir()) return;
        BlockData data = b.getBlockData();
        b.setType(Material.AIR, false);

        Collection<Player> vs = viewers.get();
        Location center = new Location(world, e.x + 0.5, e.y + 0.5, e.z + 0.5);
        Fx.particle(vs, Particle.BLOCK, center, 10, 0.3, 0.3, 0.3, 0, data);
        playLimited(vs, SoundId.WALL_BREAK, center);

        AnimationSettings s = s();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int want = s.fragmentsMin() + (s.fragmentsMax() > s.fragmentsMin() ? r.nextInt(s.fragmentsMax() - s.fragmentsMin() + 1) : 0);
        int avail = plugin.fragments().available();
        double fair = (double) avail / Math.max(1, remainingIncl);
        int count = (int) Math.floor(fair);
        if (r.nextDouble() < fair - count) count++;
        count = Math.min(want, count);

        Vector3f dir = new Vector3f((float) (center.getX() - origin.getX()), (float) (center.getY() - origin.getY()),
                (float) (center.getZ() - origin.getZ()));
        if (dir.lengthSquared() < 1.0e-4f) dir.set(r.nextFloat() - 0.5f, 0.3f, r.nextFloat() - 0.5f);
        dir.normalize();
        float speed = s.speed();
        for (int i = 0; i < count; i++) {
            Vector3f v = new Vector3f(dir).mul(speed * (0.6f + r.nextFloat() * 0.8f));
            v.add((r.nextFloat() - 0.5f) * speed * 1.2f, 0, (r.nextFloat() - 0.5f) * speed * 1.2f);
            v.y += speed * (0.4f + r.nextFloat() * 0.8f);
            if (!plugin.fragments().spawn(center, data, v, vs)) break;
        }
    }

    // ODBUDOWA
    /** Odbudowa od krawędzi do środka; bloki wyrastają ze skali 0 do 1. */
    public void rebuild(Runnable onDone) {
        stopCrack();
        if (pendingShatter != null) pendingShatter.cancel();
        pendingShatter = null;
        if (shatterLine != null) shatterLine.cancel();
        if (rebuildLine != null) rebuildLine.cancel();
        cancelled = false;
        clearCracks();

        double perBlock = Math.max(0, s().waveTicksPerBlock());
        rebuildLine = new Timeline();
        for (Entry e : entries) {
            if (world.getBlockAt(e.x, e.y, e.z).getBlockData().equals(e.original)) continue;
            int delay = (int) Math.round((maxDist - e.dist) * perBlock);
            rebuildLine.at(delay, () -> grow(e));
        }
        rebuildLine.start(plugin, onDone);
    }

    private void grow(Entry e) {
        Block b = world.getBlockAt(e.x, e.y, e.z);
        if (b.getBlockData().equals(e.original)) return;
        Collection<Player> vs = viewers.get();
        Location blockCenter = new Location(world, e.x + 0.5, e.y + 0.5, e.z + 0.5);
        playLimited(vs, SoundId.WALL_REBUILD, blockCenter);
        Fx.particle(vs, Particle.BLOCK, blockCenter, 4, 0.25, 0.25, 0.25, 0, e.original);

        if (vs.isEmpty() || !plugin.fragments().tryAcquireRebuild()) {
            b.setBlockData(e.original, false);
            return;
        }
        int grow = s().growTicks();
        BlockDisplay display = Fx.spawnFor(plugin, new Location(world, e.x, e.y, e.z), BlockDisplay.class, vs, d -> {
            d.setBlock(e.original);
            d.setInterpolationDuration(0);
            d.setTransformation(scaled(0.01f));
        });
        growing.add(display);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (cancelled || !display.isValid()) return;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(grow);
            display.setTransformation(scaled(1f));
        }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.fragments().releaseRebuild();
            growing.remove(display);
            if (display.isValid()) display.remove();
            if (!cancelled) world.getBlockAt(e.x, e.y, e.z).setBlockData(e.original, false);
        }, 1L + grow);
    }

    /** Kostka 1x1x1 przeskalowana względem własnego środka. */
    private static Transformation scaled(float s) {
        float off = 0.5f - s * 0.5f;
        return new Transformation(new Vector3f(off, off, off), new Quaternionf(), new Vector3f(s, s, s), new Quaternionf());
    }

    private void playLimited(Collection<Player> vs, SoundId sound, Location at) {
        int now = Bukkit.getCurrentTick();
        if (now != soundTick) {
            soundTick = now;
            soundsThisTick = 0;
        }
        if (soundsThisTick++ < s().maxBreakSoundsPerTick()) plugin.sounds().play(vs, sound, at);
    }

    // STEROWANIE
    /** Zatrzymuje pękanie i zaplanowany rozpad (bez odbudowy). */
    public void cancelPending() {
        stopCrack();
        if (pendingShatter != null) pendingShatter.cancel();
        pendingShatter = null;
        if (shatterLine != null) shatterLine.cancel();
    }

    /** Pełne przerwanie - usuwa też rosnące bloki (np. przy przerwaniu meczu). */
    public void cancel() {
        cancelled = true;
        cancelPending();
        if (rebuildLine != null) rebuildLine.cancel();
        for (BlockDisplay d : growing) {
            if (d.isValid()) d.remove();
        }
        growing.clear();
    }
}
