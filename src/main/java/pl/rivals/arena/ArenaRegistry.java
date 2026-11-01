package pl.rivals.arena;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.rivals.RivalsPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Wszystkie szablony aren wczytane z arenas.yml. */
public final class ArenaRegistry {

    private final Map<String, ArenaTemplate> templates;
    private final int clearHalf;
    private final int clearTop;

    private ArenaRegistry(Map<String, ArenaTemplate> templates) {
        this.templates = templates;
        int h = 17;
        int top = 8;
        for (ArenaTemplate t : templates.values()) {
            h = Math.max(h, t.half());
            top = Math.max(top, t.blueprint().maxY());
        }
        this.clearHalf = h + 2;
        this.clearTop = top + 4;
    }

    public static ArenaRegistry load(RivalsPlugin plugin, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Map<String, ArenaTemplate> map = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("arenas");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) continue;
                try {
                    map.put(id.toLowerCase(Locale.ROOT), parse(id.toLowerCase(Locale.ROOT), s));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Arena '" + id + "' pominięta: " + ex.getMessage());
                }
            }
        }
        if (map.isEmpty()) plugin.getLogger().severe("Brak poprawnych aren w arenas.yml!");
        return new ArenaRegistry(map);
    }

    private static ArenaTemplate parse(String id, ConfigurationSection s) {
        int size = Math.max(15, s.getInt("size", 35));
        int half = size / 2;
        int spawnDistance = Math.max(2, Math.min(half - 2, s.getInt("spawn-distance", 6)));
        int wallWidth = s.getInt("wall.width", 9);
        int wallHeight = s.getInt("wall.height", 5);

        Map<String, String> palette = new LinkedHashMap<>();
        ConfigurationSection pal = s.getConfigurationSection("palette");
        if (pal != null) for (String k : pal.getKeys(false)) palette.put(k, pal.getString(k));

        BlueprintBuilder builder = new BlueprintBuilder(palette, half);
        for (String line : s.getStringList("structures")) builder.structure(line);
        Blueprint bp = builder.build(Bukkit.createBlockData(s.getString("wall.block", "minecraft:stone")),
                wallWidth, wallHeight, spawnDistance);

        return new ArenaTemplate(id,
                s.getString("display-name", id),
                s.getBoolean("enabled", true),
                s.getDouble("gravity", 0.08),
                s.getLong("time", 6000),
                half, spawnDistance, wallWidth, wallHeight, bp,
                decorations(s.getStringList("decorations")));
    }

    /**
     * "typ x y z [yaw] [mx] [single]" - domyślnie lustro po osi Z (z -> -z, yaw -> 180 - yaw),
     * "mx" dodatkowo po osi X, "single" bez lustra.
     */
    private static List<ArenaTemplate.Decoration> decorations(List<String> lines) {
        List<ArenaTemplate.Decoration> out = new ArrayList<>();
        for (String line : lines) {
            String[] t = line.trim().split("\\s+");
            if (t.length < 4) continue;
            try {
                String type = t[0].toLowerCase(Locale.ROOT);
                double x = Double.parseDouble(t[1]), y = Double.parseDouble(t[2]), z = Double.parseDouble(t[3]);
                float yaw = t.length > 4 && !t[4].equalsIgnoreCase("mx") && !t[4].equalsIgnoreCase("single") ? Float.parseFloat(t[4]) : 0;
                boolean mx = false, single = false;
                for (String tok : t) {
                    if (tok.equalsIgnoreCase("mx")) mx = true;
                    if (tok.equalsIgnoreCase("single")) single = true;
                }
                List<ArenaTemplate.Decoration> base = new ArrayList<>();
                base.add(new ArenaTemplate.Decoration(type, x, y, z, yaw));
                if (mx) base.add(new ArenaTemplate.Decoration(type, -x, y, z, -yaw));
                for (ArenaTemplate.Decoration d : base) {
                    out.add(d);
                    if (!single) out.add(new ArenaTemplate.Decoration(type, d.x(), d.y(), -d.z(), 180 - d.yaw()));
                }
            } catch (NumberFormatException ignored) {
                // pomijamy błędną linię
            }
        }
        return out;
    }

    public ArenaTemplate get(String id) {
        return id == null ? null : templates.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<ArenaTemplate> all() {
        return templates.values();
    }

    public List<String> ids() {
        return new ArrayList<>(templates.keySet());
    }

    /** Losowa włączona arena (null, gdy żadnej). */
    public ArenaTemplate random() {
        List<ArenaTemplate> enabled = new ArrayList<>();
        for (ArenaTemplate t : templates.values()) if (t.enabled()) enabled.add(t);
        if (enabled.isEmpty()) return null;
        return enabled.get(ThreadLocalRandom.current().nextInt(enabled.size()));
    }

    /** Połowa szerokości obszaru czyszczonego przy budowie (największa arena + margines). */
    public int clearHalf() {
        return clearHalf;
    }

    /** Wysokość obszaru czyszczonego przy budowie. */
    public int clearTop() {
        return clearTop;
    }
}
