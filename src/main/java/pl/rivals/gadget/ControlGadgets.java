package pl.rivals.gadget;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.Color;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.combat.Combat;
import pl.rivals.combat.Displays;
import pl.rivals.combat.MatchObject;
import pl.rivals.combat.SimProjectile;
import pl.rivals.combat.TimedStatus;
import pl.rivals.item.ItemFactory;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;

/** Gadżety KONTROLA. */
public final class ControlGadgets {

    private ControlGadgets() {
    }

    // 1. KRIOGRANAT
    public static final class Cryo extends Gadget {
        public Cryo() {
            super(RivalsItem.CRYO);
        }

        @Override
        public boolean use(GadgetUse u) {
            SimProjectile p = u.throwItem(1.15, 0.05, 0.75f);
            p.bounces = 2;
            p.maxAge = u.ticks("fuse");
            p.spinDegPerTick = 25;
            p.spinAxis = new Vector3f(1, 0.3f, 0.2f).normalize();
            p.onHit = (proj, target, decoy, block, face, point) -> {
                explode(u, point.toLocation(proj.world));
                return SimProjectile.Result.STOP;
            };
            p.onExpire = proj -> explode(u, proj.location());
            p.onTick = proj -> {
                if (proj.age % 2 == 0) Fx.particle(u.viewers(), Particle.SNOWFLAKE, proj.location(), 1, 0, 0, 0, 0, null);
            };
            u.sound(SoundId.CRYO_THROW);
            return true;
        }

        private void explode(GadgetUse u, Location at) {
            Collection<Player> vs = u.viewers();
            Fx.particle(vs, Particle.SNOWFLAKE, at, 50, 1.2, 0.8, 1.2, 0.05, null);
            Fx.particle(vs, Particle.BLOCK, at, 30, 1.0, 0.6, 1.0, 0, Material.ICE.createBlockData());
            u.sound(SoundId.CRYO_EXPLODE, at);
            Combatant t = u.target();
            LivingEntity te = t.entity();
            if (te == null) return;
            double r = u.stat("radius");
            if (te.getLocation().add(0, 0.9, 0).distance(at) > r) return;
            Combat.damage(u.match(), u.user(), t, u.stat("damage"), Combat.Kind.GADGET);
            TimedStatus freeze = TimedStatus.freeze(u.plugin(), te, u.ticks("freeze"));
            u.match().effects().add(freeze);
            u.match().effects().add(new IceEncase(u.plugin(), u.match(), t, freeze));
        }
    }

    /** Bryła lodu wokół zamrożonego - rośnie 0->1 w 4 ticki, pierwsze trafienie ją rozbija. */
    static final class IceEncase implements MatchObject {
        private final RivalsPlugin plugin;
        private final Match match;
        private final Combatant target;
        private final TimedStatus freeze;
        private final BlockDisplay display;
        private final Location base;
        private boolean shattered;

        IceEncase(RivalsPlugin plugin, Match match, Combatant target, TimedStatus freeze) {
            this.plugin = plugin;
            this.match = match;
            this.target = target;
            this.freeze = freeze;
            this.base = target.entity().getLocation();
            base.setYaw(0);
            base.setPitch(0);
            BlockData ice = Material.ICE.createBlockData();
            display = Fx.spawnFor(plugin, base, BlockDisplay.class, match.viewers(), d -> {
                d.setBlock(ice);
                d.setTransformation(shape(0.02f));
                d.setInterpolationDuration(0);
            });
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> Displays.animate(display, 4, shape(1f)), 1L);
        }

        private static Transformation shape(float s) {
            Vector3f size = new Vector3f(1.2f * s, 2.15f * s, 1.2f * s);
            return Displays.trs(new Vector3f(-size.x / 2, 0, -size.z / 2), new Quaternionf(), size);
        }

        @Override
        public boolean tick() {
            return !shattered && freeze.active();
        }

        @Override
        public void onDamaged(Combatant victim, double damage) {
            if (victim == target && !shattered) {
                freeze.end();
                shatter();
            }
        }

        private void shatter() {
            if (shattered) return;
            shattered = true;
            Collection<Player> vs = match.viewers();
            ThreadLocalRandom r = ThreadLocalRandom.current();
            BlockData ice = Material.ICE.createBlockData();
            BlockData packed = Material.PACKED_ICE.createBlockData();
            for (int i = 0; i < 10; i++) {
                Location c = base.clone().add(r.nextDouble(-0.5, 0.5), r.nextDouble(0.1, 2.0), r.nextDouble(-0.5, 0.5));
                Vector3f v = new Vector3f((float) r.nextDouble(-0.15, 0.15), (float) r.nextDouble(0.05, 0.25), (float) r.nextDouble(-0.15, 0.15));
                plugin.fragments().spawn(c, i % 2 == 0 ? ice : packed, v, vs, 0.3f, 0.05f, 22);
            }
            Fx.particle(vs, Particle.BLOCK, base.clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0, ice);
            plugin.sounds().play(vs, SoundId.CRYO_SHATTER, base);
            Displays.remove(display);
        }

