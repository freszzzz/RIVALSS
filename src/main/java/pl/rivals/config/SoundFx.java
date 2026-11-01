package pl.rivals.config;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.rivals.shared.SoundId;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dźwięki trybu. Domyślnie każdy {@link SoundId} gra rivals:&lt;id&gt; z sounds.json resource packa
 * (tam jest dźwięk vanilla z ustawioną wysokością). Sekcja "sounds" w config.yml może podmienić
 * klucz, głośność i wysokość dowolnego wpisu.
 */
public final class SoundFx {

    private record Entry(Key key, float volume, float pitch, float variance) {
    }

    private final Map<SoundId, Entry> entries = new EnumMap<>(SoundId.class);

    public SoundFx(ConfigurationSection overrides) {
        for (SoundId id : SoundId.values()) {
            float variance = id == SoundId.WALL_BREAK || id == SoundId.WALL_CRACK || id == SoundId.WALL_REBUILD
                    || id == SoundId.IMPACT ? 0.12f : 0f;
            entries.put(id, new Entry(Key.key(id.key()), 1f, 1f, variance));
        }
        if (overrides == null) return;
        for (String k : overrides.getKeys(false)) {
            SoundId id = find(k);
            ConfigurationSection s = overrides.getConfigurationSection(k);
            if (id == null || s == null) continue;
            try {
                entries.put(id, new Entry(Key.key(s.getString("key", id.key())),
                        (float) s.getDouble("volume", 1.0), (float) s.getDouble("pitch", 1.0),
                        (float) s.getDouble("variance", 0)));
            } catch (Exception ignored) {
                // niepoprawny klucz - zostaje domyślny
            }
        }
    }

    private static SoundId find(String id) {
        for (SoundId s : SoundId.values()) if (s.id.equals(id) || s.name().equalsIgnoreCase(id)) return s;
        return null;
    }

    private Sound build(Entry e, float pitchMul) {
        float pitch = e.pitch * pitchMul;
        if (e.variance > 0) pitch += (float) ((ThreadLocalRandom.current().nextDouble() * 2 - 1) * e.variance);
        return Sound.sound(e.key, Sound.Source.MASTER, e.volume, Math.max(0.5f, Math.min(2f, pitch)));
    }

    public void play(Player p, SoundId id, Location at) {
        if (p == null || at == null) return;
        p.playSound(build(entries.get(id), 1f), at.getX(), at.getY(), at.getZ());
    }

    public void play(Collection<? extends Player> players, SoundId id, Location at) {
        for (Player p : players) play(p, id, at);
    }

    /** Dźwięk "przy uchu" gracza. */
    public void play(Player p, SoundId id) {
        play(p, id, 1f);
    }

    public void play(Player p, SoundId id, float pitchMul) {
        if (p == null) return;
        p.playSound(build(entries.get(id), pitchMul), Sound.Emitter.self());
    }
}
