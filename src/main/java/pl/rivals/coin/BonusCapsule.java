package pl.rivals.coin;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.combat.Displays;
import pl.rivals.combat.MatchObject;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Kapsuła z bonusem: spada na środek areny (interpolacja z wysokości), ląduje z obłokiem,
 * otwiera wieko na zawiasie, w środku obraca się kula leczenia albo dodatkowego ładunku gadżetu.
 */
public final class BonusCapsule implements MatchObject {

    private static final int FALL = 14;
    private static final int OPEN_AT = FALL + 6;

    private final RivalsPlugin plugin;
    private final Match match;
    private final Location floor;
    private final boolean heal;
    private final ItemDisplay bottom;
    private final ItemDisplay lid;
    private ItemDisplay orb;
    private int age;
    private boolean taken;
    private int takenAge;
    private float orbAngle;

    public BonusCapsule(RivalsPlugin plugin, Match match, Location floor) {
        this.plugin = plugin;
        this.match = match;
        this.floor = floor;
        this.heal = ThreadLocalRandom.current().nextBoolean();
        Location b = floor.clone().add(0, 0.5, 0);
        b.setYaw(0);
        b.setPitch(0);
        bottom = Displays.world(plugin, b, WorldModel.CAPSULE_BOTTOM, match.viewers(), 1f);
        Location h = b.clone().add(0, -0.5 + 10 / 16.0, -0.5 - 2 / 16.0);
        lid = Displays.world(plugin, h, WorldModel.CAPSULE_LID, match.viewers(), 1f);
        Vector3f high = new Vector3f(0, 18, 0);
        bottom.setTransformation(Displays.trs(high, new Quaternionf(), 1f));
        lid.setTransformation(Displays.trs(high, new Quaternionf(), 1f));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Displays.animate(bottom, FALL - 1, Displays.trs(new Vector3f(), new Quaternionf(), 1f));
            Displays.animate(lid, FALL - 1, Displays.trs(new Vector3f(), new Quaternionf(), 1f));
        }, 1L);
        plugin.sounds().play(match.viewers(), SoundId.CAPSULE_FALL, floor);
    }

    @Override
    public boolean tick() {
        age++;
        if (age < FALL && age % 2 == 0) {
            Location trail = floor.clone().add(0, 18.0 * (1 - age / (double) FALL) + 1, 0);
            Fx.particle(match.viewers(), Particle.CAMPFIRE_COSY_SMOKE, trail, 2, 0.2, 0.2, 0.2, 0.01, null);
        }
        if (age == FALL) {
            Fx.particle(match.viewers(), Particle.CLOUD, floor.clone().add(0, 0.3, 0), 30, 1.0, 0.1, 1.0, 0.05, null);
            Fx.particle(match.viewers(), Particle.BLOCK, floor.clone().add(0, 0.1, 0), 30, 1.0, 0.1, 1.0, 0,
                    floor.clone().add(0, -0.5, 0).getBlock().getType().isAir() ? Material.STONE.createBlockData()
                            : floor.clone().add(0, -0.5, 0).getBlock().getBlockData());
            plugin.sounds().play(match.viewers(), SoundId.CAPSULE_LAND, floor);
        }
        if (age == OPEN_AT) {
            Displays.animate(lid, 8, Displays.trs(new Vector3f(), new Quaternionf().rotationX((float) Math.toRadians(-110)), 1f));
            plugin.sounds().play(match.viewers(), SoundId.CAPSULE_OPEN, floor);
            Location ol = floor.clone().add(0, 1.0, 0);
            orb = Displays.world(plugin, ol, heal ? WorldModel.HEAL_ORB : WorldModel.GADGET_ORB, match.viewers(), 0.01f);
            orb.setBrightness(new Display.Brightness(15, 15));
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    Displays.animate(orb, 8, Displays.trs(new Vector3f(0, 0.5f, 0), new Quaternionf(), 0.8f)), 2L);
        }
        if (orb != null && !taken && age > OPEN_AT + 9) {
            if ((age - OPEN_AT) % 10 == 0) {
                orbAngle += (float) Math.toRadians(100);
                float bob = 0.5f + (float) Math.sin(age / 8.0) * 0.12f;
                Displays.animate(orb, 10, Displays.trs(new Vector3f(0, bob, 0), new Quaternionf().rotationY(orbAngle), 0.8f));
            }
            double r = plugin.settings().capsulePickupRadius;
            for (Combatant c : new Combatant[]{match.a(), match.b()}) {
                LivingEntity e = c.entity();
                if (e == null || c.isBot()) continue;
                if (e.getLocation().distance(floor) <= r + 0.3) {
                    take(c, e);
                    break;
                }
            }
        }
        if (taken && ++takenAge > 30) {
            Displays.animate(bottom, 6, Displays.trs(new Vector3f(), new Quaternionf(), 0.01f));
            Displays.animate(lid, 6, Displays.trs(new Vector3f(), new Quaternionf(), 0.01f));
            return takenAge < 38;
        }
        return true;
    }

    private void take(Combatant c, LivingEntity e) {
        taken = true;
        Displays.animate(orb, 4, Displays.trs(new Vector3f(0, 1.2f, 0), new Quaternionf(), 0.01f));
        Location l = e.getLocation().add(0, 1, 0);
        if (heal) {
            AttributeInstance max = e.getAttribute(Attribute.MAX_HEALTH);
            double mh = max == null ? 20 : max.getValue();
            e.setHealth(Math.min(mh, e.getHealth() + plugin.settings().capsuleHeal));
            Fx.particle(match.viewers(), Particle.HEART, l.clone().add(0, 1, 0), 6, 0.4, 0.3, 0.4, 0, null);
        } else {
            c.state.bonusCharges++;
            Fx.particle(match.viewers(), Particle.WAX_ON, l, 20, 0.4, 0.6, 0.4, 0.5, null);
        }
        plugin.sounds().play(match.viewers(), SoundId.BONUS_PICKUP, l);
        Player p = c.player();
        if (p != null) plugin.messages().send(p, heal ? "bonus.heal" : "bonus.gadget");
    }

    @Override
    public void remove() {
        Displays.remove(bottom);
        Displays.remove(lid);
        Displays.remove(orb);
    }
}
