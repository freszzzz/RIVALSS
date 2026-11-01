package pl.rivals.weapon;

import org.bukkit.block.Block;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.combat.Combat;
import pl.rivals.combat.CombatState;
import pl.rivals.combat.Cooldowns;
import pl.rivals.combat.Displays;
import pl.rivals.combat.MatchObject;
import pl.rivals.combat.SimProjectile;
import pl.rivals.config.RivalsConfig;
import pl.rivals.gadget.Gadget;
import pl.rivals.gadget.GadgetUse;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/** Logika broni i wywoływanie gadżetów. */
public final class Weapons {

    private static final Color TRACER = Color.fromRGB(0x38E8F0);
    private static final Color TRACER_SHOTGUN = Color.fromRGB(0xFF8A3D);

    private final RivalsPlugin plugin;

    public Weapons(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    private RivalsConfig cfg() {
        return plugin.settings();
    }

    // PPM
    /** @return true, jeśli przedmiot został obsłużony (zdarzenie należy anulować). */
    public boolean rightClick(Match m, Combatant c, Player p, RivalsItem item) {
        switch (item) {
            case RIFLE, PISTOL -> hitscan(m, c, p, item, 1);
            case SHOTGUN -> hitscan(m, c, p, item, cfg().statInt(item, "pellets"));
            case SLINGSHOT -> slingshot(m, c, p);
            default -> {
                if (item.slot == RivalsItem.Slot.GADGET) gadget(m, c, p, item);
                else return false;
            }
        }
        return true;
    }

    // broń palna (hitscan)
    private void hitscan(Match m, Combatant c, Player p, RivalsItem item, int pellets) {
        if (!m.fighting()) return;
        CombatState st = c.state;
        int magazine = cfg().statInt(item, "magazine");
        int delay = cfg().statInt(item, "fire-delay-ticks");
        if (CombatState.now() - st.lastShot(item) < delay) return;
        if (st.reloading(item)) {
            if (!st.ready(item)) return;
            st.ammo(item, magazine, false);
        }
        int ammo = st.ammo(item, magazine);
        if (ammo <= 0) {
            reload(c, p, item);
            return;
        }
        st.shot(item);
        st.ammo(item, ammo - 1, false);
        c.stats.shots++;

        Location eye = p.getEyeLocation();
        Vector look = eye.getDirection();
        double range = cfg().stat(item, "range");
        double spread = item == RivalsItem.SHOTGUN ? cfg().stat(item, "spread") : 0.004;
        double perHit = item == RivalsItem.SHOTGUN ? cfg().stat(item, "pellet-damage") : cfg().stat(item, "damage");
        double headMul = item == RivalsItem.SHOTGUN ? 1.0 : Math.max(1, cfg().stat(item, "headshot"));
        Collection<Player> vs = m.viewers();
        ThreadLocalRandom r = ThreadLocalRandom.current();

        double total = 0;
        boolean head = false;
        boolean hitAny = false;
        for (int i = 0; i < pellets; i++) {
            Vector dir = look.clone().add(new Vector(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).multiply(spread)).normalize();
            Combat.RayHit hit = Combat.ray(m, c, eye, dir, range);
            Location muzzle = eye.clone().add(dir.clone().multiply(0.8)).add(0, -0.15, 0);
            Combat.tracer(vs, muzzle, hit.point(), item == RivalsItem.SHOTGUN ? TRACER_SHOTGUN : TRACER, item == RivalsItem.SHOTGUN ? 0.9 : 0.6);
            Location hp = hit.point().toLocation(eye.getWorld());
            if (hit.blocked()) {
                Fx.particle(vs, Particle.ELECTRIC_SPARK, hp, 6, 0.1, 0.1, 0.1, 0.15, null);
            } else if (hit.target() != null) {
                double dmg = perHit * (hit.head() ? headMul : 1);
                head |= hit.head();
                total += dmg;
                hitAny = true;
            } else if (hit.decoy() != null) {
                Combat.hitDecoy(m, hit.decoy(), c);
            } else if (hit.distance() < range - 0.01) {
                Block b = hp.getBlock();
                Material mat = b.getType().isAir() ? Material.STONE : b.getType();
                Fx.particle(vs, Particle.BLOCK, hp, 4, 0.05, 0.05, 0.05, 0, mat.createBlockData());
            }
        }
        if (hitAny) {
            c.stats.hits++;
            Combat.damage(m, c, c.opponent(), total, head ? Combat.Kind.RANGED_HEAD : Combat.Kind.RANGED);
        }
        Location muzzle = eye.clone().add(look.clone().multiply(0.9)).add(0, -0.15, 0);
        Fx.particle(vs, Particle.SMOKE, muzzle, 2, 0.02, 0.02, 0.02, 0.01, null);
        Fx.particle(vs, Particle.ELECTRIC_SPARK, muzzle, 2, 0.02, 0.02, 0.02, 0.05, null);
        SoundId sound = switch (item) {
            case RIFLE -> SoundId.RIFLE_SHOOT;
            case SHOTGUN -> SoundId.SHOTGUN_SHOOT;
            default -> SoundId.PISTOL_SHOOT;
        };
        plugin.sounds().play(vs, sound, p.getLocation());
        if (item == RivalsItem.SHOTGUN) {
            Vector kick = look.clone().multiply(-0.25);
            kick.setY(Math.min(0.1, kick.getY()));
            p.setVelocity(p.getVelocity().add(kick));
        }
        if (ammo - 1 <= 0) reload(c, p, item);
        else if (delay >= 8) Cooldowns.start(c, item, delay);
    }

    /** Przeładowanie (też klawisz F w trakcie meczu). */
    public void reload(Combatant c, Player p, RivalsItem item) {
        if (item == null || cfg().stat(item, "magazine") <= 0) return;
        CombatState st = c.state;
        if (st.reloading(item) && !st.ready(item)) return;
        int ticks = cfg().statTicks(item, "reload");
        st.ammo(item, 0, true);
        Cooldowns.start(c, item, ticks);
        plugin.sounds().play(p, SoundId.RELOAD);
    }

    // proca
    private void slingshot(Match m, Combatant c, Player p) {
        if (!m.fighting()) return;
        RivalsItem item = RivalsItem.SLINGSHOT;
        int delay = cfg().statInt(item, "fire-delay-ticks");
        if (!c.state.ready(item)) return;
        c.stats.shots++;
        Location start = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(0.6));
        SimProjectile proj = new SimProjectile(m, c, start, p.getEyeLocation().getDirection().multiply(cfg().stat(item, "speed")));
        proj.gravity = 0.05;
        proj.maxAge = 60;
        proj.display = Displays.world(plugin, start, WorldModel.PEBBLE, m.viewers(), 1f);
        proj.spinDegPerTick = 30;
        proj.onHit = (pr, target, decoy, block, face, point) -> {
            if (target != null) {
                c.stats.hits++;
                Combat.damage(m, c, target, cfg().stat(item, "damage"), Combat.Kind.RANGED);
                LivingEntity te = target.entity();
                if (te != null) {
                    Vector kb = pr.vel.clone().normalize().multiply(cfg().stat(item, "knockback"));
                    kb.setY(0.3);
                    te.setVelocity(te.getVelocity().add(kb));
                }
            }
            plugin.sounds().play(m.viewers(), SoundId.IMPACT, point.toLocation(pr.world));
            return SimProjectile.Result.STOP;
        };
        m.effects().add(proj);
        Cooldowns.start(c, item, delay);
        plugin.sounds().play(m.viewers(), SoundId.SLINGSHOT_SHOOT, p.getLocation());
    }

