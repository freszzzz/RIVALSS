package pl.rivals.gadget;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.combat.Combat;
import pl.rivals.combat.Displays;
import pl.rivals.combat.MatchObject;
import pl.rivals.combat.SimProjectile;
import pl.rivals.item.ItemFactory;
import pl.rivals.match.Combatant;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

/** Gadżety OBRAŻENIA. */
public final class DamageGadgets {

    private DamageGadgets() {
    }

    // 10. BUMERANG-OSTRZE
    public static final class Boomerang extends Gadget {
        public Boomerang() {
            super(RivalsItem.BOOMERANG);
        }

        @Override
        public boolean use(GadgetUse u) {
            Vector forward = u.dir();
            Vector side = forward.clone().crossProduct(new Vector(0, 1, 0));
            if (side.lengthSquared() < 1e-4) side = new Vector(1, 0, 0);
            side.normalize();
            double speed = 1.1;
            int outTicks = Math.max(4, (int) Math.round(u.stat("range") / speed));
            SimProjectile p = u.throwItem(speed, 0, 0.9f);
            p.vel = forward.clone().multiply(speed);
            p.drag = 1;
            p.maxAge = outTicks * 3 + 20;
            p.radius = 0.35;
            p.spinDegPerTick = 50;
            p.spinAxis = new Vector3f(0, 1, 0);
            boolean[] returning = {false};
            boolean[] hitOut = {false};
            boolean[] hitBack = {false};
            Vector sideF = side;
            p.onTick = proj -> {
                if (!returning[0] && proj.age >= outTicks) returning[0] = true;
                if (!returning[0]) {
                    // łuk: odchylenie w bok, które rośnie i maleje
                    double t = proj.age / (double) outTicks;
                    proj.vel = forward.clone().multiply(speed).add(sideF.clone().multiply(Math.cos(t * Math.PI) * 0.35));
                } else {
                    Location home = u.player().getLocation().add(0, 1.1, 0);
                    Vector to = home.toVector().subtract(proj.pos);
                    if (to.length() < 1.2 || !u.player().isOnline()) {
                        proj.kill();
                        return;
                    }
                    proj.vel = to.normalize().multiply(speed * 1.15);
                }
                if (proj.age % 3 == 0) {
                    Fx.particle(u.viewers(), Particle.CRIT, proj.location(), 1, 0, 0, 0, 0, null);
                }
            };
            p.onHit = (proj, target, decoy, block, face, point) -> {
                if (target != null) {
                    boolean[] flag = returning[0] ? hitBack : hitOut;
                    if (!flag[0]) {
                        flag[0] = true;
                        Combat.damage(u.match(), u.user(), target, u.stat("damage"), Combat.Kind.GADGET);
                        u.sound(SoundId.BOOMERANG_HIT, point.toLocation(proj.world));
                    }
                    return SimProjectile.Result.CONTINUE;
                }
                if (decoy != null) return SimProjectile.Result.CONTINUE;
                // blok: zawraca
                if (!returning[0]) {
                    returning[0] = true;
                    u.sound(SoundId.IMPACT, point.toLocation(proj.world));
                    return SimProjectile.Result.CONTINUE;
                }
                return SimProjectile.Result.STOP;
            };
            u.sound(SoundId.BOOMERANG_THROW);
            return true;
        }
    }

    // 11. LEPKI GRANAT
    public static final class Sticky extends Gadget {
        public Sticky() {
            super(RivalsItem.STICKY);
        }

        @Override
        public boolean use(GadgetUse u) {
            SimProjectile p = u.throwItem(1.1, 0.05, 0.7f);
            int fuse = u.ticks("fuse");
            p.maxAge = 200;
            p.spinDegPerTick = 20;
            StickyBomb bomb = new StickyBomb(u, p, fuse);
            p.onHit = (proj, target, decoy, block, face, point) -> {
                if (target != null) {
                    bomb.attach(target);
                    return SimProjectile.Result.STICK;
                }
                if (decoy != null) return SimProjectile.Result.STOP;
                u.sound(SoundId.STICKY_STICK, point.toLocation(proj.world));
                bomb.armed = true;
                return SimProjectile.Result.STICK;
            };
            u.match().effects().add(bomb);
            u.sound(SoundId.FLASH_THROW);
            return true;
        }
    }

