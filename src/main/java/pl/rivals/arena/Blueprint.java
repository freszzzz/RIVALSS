package pl.rivals.arena;

import org.bukkit.block.data.BlockData;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Gotowy, niezmienny plan areny: bloki względem środka (0,0,0 = środek podłogi). */
public final class Blueprint {

    private final Map<BlockVec, BlockData> blocks;
    private final Map<BlockVec, BlockData> wall;
    private final int half;
    private final int maxY;
    private final Map<Integer, Map<BlockVec, BlockData>> ringCache = new HashMap<>();

    Blueprint(Map<BlockVec, BlockData> blocks, Map<BlockVec, BlockData> wall, int half) {
        this.blocks = Collections.unmodifiableMap(blocks);
        this.wall = Collections.unmodifiableMap(wall);
        this.half = half;
        int top = 0;
        for (BlockVec v : blocks.keySet()) top = Math.max(top, v.y());
        this.maxY = top;
    }

    public BlockData get(BlockVec rel) {
        return blocks.get(rel);
    }

    public Map<BlockVec, BlockData> blocks() {
        return blocks;
    }

    /** Bloki ściany startowej (są też zawarte w {@link #blocks()}). */
    public Map<BlockVec, BlockData> wall() {
        return wall;
    }

    public int half() {
        return half;
    }

    public int maxY() {
        return maxY;
    }

    /** Wszystkie bloki (każda wysokość) leżące na pierścieniu o promieniu r. */
    public Map<BlockVec, BlockData> ring(int r) {
        return ringCache.computeIfAbsent(r, radius -> {
            Map<BlockVec, BlockData> out = new LinkedHashMap<>();
            for (Map.Entry<BlockVec, BlockData> e : blocks.entrySet()) {
                if (e.getKey().ring() == radius) out.put(e.getKey(), e.getValue());
            }
            return Collections.unmodifiableMap(out);
        });
    }
}
