package pl.rivals.combat;

import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import pl.rivals.match.Combatant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Wszystkie obiekty bieżącej rundy jednego meczu + rejestr tarcz i atrap. */
public final class MatchEffects {

    /** Aktywna tarcza energetyczna (blokuje pociski). */
    public static final class Shield {
        public final BoundingBox box;
        public final Combatant owner;
        public Runnable onHit;

        public Shield(BoundingBox box, Combatant owner) {
            this.box = box;
            this.owner = owner;
        }
    }

    private final List<MatchObject> objects = new ArrayList<>();
    private final List<MatchObject> pending = new ArrayList<>();
    private final List<Shield> shields = new ArrayList<>();
    private final Map<UUID, Consumer<Combatant>> decoys = new HashMap<>();
    private boolean ticking;

    public void add(MatchObject o) {
        if (ticking) pending.add(o);
        else objects.add(o);
    }

    public void tick() {
        ticking = true;
        try {
            Iterator<MatchObject> it = objects.iterator();
            while (it.hasNext()) {
                MatchObject o = it.next();
                boolean alive;
                try {
                    alive = o.tick();
                } catch (RuntimeException ex) {
                    alive = false;
                }
                if (!alive) {
                    safeRemove(o);
                    it.remove();
                }
            }
        } finally {
            ticking = false;
        }
        objects.addAll(pending);
        pending.clear();
    }

    public void notifyDamage(Combatant victim, double dmg) {
        for (MatchObject o : new ArrayList<>(objects)) o.onDamaged(victim, dmg);
    }

    /** Koniec rundy - usuwa wszystko. */
    public void clear() {
        for (MatchObject o : objects) safeRemove(o);
        for (MatchObject o : pending) safeRemove(o);
        objects.clear();
        pending.clear();
        shields.clear();
        decoys.clear();
    }

    private static void safeRemove(MatchObject o) {
        try {
            o.remove();
        } catch (RuntimeException ignored) {
        }
    }

    public List<Shield> shields() {
        return shields;
    }

    public void addShield(Shield s) {
        shields.add(s);
    }

    public void removeShield(Shield s) {
        shields.remove(s);
    }

    /** Rejestruje atrapę: reakcja na trafienie przez przeciwnika. */
    public void addDecoy(Entity e, Consumer<Combatant> onHit) {
        decoys.put(e.getUniqueId(), onHit);
    }

    public void removeDecoy(Entity e) {
        decoys.remove(e.getUniqueId());
    }

    public Consumer<Combatant> decoy(Entity e) {
        return e == null ? null : decoys.get(e.getUniqueId());
    }

    public Iterable<UUID> decoyIds() {
        return decoys.keySet();
    }
}
