package pl.rivals.hud;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.combat.Displays;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Liczby obrażeń: text_display nad trafionym (widzi je tylko atakujący), unosi się i zanika. */
public final class DamageNumbers {

    private static final class Num {
        final TextDisplay d;
        int age;

        Num(TextDisplay d) {
            this.d = d;
        }
    }

    private final Plugin plugin;
    private final List<Num> list = new ArrayList<>();

    public DamageNumbers(Plugin plugin) {
        this.plugin = plugin;
    }

    public void spawn(Player viewer, LivingEntity victim, double dmg, boolean head) {
        if (list.size() > 120) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location l = victim.getEyeLocation().add(r.nextDouble(-0.4, 0.4), 0.45, r.nextDouble(-0.4, 0.4));
        String txt = dmg >= 10 || dmg == Math.rint(dmg) ? String.valueOf(Math.round(dmg)) : String.format(Locale.ROOT, "%.1f", dmg);
        TextDisplay d = Fx.spawnFor(plugin, l, TextDisplay.class, List.of(viewer), t -> {
            t.text(Hud.pixel(head ? txt + "!" : txt, head ? Hud.GOLD : Hud.RED));
            t.setBillboard(Display.Billboard.CENTER);
            t.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            t.setShadowed(false);
            t.setSeeThrough(false);
            t.setTransformation(Displays.trs(new Vector3f(), new Quaternionf(), head ? 0.9f : 0.7f));
            t.setInterpolationDuration(0);
            t.setBrightness(new Display.Brightness(15, 15));
        });
        list.add(new Num(d));
    }

    public void tick() {
        Iterator<Num> it = list.iterator();
        while (it.hasNext()) {
            Num n = it.next();
            n.age++;
            if (!n.d.isValid()) {
                it.remove();
                continue;
            }
            switch (n.age) {
                case 1 -> Displays.animate(n.d, 14, Displays.trs(new Vector3f(0, 0.8f, 0), new Quaternionf(), 1.1f));
                case 9 -> n.d.setTextOpacity((byte) 170);
                case 12 -> n.d.setTextOpacity((byte) 100);
                case 14 -> n.d.setTextOpacity((byte) 40);
                default -> {
                }
            }
            if (n.age >= 16) {
                n.d.remove();
                it.remove();
            }
        }
    }

    public void clear() {
        for (Num n : list) if (n.d.isValid()) n.d.remove();
        list.clear();
    }
}
