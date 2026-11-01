package pl.rivals.gadget;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.FluidCollisionMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.combat.Combat;
import pl.rivals.combat.Cooldowns;
import pl.rivals.combat.Displays;
import pl.rivals.combat.MatchObject;
import pl.rivals.combat.SimProjectile;
import pl.rivals.data.PlayerProfile;
import pl.rivals.item.ItemFactory;
import pl.rivals.match.Combatant;
import pl.rivals.shared.Cosmetic;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.util.HashMap;
import java.util.Map;

/** Gadżety RUCH. */
public final class MovementGadgets {

    private MovementGadgets() {
    }

    // 6. HAK Z LINKĄ
    public static final class Grapple extends Gadget {
        public Grapple() {
            super(RivalsItem.GRAPPLE);
        }

        @Override
        public boolean use(GadgetUse u) {
            Location eye = u.eye();
            RayTraceResult r = eye.getWorld().rayTraceBlocks(eye, u.dir(), u.stat("range"), FluidCollisionMode.NEVER, true);
            if (r == null) {
                u.plugin().sounds().play(u.player(), SoundId.UI_ERROR);
                return false;
            }
            u.match().effects().add(new Hook(u, r.getHitPosition().toLocation(eye.getWorld())));
            u.sound(SoundId.GRAPPLE_FIRE);
            return true;
        }
    }

    static final class Hook implements MatchObject {
        private final GadgetUse u;
        private final Location point;
        private final ItemDisplay head;
        private final ChainLine rope;
        private final int flyTicks;
        private int age;

        Hook(GadgetUse u, Location point) {
            this.u = u;
            this.point = point;
            Location start = ControlGadgets.Anchor.hand(u.player());
            start.setDirection(point.toVector().subtract(start.toVector()));
            this.head = Displays.world(u.plugin(), start, WorldModel.GRAPPLE_HOOK, u.viewers(), 1f);
            this.rope = new ChainLine(u.plugin(), WorldModel.ROPE, u.viewers(), 0.9, 1f);
            this.flyTicks = Math.max(2, (int) Math.ceil(start.distance(point) / 3.0));
        }

        @Override
        public boolean tick() {
            age++;
            Player p = u.player();
            if (!p.isOnline() || age > flyTicks + 30) return false;
            Location hand = ControlGadgets.Anchor.hand(p);
            if (age <= flyTicks) {
                double t = age / (double) flyTicks;
                Location cur = hand.clone().add(point.toVector().subtract(hand.toVector()).multiply(t));
                cur.setDirection(point.toVector().subtract(hand.toVector()));
                head.teleport(cur);
                rope.update(hand, cur);
                if (age == flyTicks) {
                    Fx.particle(u.viewers(), Particle.CRIT, point, 10, 0.1, 0.1, 0.1, 0.2, null);
                    u.sound(SoundId.GRAPPLE_PULL, point);
                }
                return true;
            }
            if (p.isSneaking()) return false;
            Vector d = point.toVector().subtract(p.getLocation().toVector().add(new Vector(0, 1, 0)));
            if (d.length() < 1.8) return false;
            Vector v = d.normalize().multiply(u.stat("pull-speed"));
            v.setY(v.getY() + 0.12);
            p.setVelocity(v);
            p.setFallDistance(0);
            rope.update(hand, point);
            return true;
        }

        @Override
        public void remove() {
            Displays.remove(head);
            rope.remove();
        }
    }

    // 7. RZUCANA TRAMPOLINA
    public static final class Trampoline extends Gadget {
        public Trampoline() {
            super(RivalsItem.TRAMPOLINE);
        }

        @Override
        public boolean use(GadgetUse u) {
            SimProjectile p = u.throwModel(WorldModel.TRAMPOLINE_FOLDED, 0.9, 0.06, 0.7f);
            p.maxAge = 60;
            p.hitEntities = false;
            p.spinDegPerTick = 18;
            p.spinAxis = new Vector3f(0, 1, 0);
            p.onHit = (proj, target, decoy, block, face, point) -> {
                deploy(u, point.toLocation(proj.world));
                return SimProjectile.Result.STOP;
            };
            p.onExpire = proj -> deploy(u, proj.location());
            u.sound(SoundId.FLASH_THROW);
            return true;
        }

        private void deploy(GadgetUse u, Location around) {
            RayTraceResult down = around.getWorld().rayTraceBlocks(around.clone().add(0, 0.3, 0), new Vector(0, -1, 0), 8,
                    FluidCollisionMode.NEVER, true);
            if (down == null) return;
            Location floor = down.getHitPosition().toLocation(around.getWorld());
            floor.setYaw(u.player().getLocation().getYaw());
            floor.setPitch(0);
            u.match().effects().add(new Pad(u, floor));
        }
    }

