package pl.rivals.match;

import java.util.HashMap;
import java.util.Map;

/** Statystyki uczestnika w jednym meczu (podsumowanie po meczu). */
public final class MatchStats {

    public double damageDealt;
    public int shots;
    public int hits;
    public int kills;
    public int deaths;
    public final Map<String, Integer> gadgetsUsed = new HashMap<>();

    public void gadgetUsed(String id) {
        gadgetsUsed.merge(id, 1, Integer::sum);
    }

    public int gadgetsTotal() {
        int sum = 0;
        for (int v : gadgetsUsed.values()) sum += v;
        return sum;
    }

    public int accuracyPercent() {
        return shots <= 0 ? 0 : (int) Math.round(100.0 * Math.min(hits, shots) / shots);
    }
}
