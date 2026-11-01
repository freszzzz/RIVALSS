package pl.rivals.combat;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;
import pl.rivals.shared.SoundId;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Pocisk symulowany w pluginie: grawitacja, opór, odbicia, kolizje z blokami, przeciwnikiem,
 * atrapami i tarczami. Model (item_display) jest przesuwany co 2 ticki z interpolacją teleportu.
 */
public class SimProjectile implements MatchObject {

    public enum Result { CONTINUE, STOP, STICK }

    @FunctionalInterface
    public interface HitHandler {
        /** target/decoy/block - dokładnie jedno niepuste. */
        Result onHit(SimProjectile p, Combatant target, Entity decoy, Block block, BlockFace face, Vector point);
    }

    public final Match match;
    public final Combatant owner;
    public final World world;
    public Vector pos;
    public Vector vel;
    public double gravity;
    public double drag = 0.99;
    public double radius = 0.2;
    public int age;
    public int maxAge = 100;
    public int bounces;
    public double bounceDamp = 0.45;
    public boolean hitEntities = true;
    public boolean stuck;

    public ItemDisplay display;
    public float displayScale = 1f;
    public boolean faceVelocity;
    public float spinDegPerTick;
    public Vector3f spinAxis = new Vector3f(1, 0, 0);
    private final Quaternionf spin = new Quaternionf();

    public HitHandler onHit;
    public Consumer<SimProjectile> onTick;
    public Consumer<SimProjectile> onExpire;
    private boolean dead;

    public SimProjectile(Match match, Combatant owner, Location start, Vector velocity) {
        this.match = match;
        this.owner = owner;
        this.world = start.getWorld();
        this.pos = start.toVector();
        this.vel = velocity.clone();
    }

    public Location location() {
        return pos.toLocation(world);
    }

    public void kill() {
        dead = true;
    }

    public boolean dead() {
        return dead;
    }

    @Override
    public boolean tick() {
        if (dead) return false;
        age++;
        if (age > maxAge) {
            if (onExpire != null) onExpire.accept(this);
            return false;
        }
        if (!stuck) step();
        if (dead) return false;
        if (onTick != null) onTick.accept(this);
        if (dead) return false;
        if (age % 2 == 0) updateDisplay();
        return true;
    }

    private void step() {
        vel.setY(vel.getY() - gravity);
        vel.multiply(drag);
        double len = vel.length();
        if (len < 1e-6) return;
        Vector dir = vel.clone().multiply(1 / len);
        Location from = location();

        double best = len;
        Vector point = null;
        Block block = null;
        BlockFace face = null;
        RayTraceResult rb = world.rayTraceBlocks(from, dir, len + radius, FluidCollisionMode.NEVER, true);
        if (rb != null && rb.getHitBlock() != null) {
            best = Math.max(0, rb.getHitPosition().distance(pos) - radius);
            point = rb.getHitPosition();
            block = rb.getHitBlock();
            face = rb.getHitBlockFace();
        }
        MatchEffects.Shield shield = null;
        for (MatchEffects.Shield s : match.effects().shields()) {
            if (s.owner == owner) continue;
            RayTraceResult r = s.box.rayTrace(pos, dir, best);
            if (r != null) {
                best = r.getHitPosition().distance(pos);
                point = r.getHitPosition();
                shield = s;
                block = null;
            }
        }
        Combatant target = null;
        Entity decoy = null;
        if (hitEntities) {
            Combatant opp = owner.opponent();
            LivingEntity oe = opp == null ? null : opp.entity();
            if (oe != null && oe.isValid() && !oe.isDead()) {
                RayTraceResult r = oe.getBoundingBox().expand(radius).rayTrace(pos, dir, best);
                if (r != null) {
                    best = r.getHitPosition().distance(pos);
                    point = r.getHitPosition();
                    target = opp;
                    shield = null;
                    block = null;
                }
            }
            for (UUID id : match.effects().decoyIds()) {
                Entity e = Bukkit.getEntity(id);
                if (e == null) continue;
                RayTraceResult r = e.getBoundingBox().expand(radius).rayTrace(pos, dir, best);
                if (r != null) {
                    best = r.getHitPosition().distance(pos);
                    point = r.getHitPosition();
                    decoy = e;
                    target = null;
                    shield = null;
                    block = null;
                }
            }
        }

        if (shield != null) {
            Fx.particle(match.viewers(), Particle.ELECTRIC_SPARK, point.toLocation(world), 12, 0.15, 0.15, 0.15, 0.2, null);
            match.plugin().sounds().play(match.viewers(), SoundId.SHIELD_BLOCK, point.toLocation(world));
            if (shield.onHit != null) shield.onHit.run();
            dead = true;
            return;
        }
        if (target != null || decoy != null) {
            Result res = onHit == null ? Result.STOP : onHit.onHit(this, target, decoy, null, null, point);
            if (decoy != null && target == null) Combat.hitDecoy(match, decoy, owner);
            apply(res, point, dir);
            if (res == Result.CONTINUE) pos.add(vel);
            return;
        }
        if (block != null) {
            if (bounces > 0 && face != null) {
                bounces--;
                Vector n = face.getDirection();
                double dot = vel.dot(n);
                vel.subtract(n.clone().multiply(2 * dot)).multiply(bounceDamp);
                pos = point.clone().add(n.multiply(radius + 0.02));
                match.plugin().sounds().play(match.viewers(), SoundId.IMPACT, location());
                return;
            }
            Result res = onHit == null ? Result.STOP : onHit.onHit(this, null, null, block, face, point);
            if (res == Result.CONTINUE) {
                pos = point.clone().subtract(dir.clone().multiply(radius + 0.05));
            } else {
                apply(res, point.clone().subtract(dir.clone().multiply(0.05)), dir);
            }
            return;
        }
        pos.add(vel);
    }

    private void apply(Result res, Vector point, Vector dir) {
        switch (res) {
            case STOP -> dead = true;
            case STICK -> {
                stuck = true;
                pos = point.clone();
                vel.zero();
                updateDisplay();
            }
            case CONTINUE -> {
            }
        }
    }

    public void updateDisplay() {
        if (display == null || !display.isValid()) return;
        Location l = location();
        if (faceVelocity && vel.lengthSquared() > 1e-4) l.setDirection(vel);
        display.teleport(l);
        if (spinDegPerTick != 0 && !stuck) {
            spin.rotateAxis((float) Math.toRadians(spinDegPerTick * 2), spinAxis.x, spinAxis.y, spinAxis.z);
            Displays.animate(display, 2, Displays.trs(new Vector3f(), new Quaternionf(spin), displayScale));
        }
    }

    @Override
    public void remove() {
        dead = true;
        Displays.remove(display);
    }
}
