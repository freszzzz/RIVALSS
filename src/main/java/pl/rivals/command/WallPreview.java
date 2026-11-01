package pl.rivals.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.rivals.RivalsPlugin;
import pl.rivals.anim.DestructionAnimation;
import pl.rivals.arena.BlockVec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * /rivals admin wallpreview - stawia ścianę 9x5 przed graczem i odgrywa pełny cykl:
 * pękanie (3 s) -> fala rozpadu -> odbudowa -> usunięcie. Do testowania animacji w pojedynkę.
 */
final class WallPreview {

    private WallPreview() {
    }

    static void run(RivalsPlugin plugin, Player player, Material material) {
        World world = player.getWorld();
        BlockFace facing = player.getFacing();
        if (facing.getModY() != 0 || (facing.getModX() != 0 && facing.getModZ() != 0)) facing = BlockFace.NORTH;
        Block base = player.getLocation().getBlock().getRelative(facing, 5);
        int rx = facing.getModZ() != 0 ? 1 : 0;
        int rz = facing.getModX() != 0 ? 1 : 0;

        BlockData data = material.createBlockData();
        Map<BlockVec, BlockData> blocks = new LinkedHashMap<>();
        for (int w = -4; w <= 4; w++) {
            for (int h = 0; h < 5; h++) {
                Block b = base.getRelative(rx * w, h, rz * w);
                if (!b.getType().isAir()) continue;
                b.setBlockData(data, false);
                blocks.put(new BlockVec(b.getX(), b.getY(), b.getZ()), data);
            }
        }
        if (blocks.isEmpty()) return;

        Vector center = base.getLocation().toVector().add(new Vector(0.5, 2.5, 0.5));
        DestructionAnimation anim = new DestructionAnimation(plugin, world, blocks, center, () -> {
            Collection<Player> list = new ArrayList<>();
            for (Player p : world.getPlayers()) {
                if (p.getLocation().toVector().distanceSquared(center) < 64 * 64) list.add(p);
            }
            return list;
        });
        anim.startCrack(60);
        Bukkit.getScheduler().runTaskLater(plugin, () -> anim.shatter(() ->
                Bukkit.getScheduler().runTaskLater(plugin, () -> anim.rebuild(() ->
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            for (BlockVec v : blocks.keySet()) {
                                Block b = world.getBlockAt(v.x(), v.y(), v.z());
                                if (b.getType() == material) b.setType(Material.AIR, false);
                            }
                        }, 40L)), 50L)), 60L);
    }
}
