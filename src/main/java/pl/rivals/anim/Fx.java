package pl.rivals.anim;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.function.Consumer;

/** Efekty widoczne tylko dla wskazanych graczy (gracze danej areny). */
public final class Fx {

    private Fx() {
    }

    /**
     * Tworzy byt ukryty domyślnie i pokazuje go tylko widzom. Byt nie jest zapisywany na dysk.
     * init wykonuje się przed dodaniem bytu do świata, więc pierwszy pakiet ma już właściwy stan.
     */
    public static <T extends Entity> T spawnFor(Plugin plugin, Location loc, Class<T> type,
                                                Collection<? extends Player> viewers, Consumer<T> init) {
        T entity = loc.getWorld().spawn(loc, type, e -> {
            e.setPersistent(false);
            e.setVisibleByDefault(false);
            init.accept(e);
        });
        for (Player p : viewers) p.showEntity(plugin, entity);
        return entity;
    }

    public static <T> void particle(Collection<? extends Player> viewers, Particle particle, Location loc, int count,
                                    double dx, double dy, double dz, double speed, T data) {
        for (Player p : viewers) {
            if (data == null) p.spawnParticle(particle, loc, count, dx, dy, dz, speed);
            else p.spawnParticle(particle, loc, count, dx, dy, dz, speed, data);
        }
    }
}
