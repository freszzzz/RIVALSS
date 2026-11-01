package pl.rivals.gadget;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.FluidCollisionMode;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.combat.Combat;
import pl.rivals.combat.Displays;
import pl.rivals.combat.MatchEffects;
import pl.rivals.combat.MatchObject;
import pl.rivals.combat.SimProjectile;
import pl.rivals.combat.TimedStatus;
import pl.rivals.item.ItemFactory;
import pl.rivals.match.Combatant;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/** Gadżety OBRONA. */
public final class DefenseGadgets {

    private DefenseGadgets() {
    }

    // 13. ROZKŁADANA TARCZA
    public static final class Shield extends Gadget {
        public Shield() {
            super(RivalsItem.SHIELD);
        }

        @Override
        public boolean use(GadgetUse u) {
            SimProjectile p = u.throwItem(0.8, 0.08, 0.7f);
            p.maxAge = 30;
            p.hitEntities = false;
            p.spinDegPerTick = 25;
            p.spinAxis = new Vector3f(0, 1, 0);
            float yaw = snap(u.player().getLocation().getYaw());
            p.onHit = (proj, target, decoy, block, face, point) -> {
                deploy(u, point.toLocation(proj.world), yaw);
                return SimProjectile.Result.STOP;
            };
            p.onExpire = proj -> deploy(u, proj.location(), yaw);
            u.sound(SoundId.FLASH_THROW);
            return true;
        }

        private static float snap(float yaw) {
            return Math.round(yaw / 90f) * 90f;
        }

        private void deploy(GadgetUse u, Location around, float yaw) {
            RayTraceResult down = around.getWorld().rayTraceBlocks(around.clone().add(0, 0.3, 0), new Vector(0, -1, 0), 8,
                    FluidCollisionMode.NEVER, true);
            if (down == null) return;
            Location floor = down.getHitPosition().toLocation(around.getWorld());
            u.match().effects().add(new Wall(u, floor, yaw));
        }
    }

    /** Ściana 3x2 z animowanej "energii": rozkłada się, blokuje pociski, pęka i rozsypuje. */
    static final class Wall implements MatchObject {
        private final GadgetUse u;
        private final ItemDisplay display;
        private final MatchEffects.Shield shield;
        private final Location center;
        private final int duration;
        private int age;
        private boolean broken;

        Wall(GadgetUse u, Location floor, float yaw) {
            this.u = u;
            this.duration = u.ticks("duration");
            this.center = floor.clone().add(0, 1.0, 0);
            Location l = center.clone();
            l.setYaw(yaw);
            l.setPitch(0);
            display = Displays.world(u.plugin(), l, WorldModel.SHIELD_WALL, u.viewers(), 1f);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setTransformation(Displays.trs(new Vector3f(), new Quaternionf(), new Vector3f(0.05f, 0.05f, 1f)));
            u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () ->
                    Displays.animate(display, 6, Displays.trs(new Vector3f(), new Quaternionf(), 1f)), 1L);
            boolean alongX = Math.round(yaw / 90f) % 2 == 0; // patrzy na N/S → ściana rozciąga się wzdłuż X
            BoundingBox box = alongX
                    ? new BoundingBox(center.getX() - 1.5, center.getY() - 1, center.getZ() - 0.25,
                            center.getX() + 1.5, center.getY() + 1, center.getZ() + 0.25)
                    : new BoundingBox(center.getX() - 0.25, center.getY() - 1, center.getZ() - 1.5,
                            center.getX() + 0.25, center.getY() + 1, center.getZ() + 1.5);
            shield = new MatchEffects.Shield(box, u.user());
            u.match().effects().addShield(shield);
            u.sound(SoundId.SHIELD_DEPLOY, center);
        }

        @Override
        public boolean tick() {
            age++;
            if (age == duration) {
                display.setItemStack(ItemFactory.model(WorldModel.SHIELD_WALL_CRACKED));
                u.match().effects().removeShield(shield);
            }
            if (age >= duration + 8) {
                shatter();
                return false;
            }
            return true;
        }