    /** Odliczanie lepkiego granatu - dioda miga coraz szybciej, wybuch po fuse ticków od przyklejenia. */
    static final class StickyBomb implements MatchObject {
        private final GadgetUse u;
        private final SimProjectile proj;
        private final int fuse;
        boolean armed;
        private Combatant attached;
        private ItemDisplay onBack;
        private int timer;
        private int nextBlink;
        private boolean lit;
        private boolean exploded;

        StickyBomb(GadgetUse u, SimProjectile proj, int fuse) {
            this.u = u;
            this.proj = proj;
            this.fuse = fuse;
        }

        void attach(Combatant target) {
            LivingEntity te = target.entity();
            if (te == null) return;
            attached = target;
            armed = true;
            Displays.remove(proj.display);
            proj.display = null;
            onBack = Displays.item(u.plugin(), te.getLocation(), RivalsItem.STICKY, u.viewers(), 0.7f);
            onBack.setTransformation(Displays.trs(new Vector3f(0, -0.95f, -0.32f), new Quaternionf(), 0.7f));
            te.addPassenger(onBack);
            u.sound(SoundId.STICKY_STICK, te.getLocation());
        }

        @Override
        public boolean tick() {
            if (exploded) return false;
            if (!armed) {
                // nie przykleił się - i tak wybucha po 2× fuse
                if (proj.dead() && !proj.stuck) return false;
                if (++timer > fuse * 2) explode();
                return !exploded;
            }
            timer++;
            ItemDisplay d = attached != null ? onBack : proj.display;
            if (attached != null && onBack != null && onBack.isValid()) {
                LivingEntity te = attached.entity();
                if (te != null && timer % 2 == 0) onBack.setRotation(te.getBodyYaw(), 0);
            }
            if (timer >= nextBlink && d != null && d.isValid()) {
                lit = !lit;
                d.setItemStack(lit ? ItemFactory.model(WorldModel.STICKY_LIT) : ItemFactory.model(RivalsItem.STICKY.modelKey()));
                if (lit) u.sound(SoundId.STICKY_BEEP, location());
                double progress = timer / (double) fuse;
                nextBlink = timer + Math.max(1, (int) Math.round(8 - progress * 7));
            }
            if (timer >= fuse) explode();
            return !exploded;
        }

        private Location location() {
            if (attached != null && attached.entity() != null) return attached.entity().getLocation().add(0, 1.0, 0);
            return proj.location();
        }

        private void explode() {
            if (exploded) return;
            exploded = true;
            Location at = location();
            u.sound(SoundId.STICKY_EXPLODE, at);
            Combat.explode(u.match(), u.user(), at, u.stat("radius"), u.stat("damage-max"), u.stat("damage-min"), 0.5, 0.9);
            proj.kill();
        }

        @Override
        public void remove() {
            exploded = true;
            Displays.remove(onBack);
            proj.remove();
        }
    }

    // 12. WYRZUTNIA RAKIET
    public static final class Rocket extends Gadget {
        public Rocket() {
            super(RivalsItem.ROCKET);
        }

        @Override
        public boolean use(GadgetUse u) {
            SimProjectile p = u.throwModel(WorldModel.ROCKET, u.stat("speed"), 0, 1f);
            p.vel = u.dir().multiply(u.stat("speed"));
            p.drag = 1.0;
            p.faceVelocity = true;
            p.maxAge = 60;
            p.radius = 0.25;
            p.onTick = proj -> {
                Location l = proj.location().subtract(proj.vel.clone().normalize().multiply(0.4));
                Fx.particle(u.viewers(), Particle.CAMPFIRE_COSY_SMOKE, l, 1, 0.02, 0.02, 0.02, 0.005, null);
                Fx.particle(u.viewers(), Particle.FLAME, l, 1, 0.02, 0.02, 0.02, 0.01, null);
            };
            p.onHit = (proj, target, decoy, block, face, point) -> {
                boom(u, point.toLocation(proj.world));
                return SimProjectile.Result.STOP;
            };
            p.onExpire = proj -> boom(u, proj.location());
            u.sound(SoundId.ROCKET_FIRE);
            return true;
        }

        private void boom(GadgetUse u, Location at) {
            u.sound(SoundId.ROCKET_EXPLODE, at);
            Combat.explode(u.match(), u.user(), at, u.stat("radius"), u.stat("damage-max"), u.stat("damage-min"),
                    u.stat("self-damage"), u.stat("knockback"));
        }
    }
}
