package pl.rivals.hud;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Sidebar jednego gracza. Numery ukryte (NumberFormat.blank), treść wierszy przez customName.
 * Aktualizowane są tylko wiersze, które się zmieniły - bez migotania.
 */
public final class SidebarHud {

    private static final int MAX = 15;

    private final Scoreboard board;
    private final Objective objective;
    private final Component[] cache = new Component[MAX];
    private Component titleCache;
    private int shown;

    // wartości do rozjaśniania przy zmianie i animacji liczników
    private final Map<String, String> lastValues = new HashMap<>();
    private final Map<String, Long> flashUntil = new HashMap<>();
    private final Map<String, Integer> tween = new HashMap<>();

    public SidebarHud(Player p) {
        board = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = board.registerNewObjective("rivals", Criteria.DUMMY, Component.text("RIVALS"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        p.setScoreboard(board);
    }

    public Scoreboard board() {
        return board;
    }

    public void title(Component c) {
        if (Objects.equals(c, titleCache)) return;
        titleCache = c;
        objective.displayName(c);
    }

    public void lines(List<Component> lines) {
        int n = Math.min(MAX, lines.size());
        for (int i = 0; i < n; i++) {
            Component c = lines.get(i);
            if (Objects.equals(c, cache[i])) continue;
            Score s = objective.getScore("l" + i);
            if (cache[i] == null) s.setScore(MAX - i);
            s.customName(c);
            cache[i] = c;
        }
        for (int i = n; i < shown; i++) {
            board.resetScores("l" + i);
            cache[i] = null;
        }
        shown = n;
    }

    /** Wartość, która na chwilę się rozjaśnia po zmianie. */
    public Component value(String key, String value, TextColor normal, long tick) {
        String prev = lastValues.put(key, value);
        if (prev != null && !prev.equals(value)) flashUntil.put(key, tick + 12);
        boolean flash = flashUntil.getOrDefault(key, 0L) > tick;
        return Hud.text(value, flash ? Hud.ACCENT_BRIGHT : normal);
    }

    /** Licznik nabijany stopniowo do celu. @return {aktualnie wyświetlana, 1 jeśli w trakcie animacji} */
    public int[] tween(String key, int target) {
        int cur = tween.getOrDefault(key, target);
        if (cur == target) {
            tween.put(key, target);
            return new int[]{target, 0};
        }
        int diff = target - cur;
        int step = Math.max(1, Math.abs(diff) / 6);
        cur += Integer.signum(diff) * Math.min(step, Math.abs(diff));
        tween.put(key, cur);
        return new int[]{cur, 1};
    }
}
