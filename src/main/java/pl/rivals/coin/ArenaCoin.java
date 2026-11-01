package pl.rivals.coin;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.arena.ArenaInstance;
import pl.rivals.arena.BlockVec;
import pl.rivals.arena.Blueprint;
import pl.rivals.combat.Displays;
import pl.rivals.combat.MatchObject;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;

/**
 * Moneta do zebrania na arenie. Obraca się wokół osi Y i unosi (sinus) - oba ruchy to stany
 * docelowe co 10 ticków, interpolowane przez klienta. Zebrana przyspiesza, leci do gracza i znika z błyskiem.
 */
public final class ArenaCoin implements MatchObject {

    private static final float SCALE = 0.7f;

    private final RivalsPlugin plugin;
    private final Match match;
    private final Location at;
    private final ItemDisplay display;
    private int age;
    private Combatant collector;
    private int collectAge;
    private float angle;

    public ArenaCoin(RivalsPlugin plugin, Match match, Location at) {
        this.plugin = plugin;
        this.match = match;
        this.at = at;
        display = Displays.world(plugin, at, WorldModel.COIN, match.viewers(), 0.01f);
        display.setBrightness(new Display.Brightness(15, 15));
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                Displays.animate(display, 6, Displays.trs(new Vector3f(), new Quaternionf(), SCALE)), 1L);
        Fx.particle(match.viewers(), Particle.END_ROD, at, 12, 0.2, 0.3, 0.2, 0.05, null);
        plugin.sounds().play(match.viewers(), SoundId.COIN_SPAWN, at);
    }

    @Override
    public boolean tick() {
        age++;
        if (!display.isValid()) return false;
        if (collector == null) {
            if (age > 7 && age % 10 == 0) {
                angle += (float) Math.toRadians(90);
                float bob = (float) Math.sin(age / 10.0 * 0.9) * 0.12f;
                Displays.animate(display, 10, Displays.trs(new Vector3f(0, bob, 0), new Quaternionf().rotationY(angle), SCALE));
            }
            double r = plugin.settings().coinPickupRadius;
            for (Combatant c : new Combatant[]{match.a(), match.b()}) {
                LivingEntity e = c.entity();
                if (e == null || c.isBot()) continue;
                if (e.getLocation().add(0, 0.8, 0).distance(at) <= r) {
                    collector = c;
                    break;
                }
            }
            return true;
        }
        collectAge++;
        LivingEntity e = collector.entity();
        if (e == null) return false;
        if (collectAge == 1 || collectAge == 3) {
            // szybszy obrót i lot do gracza
            angle += (float) Math.toRadians(170);
            Vector3f to = e.getLocation().add(0, 1.0, 0).toVector().subtract(at.toVector()).toVector3f();
            float s = collectAge == 1 ? SCALE * 0.8f : SCALE * 0.3f;
            Vector3f t = collectAge == 1 ? new Vector3f(to).mul(0.5f) : to;
            Displays.animate(display, 2, Displays.trs(t, new Quaternionf().rotationY(angle), s));
        }
        if (collectAge >= 5) {
            Location l = e.getLocation().add(0, 1.0, 0);
            Fx.particle(match.viewers(), Particle.END_ROD, l, 10, 0.2, 0.2, 0.2, 0.08, null);
            Fx.particle(match.viewers(), Particle.WAX_ON, l, 6, 0.3, 0.3, 0.3, 0.5, null);
            plugin.sounds().play(match.viewers(), SoundId.COIN_PICKUP, l);
            collector.coinsEarned += plugin.settings().coinPickupValue;
            return false;
        }
        return true;
    }

    @Override
    public void remove() {
        Displays.remove(display);
    }

    /** Losuje symetryczne pozycje monet: na otwartej podłodze, z dala od stref startowych. */
    public static List<Location> positions(ArenaInstance arena, int count) {
        List<Location> out = new ArrayList<>();
        if (count <= 0) return out;
        Blueprint bp = arena.template().blueprint();
        int half = arena.template().half();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 200 && out.size() < count; attempt++) {
            int x = r.nextInt(-half + 3, half - 2);
            int z = -r.nextInt(3, half - 2);
            if (Math.abs(x) <= 2 && Math.abs(z - (-arena.template().spawnDistance())) <= 2) continue;
            if (!open(bp, x, z)) continue;
            out.add(center(arena, x, z));
            if (out.size() < count && open(bp, x, -z)) out.add(center(arena, x, -z));
        }
        return out;
    }

    private static boolean open(Blueprint bp, int x, int z) {
        BlockData floor = bp.get(new BlockVec(x, 0, z));
        return floor != null && bp.get(new BlockVec(x, 1, z)) == null && bp.get(new BlockVec(x, 2, z)) == null;
    }

    private static Location center(ArenaInstance arena, int x, int z) {
        BlockVec v = arena.abs(new BlockVec(x, 1, z));
        return new Location(arena.world(), v.x() + 0.5, v.y() + 0.75, v.z() + 0.5);
    }
}
