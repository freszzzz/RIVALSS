package pl.rivals.arena;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Buduje {@link Blueprint} z definicji w arenas.yml.
 * Struktury opisuje się dla połowy z &lt; 0 - lustrzane odbicie (z -> -z) daje pełną, symetryczną arenę.
 */
final class BlueprintBuilder {

    private static final BlockData AIR = Material.AIR.createBlockData();

    private final Map<String, BlockData> palette = new HashMap<>();
    private final int half;
    private final Map<BlockVec, BlockData> structures = new LinkedHashMap<>();

    BlueprintBuilder(Map<String, String> paletteDef, int half) {
        this.half = half;
        for (Map.Entry<String, String> e : paletteDef.entrySet()) {
            palette.put(e.getKey(), Bukkit.createBlockData(e.getValue()));
        }
        for (String required : List.of("floor", "floor-alt", "line", "edge")) {
            if (!palette.containsKey(required)) throw new IllegalArgumentException("Brak klucza palety: " + required);
        }
    }

    private BlockData pal(String key) {
        BlockData d = palette.get(key);
        if (d == null) throw new IllegalArgumentException("Nieznany klucz palety: " + key);
        return d;
    }

    /** Parsuje jedną linię z listy "structures". */
    void structure(String line) {
        String[] t = line.trim().split("\\s+");
        if (t.length == 0 || t[0].isEmpty()) return;
        boolean mx = t[t.length - 1].equalsIgnoreCase("mx");
        Map<BlockVec, BlockData> op = new LinkedHashMap<>();
        switch (t[0].toLowerCase(Locale.ROOT)) {
            case "box" -> {
                need(t, mx ? 9 : 8, line);
                fill(op, pal(t[1]), i(t[2]), i(t[3]), i(t[4]), i(t[5]), i(t[6]), i(t[7]));
            }
            case "block" -> {
                need(t, mx ? 6 : 5, line);
                op.put(new BlockVec(i(t[2]), i(t[3]), i(t[4])), pal(t[1]));
            }
            case "air" -> {
                need(t, mx ? 8 : 7, line);
                fill(op, AIR, i(t[1]), i(t[2]), i(t[3]), i(t[4]), i(t[5]), i(t[6]));
            }
            case "stairs" -> {
                need(t, mx ? 10 : 9, line);
                stairs(op, pal(t[1]), pal(t[2]), i(t[3]), i(t[4]), i(t[5]),
                        BlockFace.valueOf(t[6].toUpperCase(Locale.ROOT)), i(t[7]), i(t[8]));
            }
            default -> throw new IllegalArgumentException("Nieznany typ struktury: " + line);
        }
        structures.putAll(op);
        if (mx) {
            for (Map.Entry<BlockVec, BlockData> e : op.entrySet()) {
                structures.put(e.getKey().mirrorX(), mirror(e.getValue(), false));
            }
        }
    }

    Blueprint build(BlockData wallData, int wallWidth, int wallHeight, int spawnDistance) {
        Map<BlockVec, BlockData> blocks = new HashMap<>();

        // 1. Podłoga: kafle 3x3 w szachownicę, linia środkowa, świecąca krawędź.
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                int r = Math.max(Math.abs(x), Math.abs(z));
                BlockData d;
                if (r == half) d = pal("edge");
                else if (z == 0) d = pal("line");
                else d = ((Math.floorDiv(x + 1, 3) + Math.floorDiv(z + 1, 3)) & 1) == 0 ? pal("floor") : pal("floor-alt");
                blocks.put(new BlockVec(x, 0, z), d);
            }
        }

        // 2. Struktury + lustro po osi Z.
        for (Map.Entry<BlockVec, BlockData> e : structures.entrySet()) {
            blocks.put(e.getKey(), e.getValue());
            blocks.put(e.getKey().mirrorZ(), mirror(e.getValue(), true));
        }

        // 3. Strefy startowe (3x3, środek świeci).
        for (int side : new int[]{-1, 1}) {
            int cz = side * spawnDistance;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    blocks.put(new BlockVec(dx, 0, cz + dz), dx == 0 && dz == 0 ? pal("edge") : pal("line"));
                }
            }
        }

        // 4. Ściana startowa na z = 0.
        Map<BlockVec, BlockData> wall = new LinkedHashMap<>();
        int w = Math.max(1, wallWidth) / 2;
        for (int x = -w; x <= w; x++) {
            for (int y = 1; y <= Math.max(1, wallHeight); y++) {
                BlockVec v = new BlockVec(x, y, 0);
                wall.put(v, wallData);
                blocks.put(v, wallData);
            }
        }

        blocks.values().removeIf(d -> d.getMaterial().isAir());
        return new Blueprint(blocks, wall, half);
    }

    private static void fill(Map<BlockVec, BlockData> op, BlockData d, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    op.put(new BlockVec(x, y, z), d);
    }

    /** Schody wznoszące się w kierunku facing; pod każdym stopniem pełna podpora. */
    private static void stairs(Map<BlockVec, BlockData> op, BlockData stair, BlockData support,
                               int x, int y, int z, BlockFace facing, int steps, int width) {
        int fx = facing.getModX();
        int fz = facing.getModZ();
        if (Math.abs(fx) + Math.abs(fz) != 1) throw new IllegalArgumentException("Schody: kierunek musi być north/south/east/west");
        int px = fz != 0 ? 1 : 0;
        int pz = fx != 0 ? 1 : 0;
        BlockData stairData = stair.clone();
        if (stairData instanceof Directional dir && dir.getFaces().contains(facing)) dir.setFacing(facing);
        for (int i = 0; i < steps; i++) {
            for (int wdt = 0; wdt < Math.max(1, width); wdt++) {
                int bx = x + fx * i + px * wdt;
                int bz = z + fz * i + pz * wdt;
                int by = y + i;
                op.put(new BlockVec(bx, by, bz), stairData);
                for (int sy = y; sy < by; sy++) op.put(new BlockVec(bx, sy, bz), support);
            }
        }
    }

    /** Lustrzane odbicie danych bloku kierunkowego (schody itp.). */
    static BlockData mirror(BlockData data, boolean axisZ) {
        if (!(data instanceof Directional)) return data;
        BlockData copy = data.clone();
        Directional d = (Directional) copy;
        BlockFace f = d.getFacing();
        BlockFace nf = f;
        if (axisZ) {
            if (f == BlockFace.NORTH) nf = BlockFace.SOUTH;
            else if (f == BlockFace.SOUTH) nf = BlockFace.NORTH;
        } else {
            if (f == BlockFace.EAST) nf = BlockFace.WEST;
            else if (f == BlockFace.WEST) nf = BlockFace.EAST;
        }
        if (nf != f && d.getFaces().contains(nf)) d.setFacing(nf);
        return copy;
    }

    private static int i(String s) {
        return Integer.parseInt(s);
    }

    private static void need(String[] t, int n, String line) {
        if (t.length < n) throw new IllegalArgumentException("Za mało parametrów: " + line);
    }
}
