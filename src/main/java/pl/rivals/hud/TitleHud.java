package pl.rivals.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.rivals.shared.Glyphs;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jeden kanał tytułów na gracza z priorytetami (błysk &gt; odliczanie &gt; komunikat &gt; hitmarker)
 * i animacją klatkową (powiększenie dużej czcionki - kilka rozmiarów glifów jako klatki).
 */
public final class TitleHud {

    public static final int HIT = 1;
    public static final int MESSAGE = 2;
    public static final int COUNTDOWN = 3;
    public static final int FLASH = 4;

    private record Active(int prio, long until) {
    }

    private final Map<UUID, Active> active = new HashMap<>();
    private final Map<UUID, Deque<Runnable>> frames = new HashMap<>();

    private static long now() {
        return Bukkit.getCurrentTick();
    }

    public static Title.Times times(int in, int stay, int out) {
        return Title.Times.times(Duration.ofMillis(in * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(out * 50L));
    }

    private boolean blocked(Player p, int prio) {
        Active a = active.get(p.getUniqueId());
        return a != null && a.prio > prio && a.until > now();
    }

    public boolean show(Player p, int prio, Component title, Component sub, int in, int stay, int out) {
        if (blocked(p, prio)) return false;
        if (prio > HIT) frames.remove(p.getUniqueId());
        p.showTitle(Title.title(title, sub, times(in, stay, out)));
        active.put(p.getUniqueId(), new Active(prio, now() + in + stay + out));
        return true;
    }

    /**
     * Napis wjeżdżający powiększeniem: klatki od startFrame (duża) do 4 (spoczynkowa), po jednej na tick.
     */
    public void zoom(Player p, int prio, String text, TextColor color, Component sub, int startFrame, int stay) {
        if (blocked(p, prio)) return;
        Deque<Runnable> q = new ArrayDeque<>();
        int last = Glyphs.BIG_HEIGHTS.length - 1;
        for (int f = Math.max(0, startFrame); f <= last; f++) {
            final int frame = f;
            q.add(() -> {
                boolean end = frame == last;
                p.showTitle(Title.title(Hud.bigText(text, color, frame), sub, times(0, end ? stay : 3, end ? 6 : 0)));
                active.put(p.getUniqueId(), new Active(prio, now() + (end ? stay + 6 : 3)));
            });
        }
        frames.put(p.getUniqueId(), q);
        Runnable first = q.poll();
        if (first != null) first.run();
    }

    /** Biały błysk ekranu z zanikaniem (granat oślepiający). */
    public void flash(Player p, int ticks) {
        int stay = Math.max(2, (int) (ticks * 0.35));
        int out = Math.max(4, ticks - stay);
        frames.remove(p.getUniqueId());
        p.showTitle(Title.title(Hud.glyph(Glyphs.FLASH).color(NamedTextColor.WHITE), Component.empty(), times(0, stay, out)));
        active.put(p.getUniqueId(), new Active(FLASH, now() + stay + out));
    }

    /** Mały krzyżyk na środku ekranu na 3 ticki (w podtytule). */
    public void hitmarker(Player p, boolean head) {
        show(p, HIT, Component.empty(), Hud.glyph(head ? Glyphs.HITMARKER_HEAD : Glyphs.HITMARKER), 0, 3, 1);
    }

    public void tick() {
        if (frames.isEmpty()) return;
        var it = frames.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Player p = Bukkit.getPlayer(e.getKey());
            Runnable r = e.getValue().poll();
            if (p == null || r == null) {
                it.remove();
                continue;
            }
            r.run();
            if (e.getValue().isEmpty()) it.remove();
        }
    }

    public void clear(Player p) {
        active.remove(p.getUniqueId());
        frames.remove(p.getUniqueId());
    }
}