        @Override
        public void remove() {
            shatter();
        }
    }

    // 2. KLATKA-MINA
    public static final class Cage extends Gadget {
        public Cage() {
            super(RivalsItem.CAGE);
        }

        @Override
        public boolean use(GadgetUse u) {
            Location eye = u.eye();
            RayTraceResult r = eye.getWorld().rayTraceBlocks(eye, u.dir(), 4.5, FluidCollisionMode.NEVER, true);
            Location at;
            if (r != null && r.getHitBlockFace() == BlockFace.UP) {
                at = r.getHitPosition().toLocation(eye.getWorld());
            } else {
                at = u.player().getLocation();
                if (!Combat.onGround(u.player())) {
                    RayTraceResult down = eye.getWorld().rayTraceBlocks(at, new Vector(0, -1, 0), 6, FluidCollisionMode.NEVER, true);
                    if (down == null) return false;
                    at = down.getHitPosition().toLocation(eye.getWorld());
                }
            }
            at.setYaw(u.player().getLocation().getYaw());
            at.setPitch(0);
            u.match().effects().add(new Mine(u, at));
            u.sound(SoundId.CAGE_PLACE, at);
            return true;
        }
    }

    static final class Mine implements MatchObject {
        private final GadgetUse u;
        private final Location floor;
        private final ItemDisplay disc;
        private final int armTicks;
        private final int lifetime;
        private final double radius;
        private int age;
        private boolean triggered;

        Mine(GadgetUse u, Location floor) {
            this.u = u;
            this.floor = floor;
            this.armTicks = u.ticks("arm");
            this.lifetime = u.ticks("lifetime");
            this.radius = u.stat("trigger-radius");
            Location l = floor.clone().add(0, 1 / 16.0, 0);
            disc = Displays.world(u.plugin(), l, WorldModel.CAGE_MINE_WORLD, u.viewers(), 1f);
        }

        @Override
        public boolean tick() {
            age++;
            if (triggered || age > lifetime) return false;
            if (age == armTicks) {
                // zapada się w podłogę - prawie niewidoczna
                Displays.animate(disc, 6, Displays.trs(new Vector3f(0, -0.07f, 0), new Quaternionf(), new Vector3f(1, 0.3f, 1)));
                u.sound(SoundId.CAGE_PLACE, floor);
            }
            if (age < armTicks) return true;
            LivingEntity te = u.target().entity();
            if (te == null) return true;
            Location tl = te.getLocation();
            double dx = tl.getX() - floor.getX(), dz = tl.getZ() - floor.getZ();
            if (dx * dx + dz * dz <= radius * radius && Math.abs(tl.getY() - floor.getY()) < 1.2) {
                triggered = true;
                trigger(te);
            }
            return !triggered;
        }

        private void trigger(LivingEntity te) {
            int trap = u.ticks("trap");
            TimedStatus freeze = TimedStatus.freeze(u.plugin(), te, trap);
            u.match().effects().add(freeze);
            u.match().effects().add(new CageBars(u, te.getLocation(), trap));
            u.sound(SoundId.CAGE_TRIGGER, te.getLocation());
            Fx.particle(u.viewers(), Particle.ELECTRIC_SPARK, te.getLocation().add(0, 0.2, 0), 20, 0.6, 0.1, 0.6, 0.1, null);
        }

        @Override
        public void remove() {
            Displays.remove(disc);
        }
    }

    /** 8 prętów wysuwa się z podłogi wokół ofiary, po czasie chowa się z powrotem. */
    static final class CageBars implements MatchObject {
        private final List<ItemDisplay> bars = new ArrayList<>();
        private final int duration;
        private int age;

        CageBars(GadgetUse u, Location center, int duration) {
            this.duration = duration;
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location l = center.clone().add(Math.cos(a) * 0.75, 0.5, Math.sin(a) * 0.75);
                l.setYaw(0);
                l.setPitch(0);
                ItemDisplay d = Displays.world(u.plugin(), l, WorldModel.CAGE_BAR, u.viewers(), 1f);
                d.setTransformation(Displays.trs(new Vector3f(0, -2.1f, 0), new Quaternionf(), 1f));
                bars.add(d);
            }
            u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () -> {
                for (ItemDisplay d : bars) Displays.animate(d, 4, Displays.trs(new Vector3f(), new Quaternionf(), 1f));
            }, 1L);
        }

        @Override
        public boolean tick() {
            age++;
            if (age == duration) {
                for (ItemDisplay d : bars) Displays.animate(d, 5, Displays.trs(new Vector3f(0, -2.1f, 0), new Quaternionf(), 1f));
            }
            return age < duration + 6;
        }

        @Override
        public void remove() {
            for (ItemDisplay d : bars) Displays.remove(d);
        }
    }

    // 3. WYRZUTNIA SIECI
    public static final class Net extends Gadget {
        public Net() {
            super(RivalsItem.NET);
        }

        @Override
        public boolean use(GadgetUse u) {
            SimProjectile p = u.throwModel(WorldModel.NET_BALL, u.stat("speed"), 0.02, 0.6f);
            p.maxAge = 40;
            p.radius = 0.45;
            p.onTick = proj -> {
                if (proj.age == 3 && proj.display != null) {
                    // sieć rozwija się w locie
                    proj.display.setItemStack(ItemFactory.model(WorldModel.NET_OPEN));
                    proj.display.setTransformation(Displays.trs(new Vector3f(), new Quaternionf(), 0.3f));
                    proj.displayScale = 1.1f;
                    Displays.animate(proj.display, 4, Displays.trs(new Vector3f(), new Quaternionf(), 1.1f));
                }
            };
            p.onHit = (proj, target, decoy, block, face, point) -> {
                if (target != null) wrap(u, target);
                Fx.particle(u.viewers(), Particle.CRIT, point.toLocation(proj.world), 10, 0.3, 0.3, 0.3, 0.1, null);
                return SimProjectile.Result.STOP;
            };
            u.sound(SoundId.NET_SHOOT);
            return true;
        }

        private void wrap(GadgetUse u, Combatant target) {
            LivingEntity te = target.entity();
            if (te == null) return;
            int dur = u.ticks("duration");
            u.match().effects().add(TimedStatus.slow(u.plugin(), te, u.stat("slow"), dur));
            ItemDisplay net = Displays.world(u.plugin(), te.getLocation(), WorldModel.NET_WRAPPED, u.viewers(), 1f);
            net.setTransformation(Displays.trs(new Vector3f(0, -1.12f, 0), new Quaternionf(), 1f));
            te.addPassenger(net);
            u.match().effects().add(new MatchObject() {
                int age;

                @Override
                public boolean tick() {
                    return ++age < dur && net.isValid();
                }

                @Override
                public void remove() {
                    Displays.remove(net);
                }
            });
            u.sound(SoundId.NET_HIT, te.getLocation());
        }
    }

    // 4. GRANAT OŚLEPIAJĄCY
    public static final class Flash extends Gadget {
        public Flash() {
            super(RivalsItem.FLASH);
        }

        @Override
        public boolean use(GadgetUse u) {
            // w locie model bez zawleczki (odbezpieczony)
            SimProjectile p = u.throwModel(WorldModel.FLASHBANG_ARMED, 1.1, 0.05, 0.6f);
            p.bounces = 3;
            p.maxAge = u.ticks("fuse");
            p.spinDegPerTick = 30;
            p.onHit = (proj, target, decoy, block, face, point) -> {
                bang(u, point.toLocation(proj.world));
                return SimProjectile.Result.STOP;
            };
            p.onExpire = proj -> bang(u, proj.location());
            u.sound(SoundId.FLASH_THROW);
            return true;
        }

        private void bang(GadgetUse u, Location at) {
            Collection<Player> vs = u.viewers();
            Fx.particle(vs, Particle.END_ROD, at, 40, 0.2, 0.2, 0.2, 0.35, null);
            Fx.particle(vs, Particle.DUST, at, 30, 0.6, 0.6, 0.6, 0, new Particle.DustOptions(Color.WHITE, 2f));
            u.sound(SoundId.FLASH_BANG, at);
            Combatant t = u.target();
            LivingEntity te = t.entity();
            if (te == null || te.getLocation().distance(at) > u.stat("radius")) return;
            Location eye = te.getEyeLocation();
            Vector toExp = at.toVector().subtract(eye.toVector());
            double dist = toExp.length();
            boolean los = dist < 0.1 || eye.getWorld().rayTraceBlocks(eye, toExp.clone().normalize(), dist, FluidCollisionMode.NEVER, true) == null;
            double angle = Math.toDegrees(eye.getDirection().angle(toExp));
            boolean full = los && angle < u.stat("angle");
            int ticks = full ? u.ticks("blind-long") : u.ticks("blind-short");
            te.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks + 10, 0, false, false, false));
            Player tp = t.player();
            if (tp != null) u.plugin().hud().titles().flash(tp, ticks);
            if (t.isBot()) {
                t.bot().freeze(true);
                u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () -> {
                    if (u.match().fighting()) {
                        t.bot().freeze(false);
                        t.bot().target(u.user().entity());
                    }
                }, ticks);
            }
        }
    }

    // 5. KOTWICA GRAWITACYJNA
    public static final class Anchor extends Gadget {
        public Anchor() {
            super(RivalsItem.ANCHOR);
        }

        @Override
        public boolean use(GadgetUse u) {
            double speed = u.stat("speed");
            SimProjectile p = u.throwItem(speed, 0.03, 0.8f);
            p.maxAge = (int) Math.ceil(u.stat("range") / speed);
            p.spinDegPerTick = 20;
            p.spinAxis = new Vector3f(0, 0, 1);
            ChainLine chain = new ChainLine(u.plugin(), WorldModel.CHAIN_LINK, u.viewers(), 0.35, 1f);
            Retract retract = new Retract(u, chain);
            u.match().effects().add(guard(chain, 160));
            p.onTick = proj -> chain.update(hand(u.player()), proj.location());
            p.onHit = (proj, target, decoy, block, face, point) -> {
                if (target != null) {
                    u.match().effects().add(new Pull(u, target, chain));
                } else {
                    retract.from = point.toLocation(proj.world);
                    u.match().effects().add(retract);
                }
                return SimProjectile.Result.STOP;
            };
            p.onExpire = proj -> {
                retract.from = proj.location();
                u.match().effects().add(retract);
            };
            u.sound(SoundId.ANCHOR_THROW);
            return true;
        }

        /** Gwarantuje usunięcie łańcucha także po końcu rundy w trakcie lotu. */
        static MatchObject guard(ChainLine chain, int maxTicks) {
            return new MatchObject() {
                int age;

                @Override
                public boolean tick() {
                    return ++age < maxTicks;
                }

                @Override
                public void remove() {
                    chain.remove();
                }
            };
        }

        static Location hand(Player p) {
            Location l = p.getEyeLocation().add(0, -0.4, 0);
            Vector right = l.getDirection().crossProduct(new Vector(0, 1, 0));
            if (right.lengthSquared() > 1e-4) l.add(right.normalize().multiply(0.35));
            return l;
        }
    }

    /** Przyciąga trafionego o "pull" bloków w stronę rzucającego, ściągając go z powietrza. */
    static final class Pull implements MatchObject {
        private final GadgetUse u;
        private final Combatant target;
        private final ChainLine chain;
        private int age;
        private final int duration;

        Pull(GadgetUse u, Combatant target, ChainLine chain) {
            this.u = u;
            this.target = target;
            this.chain = chain;
            this.duration = 6;
            u.sound(SoundId.ANCHOR_PULL, target.entity().getLocation());
        }

        @Override
        public boolean tick() {
            age++;
            LivingEntity te = target.entity();
            if (te == null || age > duration + 3) return false;
            Location hand = Anchor.hand(u.player());
            if (age <= duration) {
                Vector d = u.player().getLocation().toVector().subtract(te.getLocation().toVector());
                d.setY(0);
                if (d.lengthSquared() > 4) {
                    d.normalize().multiply(u.stat("pull") / duration);
                    d.setY(Combat.onGround(te) ? 0.15 : -0.7);
                    te.setVelocity(d);
                }
            }
            chain.update(hand, te.getLocation().add(0, 1, 0));
            return true;
        }

        @Override
        public void remove() {
            chain.remove();
        }
    }

    /** Łańcuch zwija się do ręki, gdy kotwica chybi. */
    static final class Retract implements MatchObject {
        private final GadgetUse u;
        private final ChainLine chain;
        Location from;
        private int age;

        Retract(GadgetUse u, ChainLine chain) {
            this.u = u;
            this.chain = chain;
        }

        @Override
        public boolean tick() {
            age++;
            if (from == null || age > 6) return false;
            Location hand = Anchor.hand(u.player());
            double t = age / 6.0;
            Location cur = from.clone().add(hand.toVector().subtract(from.toVector()).multiply(t));
            chain.update(hand, cur);
            return true;
        }

        @Override
        public void remove() {
            chain.remove();
        }
    }
}
