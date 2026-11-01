package pl.rivals.arena;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import pl.rivals.RivalsPlugin;

/**
 * Rozłożone na ticki porównanie świata z planem areny: stawia brakujące bloki i czyści zbędne.
 * Przy ponownym użyciu tej samej areny zmienia tylko to, co zostało zniszczone.
 */
final class BuildTask extends BukkitRunnable {

    private static final BlockData AIR = Material.AIR.createBlockData();

    private final RivalsPlugin plugin;
    private final ArenaInstance arena;
    private final Blueprint blueprint;
    private final Runnable done;
    private final int half;
    private final int minY = -2;
    private final int maxY;

    private int x;
    private int z;
    private int y;

    BuildTask(RivalsPlugin plugin, ArenaInstance arena, Runnable done) {
        this.plugin = plugin;
        this.arena = arena;
        this.blueprint = arena.template().blueprint();
        this.done = done;
        this.half = plugin.arenas().clearHalf();
        this.maxY = plugin.arenas().clearTop();
        this.x = -half;
        this.z = -half;
        this.y = minY;
    }

    @Override
    public void run() {
        World world = arena.world();
        int budget = plugin.settings().buildBlocksPerTick;
        while (budget-- > 0) {
            if (x > half) {
                cancel();
                done.run();
                return;
            }
            BlockData want = blueprint.get(new BlockVec(x, y, z));
            if (want == null) want = AIR;
            Block b = world.getBlockAt(arena.ox() + x, arena.oy() + y, arena.oz() + z);
            if (b.getType() != want.getMaterial() || !b.getBlockData().equals(want)) {
                b.setBlockData(want, false);
            }
            advance();
        }
    }

    private void advance() {
        if (++y > maxY) {
            y = minY;
            if (++z > half) {
                z = -half;
                x++;
            }
        }
    }
}