    static final class Pad implements MatchObject {
        private final GadgetUse u;
        private final Location floor;
        private final ItemDisplay display;
        private final int duration;
        private final Map<Combatant, Integer> lastBounce = new HashMap<>();
        private int age;

        Pad(GadgetUse u, Location floor) {
            this.u = u;
            this.floor = floor;
            this.duration = u.ticks("duration");
            display = Displays.world(u.plugin(), floor.clone().add(0, 0.5, 0), WorldModel.TRAMPOLINE_DEPLOYED, u.viewers(), 1f);
            display.setTransformation(Displays.trs(new Vector3f(0, -0.45f, 0), new Quaternionf(), new Vector3f(0.3f, 0.1f, 0.3f)));
            u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () -> {
                // sprężyna: przeskok ponad rozmiar i powrót
                Displays.animate(display, 4, Displays.trs(new Vector3f(), new Quaternionf(), new Vector3f(1.05f, 1.25f, 1.05f)));
            }, 1L);
            u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () ->
                    Displays.animate(display, 3, Displays.trs(new Vector3f(), new Quaternionf(), 1f)), 6L);
            u.sound(SoundId.TRAMPOLINE_DEPLOY, floor);
        }

        @Override
        public boolean tick() {
            age++;
            if (age == duration) {
                Displays.animate(display, 5, Displays.trs(new Vector3f(0, -0.45f, 0), new Quaternionf(), new Vector3f(0.05f, 0.05f, 0.05f)));
            }
            if (age >= duration) return age < duration + 6;
            for (Combatant c : new Combatant[]{u.match().a(), u.match().b()}) {
                LivingEntity e = c.entity();
                if (e == null) continue;
                Location l = e.getLocation();
                double dx = l.getX() - floor.getX(), dz = l.getZ() - floor.getZ();
                double dy = l.getY() - floor.getY();
                if (Math.abs(dx) > 0.85 || Math.abs(dz) > 0.85 || dy < -0.1 || dy > 0.7) continue;
                if (e.getVelocity().getY() > 0.1) continue;
                if (age - lastBounce.getOrDefault(c, -100) < 6) continue;
                lastBounce.put(c, age);
                Vector v = e.getVelocity();
                v.setY(u.stat("launch"));
                e.setVelocity(v);
                e.setFallDistance(0);
                Displays.animate(display, 2, Displays.trs(new Vector3f(0, -0.1f, 0), new Quaternionf(), new Vector3f(1.1f, 0.6f, 1.1f)));
                u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () ->
                        Displays.animate(display, 4, Displays.trs(new Vector3f(), new Quaternionf(), 1f)), 3L);
                u.sound(SoundId.TRAMPOLINE_BOUNCE, floor);
                Fx.particle(u.viewers(), Particle.CLOUD, floor.clone().add(0, 0.5, 0), 8, 0.4, 0.05, 0.4, 0.02, null);
            }
            return true;
        }

        @Override
        public void remove() {
            Displays.remove(display);
        }
    }

    // 8. KRYSZTAŁ POWROTU
    public static final class Recall extends Gadget {
        private final Map<Combatant, Marker> markers = new HashMap<>();

        public Recall() {
            super(RivalsItem.RECALL);
        }

        @Override
        public boolean use(GadgetUse u) {
            Marker m = markers.get(u.user());
            if (m != null && m.active()) {
                m.warp();
                return true;
            }
            Marker created = new Marker(this, u);
            markers.put(u.user(), created);
            u.match().effects().add(created);
            return false;
        }

        void forget(Combatant c, Marker m) {
            markers.remove(c, m);
        }
    }

    /** Hologram gracza z kryształem; drugie użycie w oknie czasu = teleport + leczenie. */
    static final class Marker implements MatchObject {
        private final Recall gadget;
        private final GadgetUse u;
        private final Location at;
        private final ItemDisplay body;
        private final ItemDisplay crystal;
        private final int window;
        private int age;
        private boolean finished;

        Marker(Recall gadget, GadgetUse u) {
            this.gadget = gadget;
            this.u = u;
            this.at = u.player().getLocation();
            this.window = u.ticks("window");
            Location bodyLoc = at.clone().add(0, 0.5, 0);
            bodyLoc.setPitch(0);
            body = Displays.world(u.plugin(), bodyLoc, WorldModel.HOLOGRAM_BODY, u.viewers(), 1f);
            body.setBrightness(new Display.Brightness(15, 15));
            crystal = Displays.item(u.plugin(), at.clone().add(0, 2.5, 0), RivalsItem.RECALL, u.viewers(), 0.8f);
            crystal.setItemStack(ItemFactory.model("rivals:recall_crystal_active"));
            setHeld(true);
            Fx.particle(u.viewers(), Particle.END_ROD, at.clone().add(0, 1, 0), 15, 0.3, 0.8, 0.3, 0.02, null);
            u.sound(SoundId.RECALL_MARK, at);
        }

        boolean active() {
            return !finished && age < window;
        }

        private void setHeld(boolean active) {
            Player p = u.player();
            int slot = Cooldowns.findSlot(p, RivalsItem.RECALL);
            if (slot < 0) return;
            ItemStack it = active
                    ? ItemFactory.create(u.plugin(), RivalsItem.RECALL, null, "rivals:recall_crystal_active")
                    : ItemFactory.create(u.plugin(), RivalsItem.RECALL, null);
            p.getInventory().setItem(slot, it);
        }

        void warp() {
            Player p = u.player();
            Location from = p.getLocation();
            Location to = at.clone();
            to.setYaw(from.getYaw());
            to.setPitch(from.getPitch());
            Fx.particle(u.viewers(), Particle.REVERSE_PORTAL, from.clone().add(0, 1, 0), 40, 0.3, 0.8, 0.3, 0.05, null);
            p.teleport(to);
            p.setFallDistance(0);
            AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
            double mh = max == null ? 20 : max.getValue();
            p.setHealth(Math.min(mh, p.getHealth() + u.stat("heal")));
            Fx.particle(u.viewers(), Particle.HEART, to.clone().add(0, 2, 0), 4, 0.3, 0.2, 0.3, 0, null);
            Fx.particle(u.viewers(), Particle.REVERSE_PORTAL, to.clone().add(0, 1, 0), 40, 0.3, 0.8, 0.3, 0.05, null);
            u.sound(SoundId.RECALL_WARP, to);
            finish(false);
        }

        private void finish(boolean startCooldown) {
            if (finished) return;
            finished = true;
            setHeld(false);
            if (startCooldown) Cooldowns.start(u.user(), RivalsItem.RECALL, gadget.cooldownTicks(u.plugin()));
            Displays.animate(body, 5, Displays.trs(new Vector3f(), new Quaternionf(), 0.01f));
            Displays.animate(crystal, 5, Displays.trs(new Vector3f(), new Quaternionf(), 0.01f));
        }

        @Override
        public boolean tick() {
            age++;
            if (!finished && age >= window) finish(true);
            if (finished) return age < window + 6 && body.isValid();
            if (age % 10 == 1) {
                float a = (float) Math.toRadians((age / 10) * 120.0);
                float bob = (float) Math.sin(age / 10.0) * 0.1f;
                Displays.animate(crystal, 10, Displays.trs(new Vector3f(0, bob, 0), new Quaternionf().rotationY(a), 0.8f));
            }
            return true;
        }

        @Override
        public void remove() {
            if (!finished) {
                finished = true;
                setHeld(false);
            }
            gadget.forget(u.user(), this);
            Displays.remove(body);
            Displays.remove(crystal);
        }
    }

    // 9. RĘKAWICA DOSKOKU
    public static final class Dash extends Gadget {
        public Dash() {
            super(RivalsItem.DASH);
        }

        @Override
        public boolean use(GadgetUse u) {
            Player p = u.player();
            Vector dir = p.getLocation().getDirection();
            dir.setY(0);
            if (dir.lengthSquared() < 1e-4) dir = new Vector(0, 0, 1);
            dir.normalize();
            Vector v = dir.multiply(u.stat("power"));
            v.setY(Combat.onGround(p) ? 0.22 : 0.08);
            p.setVelocity(v);
            p.setFallDistance(0);
            Color color = trailColor(u);
            u.match().effects().add(new MatchObject() {
                int age;

                @Override
                public boolean tick() {
                    age++;
                    Location l = p.getLocation().add(0, 0.9, 0);
                    Fx.particle(u.viewers(), Particle.DUST, l, 6, 0.25, 0.4, 0.25, 0, new Particle.DustOptions(color, 1.3f));
                    Fx.particle(u.viewers(), Particle.END_ROD, l, 1, 0.1, 0.2, 0.1, 0.01, null);
                    return age < 8 && p.isOnline();
                }

                @Override
                public void remove() {
                }
            });
            u.sound(SoundId.DASH_USE);
            return true;
        }

        private static Color trailColor(GadgetUse u) {
            PlayerProfile prof = u.plugin().profiles().get(u.player().getUniqueId());
            Cosmetic c = prof == null ? null : Cosmetic.byId(prof.trail);
            return c == null ? Color.fromRGB(0x38E8F0) : Color.fromRGB(c.color);
        }
    }
}