    // łuk i kusza
    public void bowShot(Match m, Combatant c, Player p, RivalsItem item, float force) {
        if (!m.fighting()) return;
        boolean crossbow = item == RivalsItem.CROSSBOW;
        double speed = cfg().stat(item, "speed") * (crossbow ? 1 : Math.max(0.2, force));
        double dmg = cfg().stat(item, "damage") * (crossbow ? 1 : Math.max(0.15, force * force));
        Location start = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(0.7));
        SimProjectile proj = new SimProjectile(m, c, start, p.getEyeLocation().getDirection().multiply(speed));
        proj.gravity = crossbow ? 0.012 : 0.025;
        proj.drag = 0.995;
        proj.maxAge = 80;
        proj.faceVelocity = true;
        proj.display = Displays.world(plugin, start, crossbow ? WorldModel.CROSSBOW_BOLT : WorldModel.ENERGY_BOLT, m.viewers(), 1f);
        if (!crossbow) proj.display.setBrightness(new Display.Brightness(15, 15));
        c.stats.shots++;
        proj.onTick = pr -> {
            if (!crossbow) Fx.particle(m.viewers(), Particle.END_ROD, pr.location(), 1, 0, 0, 0, 0, null);
        };
        proj.onHit = (pr, target, decoy, block, face, point) -> {
            if (target != null) {
                c.stats.hits++;
                LivingEntity te = target.entity();
                boolean head = te != null && point.getY() >= te.getEyeLocation().getY() - 0.3;
                Combat.damage(m, c, target, dmg * (head ? 1.25 : 1), head ? Combat.Kind.RANGED_HEAD : Combat.Kind.RANGED);
                return SimProjectile.Result.STOP;
            }
            if (block != null && crossbow) {
                // bełt wbija się na chwilę w blok
                plugin.sounds().play(m.viewers(), SoundId.IMPACT, point.toLocation(pr.world));
                pr.maxAge = Math.min(pr.maxAge, pr.age + 40);
                return SimProjectile.Result.STICK;
            }
            Fx.particle(m.viewers(), Particle.ELECTRIC_SPARK, point.toLocation(pr.world), 6, 0.1, 0.1, 0.1, 0.1, null);
            return SimProjectile.Result.STOP;
        };
        m.effects().add(proj);
        Cooldowns.start(c, item, cfg().statTicks(item, "cooldown"));
        plugin.sounds().play(m.viewers(), crossbow ? SoundId.CROSSBOW_SHOOT : SoundId.BOW_SHOOT, p.getLocation());
    }

    // młot sejsmiczny
    public void hammerRelease(Match m, Combatant c, Player p, int heldTicks) {
        if (!m.fighting()) return;
        RivalsItem item = RivalsItem.HAMMER;
        int min = cfg().statInt(item, "min-charge-ticks");
        int max = Math.max(min + 1, cfg().statInt(item, "max-charge-ticks"));
        if (heldTicks < min || !c.state.ready(item)) return;
        double k = Math.min(1, (heldTicks - min) / (double) (max - min));
        double radius = lerp(cfg().stat(item, "slam-radius-min"), cfg().stat(item, "slam-radius-max"), k);
        double dmg = lerp(cfg().stat(item, "slam-damage-min"), cfg().stat(item, "slam-damage-max"), k);
        double kb = cfg().stat(item, "knockback") * (0.6 + 0.4 * k);
        Location center = p.getLocation();
        Collection<Player> vs = m.viewers();

        // rozszerzający się pierścień fali - jeden display, skala interpolowana po stronie klienta
        Location ringLoc = center.clone().add(0, 0.15, 0);
        ringLoc.setYaw(0);
        ringLoc.setPitch(0);
        var ring = Displays.world(plugin, ringLoc, WorldModel.SHOCKWAVE_RING, vs, 0.3f);
        ring.setBrightness(new Display.Brightness(15, 15));
        float target = (float) (radius * 2);
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                Displays.animate(ring, 6, Displays.trs(new Vector3f(), new Quaternionf(), new Vector3f(target, 1.5f, target))), 1L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                Displays.animate(ring, 4, Displays.trs(new Vector3f(), new Quaternionf(), new Vector3f(target * 1.1f, 0.01f, target * 1.1f))), 7L);
        m.effects().add(new MatchObject() {
            int age;

            @Override
            public boolean tick() {
                return ++age < 12;
            }

            @Override
            public void remove() {
                Displays.remove(ring);
            }
        });

        Block floor = center.clone().add(0, -0.5, 0).getBlock();
        Material fm = floor.getType().isAir() ? Material.STONE : floor.getType();
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            Location l = center.clone().add(Math.cos(a) * radius * 0.7, 0.1, Math.sin(a) * radius * 0.7);
            Fx.particle(vs, Particle.BLOCK, l, 4, 0.2, 0.05, 0.2, 0, fm.createBlockData());
        }
        Fx.particle(vs, Particle.EXPLOSION, center, 1, 0, 0, 0, 0, null);
        plugin.sounds().play(vs, SoundId.HAMMER_SLAM, center);

        Combatant t = c.opponent();
        LivingEntity te = t.entity();
        if (te != null) {
            double dist = te.getLocation().distance(center);
            if (dist <= radius && Math.abs(te.getLocation().getY() - center.getY()) < 2.5) {
                Combat.damage(m, c, t, dmg, Combat.Kind.MELEE);
                Vector push = te.getLocation().toVector().subtract(center.toVector()).setY(0);
                if (push.lengthSquared() < 1e-4) push = p.getLocation().getDirection().setY(0);
                push.normalize().multiply(kb);
                push.setY(0.45 + 0.25 * k);
                te.setVelocity(push);
            }
        }
        Cooldowns.start(c, item, cfg().statTicks(item, "cooldown"));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // gadżety
    private void gadget(Match m, Combatant c, Player p, RivalsItem item) {
        if (!m.fighting()) return;
        Gadget g = plugin.gadgets().get(item);
        if (g == null) return;
        CombatState st = c.state;
        boolean ready = st.ready(item);
        if (!ready && st.bonusCharges <= 0) {
            plugin.sounds().play(p, SoundId.UI_ERROR, 1.4f);
            return;
        }
        boolean consumed = g.use(new GadgetUse(plugin, m, c, p, item));
        if (!consumed) return;
        c.stats.gadgetUsed(item.id);
        if (!ready) st.bonusCharges--;
        else Cooldowns.start(c, item, g.cooldownTicks(plugin));
    }

    // amunicja do łuku/kuszy
    /** Łuk i kusza wymagają strzały w ekwipunku - trzymamy jedną w ukrytym slocie. */
    public static void ensureArrow(Player p) {
        ItemStack s = p.getInventory().getItem(17);
        if (s == null || s.getType() != Material.ARROW) p.getInventory().setItem(17, new ItemStack(Material.ARROW));
    }
}