        private void shatter() {
            if (broken) return;
            broken = true;
            u.match().effects().removeShield(shield);
            Collection<Player> vs = u.viewers();
            BlockData glass = Material.LIGHT_BLUE_STAINED_GLASS.createBlockData();
            ThreadLocalRandom r = ThreadLocalRandom.current();
            double hx = shield.box.getWidthX() / 2, hz = shield.box.getWidthZ() / 2;
            for (int i = 0; i < 12; i++) {
                Location c = center.clone().add(r.nextDouble(-hx, hx), r.nextDouble(-0.9, 0.9), r.nextDouble(-hz, hz));
                Vector3f v = new Vector3f((float) r.nextDouble(-0.12, 0.12), (float) r.nextDouble(0, 0.15), (float) r.nextDouble(-0.12, 0.12));
                u.plugin().fragments().spawn(c, glass, v, vs, 0.25f, 0.05f, 20);
            }
            u.sound(SoundId.SHIELD_BREAK, center);
            Displays.remove(display);
        }

        @Override
        public void remove() {
            u.match().effects().removeShield(shield);
            if (!broken) {
                broken = true;
                Displays.remove(display);
            }
        }
    }

    // 14. ATRAPA
    public static final class Decoy extends Gadget {
        public Decoy() {
            super(RivalsItem.DECOY);
        }

        @Override
        public boolean use(GadgetUse u) {
            Player p = u.player();
            Location start = p.getLocation();
            LivingEntity clone = spawnClone(u.plugin(), p, start);
            if (clone == null) return false;
            u.match().effects().add(new Clone(u, clone));
            Combatant opp = u.target();
            int invis = u.ticks("invisibility");
            Player op = opp.player();
            if (op != null) {
                op.hideEntity(u.plugin(), p);
                u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () -> {
                    if (op.isOnline() && p.isOnline()) op.showEntity(u.plugin(), p);
                }, invis);
            }
            if (opp.isBot() && opp.bot().entity() instanceof Mob mob) {
                mob.setTarget(clone);
                u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () -> {
                    if (u.match().fighting()) opp.bot().target(p);
                }, invis + 10);
            }
            u.sound(SoundId.DECOY_USE);
            return true;
        }

        /** Mannequin ze skinem gracza (1.21.9+) albo zastępczy zombie z głową gracza. */
        static LivingEntity spawnClone(RivalsPlugin plugin, Player owner, Location at) {
            try {
                EntityType type = EntityType.valueOf("MANNEQUIN");
                Entity e = at.getWorld().spawnEntity(at, type);
                Class<?> rp = Class.forName("io.papermc.paper.datacomponent.item.ResolvableProfile");
                Object profile = rp.getMethod("resolvableProfile", PlayerProfile.class)
                        .invoke(null, owner.getPlayerProfile());
                e.getClass().getMethod("setProfile", rp).invoke(e, profile);
                try {
                    e.getClass().getMethod("setDescription", Component.class).invoke(e, Component.empty());
                } catch (ReflectiveOperationException ignored) {
                }
                e.setPersistent(false);
                if (e instanceof LivingEntity le) {
                    EntityEquipment eq = le.getEquipment();
                    if (eq != null) eq.setItemInMainHand(owner.getInventory().getItemInMainHand().clone());
                    return le;
                }
                e.remove();
            } catch (IllegalArgumentException | ReflectiveOperationException | LinkageError ignored) {
                // starsza wersja - brak Mannequina
            }
            return at.getWorld().spawn(at, Zombie.class, z -> {
                z.setPersistent(false);
                z.setAdult();
                z.setSilent(true);
                z.setAI(false);
                z.setShouldBurnInDay(false);
                z.customName(Component.text(owner.getName()));
                z.setCustomNameVisible(true);
                EntityEquipment eq = z.getEquipment();
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(owner);
                head.setItemMeta(sm);
                eq.setHelmet(head);
                eq.setChestplate(dyed(Material.LEATHER_CHESTPLATE));
                eq.setLeggings(dyed(Material.LEATHER_LEGGINGS));
                eq.setBoots(dyed(Material.LEATHER_BOOTS));
                eq.setItemInMainHand(owner.getInventory().getItemInMainHand().clone());
                eq.setHelmetDropChance(0);
                eq.setChestplateDropChance(0);
                eq.setLeggingsDropChance(0);
                eq.setBootsDropChance(0);
                eq.setItemInMainHandDropChance(0);
            });
        }

        private static ItemStack dyed(Material m) {
            ItemStack it = new ItemStack(m);
            LeatherArmorMeta meta = (LeatherArmorMeta) it.getItemMeta();
            meta.setColor(Color.fromRGB(0x2A313D));
            it.setItemMeta(meta);
            return it;
        }
    }

    /** Klon biegnie prosto; trafiony albo po czasie znika w obłoku. */
    static final class Clone implements MatchObject {
        private final GadgetUse u;
        private final LivingEntity clone;
        private final Vector step;
        private final int run;
        private int age;
        private boolean popped;

        Clone(GadgetUse u, LivingEntity clone) {
            this.u = u;
            this.clone = clone;
            this.run = u.ticks("run");
            Vector dir = u.player().getLocation().getDirection();
            dir.setY(0);
            if (dir.lengthSquared() < 1e-4) dir = new Vector(0, 0, 1);
            this.step = dir.normalize().multiply(u.stat("speed"));
            u.match().effects().addDecoy(clone, by -> pop());
        }

        @Override
        public boolean tick() {
            age++;
            if (popped || !clone.isValid() || age > run) return false;
            Location next = clone.getLocation().add(step);
            if (next.getBlock().getType().isSolid() || next.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                return true; // stoi przy ścianie
            }
            if (!next.clone().add(0, -0.2, 0).getBlock().getType().isSolid()) {
                next.add(0, -0.3, 0);
            }
            next.setDirection(step);
            clone.teleport(next);
            return true;
        }

        private void pop() {
            if (popped) return;
            popped = true;
            Location l = clone.getLocation().add(0, 1, 0);
            Fx.particle(u.viewers(), Particle.CLOUD, l, 25, 0.3, 0.6, 0.3, 0.03, null);
            Fx.particle(u.viewers(), Particle.DUST, l, 15, 0.3, 0.6, 0.3, 0, new Particle.DustOptions(Color.fromRGB(0x38E8F0), 1.2f));
            u.sound(SoundId.DECOY_POP, l);
        }

        @Override
        public void remove() {
            pop();
            u.match().effects().removeDecoy(clone);
            if (clone.isValid()) clone.remove();
        }
    }

    // 15. LUSTRO ZAMIANY
    public static final class Mirror extends Gadget {
        public Mirror() {
            super(RivalsItem.MIRROR);
        }

        @Override
        public boolean use(GadgetUse u) {
            LivingEntity te = u.target().entity();
            Player p = u.player();
            if (te == null || te.getLocation().distance(p.getLocation()) > u.stat("range")) {
                u.plugin().sounds().play(p, SoundId.UI_ERROR);
                return false;
            }
            Location a = p.getLocation();
            Location b = te.getLocation();
            pane(u, a);
            pane(u, b);
            Location toA = a.clone();
            toA.setYaw(b.getYaw());
            toA.setPitch(b.getPitch());
            Location toB = b.clone();
            toB.setYaw(a.getYaw());
            toB.setPitch(a.getPitch());
            // teleport bytu z pasażerami (sieć, lepki granat) by się nie udał
            p.eject();
            te.eject();
            p.teleport(toB);
            te.teleport(toA);
            p.setFallDistance(0);
            te.setFallDistance(0);
            u.sound(SoundId.MIRROR_SWAP, a);
            u.sound(SoundId.MIRROR_SWAP, b);
            return true;
        }

        /** Lustro pojawia się na mgnienie oka i rozpryskuje się na odłamki. */
        private void pane(GadgetUse u, Location at) {
            Location l = at.clone().add(0, 1.0, 0);
            l.setPitch(0);
            ItemDisplay d = Displays.world(u.plugin(), l, WorldModel.MIRROR_PANE, u.viewers(), 1f);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTransformation(Displays.trs(new Vector3f(), new Quaternionf(), new Vector3f(0.1f, 1f, 1f)));
            u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () ->
                    Displays.animate(d, 3, Displays.trs(new Vector3f(), new Quaternionf(), 1f)), 1L);
            u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () -> {
                Displays.remove(d);
                BlockData glass = Material.GLASS.createBlockData();
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int i = 0; i < 10; i++) {
                    Location c = l.clone().add(r.nextDouble(-0.6, 0.6), r.nextDouble(-0.9, 0.9), r.nextDouble(-0.6, 0.6));
                    Vector3f v = new Vector3f((float) r.nextDouble(-0.15, 0.15), (float) r.nextDouble(0, 0.2), (float) r.nextDouble(-0.15, 0.15));
                    u.plugin().fragments().spawn(c, glass, v, u.viewers(), 0.18f, 0.05f, 18);
                }
                Fx.particle(u.viewers(), Particle.BLOCK, l, 20, 0.4, 0.8, 0.4, 0, glass);
            }, 6L);
        }
    }

    // 16. APTECZKA
    public static final class Medkit extends Gadget {
        public Medkit() {
            super(RivalsItem.MEDKIT);
        }

        @Override
        public boolean use(GadgetUse u) {
            Player p = u.player();
            if (!Combat.onGround(p)) {
                u.plugin().sounds().play(p, SoundId.UI_ERROR);
                return false;
            }
            int cast = Math.max(5, u.ticks("cast"));
            TimedStatus freeze = TimedStatus.freeze(u.plugin(), p, cast);
            u.match().effects().add(freeze);
            u.match().effects().add(new Kit(u, cast));
            return true;
        }
    }

    /** Apteczka na ziemi przed graczem: otwiera wieko, po czasie leczy. */
    static final class Kit implements MatchObject {
        private final GadgetUse u;
        private final int cast;
        private final ItemDisplay base;
        private final ItemDisplay lid;
        private int age;
        private boolean healed;

        Kit(GadgetUse u, int cast) {
            this.u = u;
            this.cast = cast;
            Player p = u.player();
            Vector f = p.getLocation().getDirection().setY(0);
            if (f.lengthSquared() < 1e-4) f = new Vector(0, 0, 1);
            f.normalize();
            Location floor = p.getLocation().add(f.clone().multiply(0.9));
            floor.setY(p.getLocation().getY());
            float yaw = p.getLocation().getYaw() + 180f;
            Location bl = floor.clone().add(0, 0.5, 0);
            bl.setYaw(yaw);
            bl.setPitch(0);
            base = Displays.world(u.plugin(), bl, WorldModel.MEDKIT_BASE, u.viewers(), 1f);
            // zawias wieka: góra podstawy (y = 5 px) i tylna krawędź (z = 4 px) w lokalnym układzie
            Vector hinge = new Vector(0, -0.5 + 5 / 16.0, -0.5 + 4 / 16.0).rotateAroundY(Math.toRadians(-yaw));
            Location ll = bl.clone().add(hinge);
            ll.setYaw(yaw);
            ll.setPitch(0);
            lid = Displays.world(u.plugin(), ll, WorldModel.MEDKIT_LID, u.viewers(), 1f);
            u.plugin().getServer().getScheduler().runTaskLater(u.plugin(), () -> Displays.animate(lid, 8,
                    Displays.trs(new Vector3f(), new Quaternionf().rotationX((float) Math.toRadians(-110)), 1f)), 2L);
            u.sound(SoundId.MEDKIT_OPEN, floor);
        }

        @Override
        public boolean tick() {
            age++;
            if (!healed && age >= cast) {
                healed = true;
                Player p = u.player();
                AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
                double mh = max == null ? 20 : max.getValue();
                p.setHealth(Math.min(mh, p.getHealth() + u.stat("heal")));
                Location l = p.getLocation().add(0, 1, 0);
                Fx.particle(u.viewers(), Particle.HEART, l.clone().add(0, 1, 0), 6, 0.4, 0.3, 0.4, 0, null);
                Fx.particle(u.viewers(), Particle.HAPPY_VILLAGER, l, 15, 0.4, 0.6, 0.4, 0, null);
                u.sound(SoundId.MEDKIT_HEAL, l);
                Displays.animate(base, 5, Displays.trs(new Vector3f(), new Quaternionf(), 0.01f));
                Displays.animate(lid, 5, Displays.trs(new Vector3f(), new Quaternionf(), 0.01f));
            }
            return age < cast + 6;
        }

        @Override
        public void remove() {
            Displays.remove(base);
            Displays.remove(lid);
        }
    }
}
