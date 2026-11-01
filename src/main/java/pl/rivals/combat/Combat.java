package pl.rivals.combat;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import pl.rivals.anim.Fx;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/** Wspólna logika walki: obrażenia, raycast (z tarczami i atrapami), eksplozje, odrzut. */
public final class Combat {

    private Combat() {
    }

    public enum Kind { MELEE, RANGED, RANGED_HEAD, EXPLOSION, GADGET, SELF }

    /**
     * Zadaje obrażenia przez normalny system zdarzeń (MatchListener liczy statystyki, HUD i zabójstwa).
     * @return true, jeśli obrażenia zostały zadane
     */
    public static boolean damage(Match m, Combatant attacker, Combatant victim, double amount, Kind kind) {
        if (m == null || !m.fighting() || victim == null || amount <= 0) return false;
        LivingEntity v = victim.entity();
        if (v == null || v.isDead()) return false;
        m.pendingKind(kind);
        try {
            v.setNoDamageTicks(0);
            LivingEntity src = attacker != null && attacker != victim ? attacker.entity() : null;
            if (src != null) v.damage(amount, src);
            else v.damage(amount);
        } finally {
            m.pendingKind(null);
        }
        return true;
    }

    // raycast
    public record RayHit(Combatant target, Entity decoy, MatchEffects.Shield shield, Vector point, boolean head, double distance) {
        public boolean blocked() {
            return shield != null;
        }
    }

    /** Promień od oczu strzelca - najbliższe z: blok, tarcza, przeciwnik, atrapa. */
    public static RayHit ray(Match m, Combatant shooter, Location eye, Vector dir, double range) {
        World w = eye.getWorld();
        Vector start = eye.toVector();
        Vector d = dir.clone().normalize();
        double best = range;
        Vector point = start.clone().add(d.clone().multiply(range));

        RayTraceResult blocks = w.rayTraceBlocks(eye, d, range, FluidCollisionMode.NEVER, true);
        if (blocks != null) {
            best = blocks.getHitPosition().distance(start);
            point = blocks.getHitPosition();
        }
        MatchEffects.Shield shieldHit = null;
        for (MatchEffects.Shield s : m.effects().shields()) {
            if (s.owner == shooter) continue;
            RayTraceResult r = s.box.rayTrace(start, d, best);
            if (r != null) {
                best = r.getHitPosition().distance(start);
                point = r.getHitPosition();
                shieldHit = s;
            }
        }
        Combatant target = null;
        boolean head = false;
        Combatant opp = shooter.opponent();
        LivingEntity oe = opp == null ? null : opp.entity();
        if (oe != null && oe.isValid()) {
            RayTraceResult r = oe.getBoundingBox().expand(0.08).rayTrace(start, d, best);
            if (r != null) {
                best = r.getHitPosition().distance(start);
                point = r.getHitPosition();
                target = opp;
                shieldHit = null;
                head = point.getY() >= oe.getEyeLocation().getY() - 0.3;
            }
        }
        Entity decoy = null;
        for (UUID id : m.effects().decoyIds()) {
            Entity e = Bukkit.getEntity(id);
            if (e == null) continue;
            RayTraceResult r = e.getBoundingBox().rayTrace(start, d, best);
            if (r != null) {
                best = r.getHitPosition().distance(start);
                point = r.getHitPosition();
                decoy = e;
                target = null;
                shieldHit = null;
            }
        }
        return new RayHit(target, decoy, shieldHit, point, head, best);
    }

    /** Linia cząsteczek (smuga pocisku). */
    public static void tracer(Collection<Player> viewers, Location from, Vector to, Color color, double step) {
        Vector a = from.toVector();
        Vector dir = to.clone().subtract(a);
        double len = dir.length();
        if (len < 0.01) return;
        dir.normalize();
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.6f);
        for (double t = 0.6; t < len; t += step) {
            Location l = a.clone().add(dir.clone().multiply(t)).toLocation(from.getWorld());
            Fx.particle(viewers, Particle.DUST, l, 1, 0, 0, 0, 0, dust);
        }
    }

    /** Trafienie atrapy przez strzał/pocisk. */
    public static void hitDecoy(Match m, Entity decoy, Combatant by) {
        Consumer<Combatant> c = m.effects().decoy(decoy);
        if (c != null) c.accept(by);
    }

    // eksplozja
    /**
     * Obrażenia obszarowe malejące z odległością + odrzut. Właściciel dostaje selfFactor obrażeń
     * i mocniejszy odrzut (rocket jump).
     */
    public static void explode(Match m, Combatant owner, Location c, double radius, double maxDmg, double minDmg,
                               double selfFactor, double knockback) {
        Collection<Player> vs = m.viewers();
        Fx.particle(vs, Particle.EXPLOSION, c, 2, 0.3, 0.3, 0.3, 0, null);
        Fx.particle(vs, Particle.LARGE_SMOKE, c, 12, 0.6, 0.4, 0.6, 0.02, null);
        for (Combatant t : new Combatant[]{m.a(), m.b()}) {
            LivingEntity e = t.entity();
            if (e == null || !e.isValid()) continue;
            Vector body = e.getLocation().toVector().add(new Vector(0, 0.9, 0));
            double dist = body.distance(c.toVector());
            if (dist > radius) continue;
            double f = 1 - dist / radius;
            double dmg = minDmg + (maxDmg - minDmg) * f;
            Vector kb = body.clone().subtract(c.toVector());
            if (kb.lengthSquared() < 1e-4) kb = new Vector(0, 1, 0);
            kb.normalize().multiply(knockback * (0.4 + 0.6 * f));
            kb.setY(Math.max(kb.getY(), 0.25 + 0.45 * knockback * f));
            if (t == owner) {
                if (selfFactor > 0) damage(m, null, t, dmg * selfFactor, Kind.SELF);
                e.setVelocity(e.getVelocity().add(kb.multiply(1.35)));
            } else {
                damage(m, owner, t, dmg, Kind.EXPLOSION);
                e.setVelocity(e.getVelocity().add(kb));
            }
        }
        for (UUID id : m.effects().decoyIds()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null && e.getLocation().distance(c) <= radius) hitDecoy(m, e, owner);
        }
    }

    /** Czy byt stoi na bloku (sprawdzane po stronie serwera, nie z flagi klienta). */
    public static boolean onGround(Entity e) {
        BoundingBox bb = e.getBoundingBox();
        World w = e.getWorld();
        double y = bb.getMinY() - 0.06;
        for (double x : new double[]{bb.getMinX() + 0.01, bb.getMaxX() - 0.01}) {
            for (double z : new double[]{bb.getMinZ() + 0.01, bb.getMaxZ() - 0.01}) {
                if (w.getBlockAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)).getType().isSolid()) return true;
            }
        }
        return false;
    }
}
