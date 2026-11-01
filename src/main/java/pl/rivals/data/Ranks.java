package pl.rivals.data;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Rangi wyznaczane z ELO (lista "ranks" w config.yml). */
public final class Ranks {

    public record Rank(String name, int min, String color) {
        public String mini() {
            return "<" + color + ">" + name + "</" + color + ">";
        }
    }

    private final List<Rank> ranks = new ArrayList<>();

    public Ranks(FileConfiguration config) {
        for (Map<?, ?> m : config.getMapList("ranks")) {
            Object name = m.get("name");
            Object min = m.get("min");
            Object color = m.get("color");
            if (name == null || !(min instanceof Number n)) continue;
            ranks.add(new Rank(String.valueOf(name), n.intValue(), color == null ? "#FFFFFF" : String.valueOf(color)));
        }
        if (ranks.isEmpty()) ranks.add(new Rank("Bez rangi", 0, "#FFFFFF"));
        ranks.sort(Comparator.comparingInt(Rank::min));
    }

    public Rank of(int elo) {
        Rank result = ranks.get(0);
        for (Rank r : ranks) {
            if (elo >= r.min()) result = r;
        }
        return result;
    }
}
