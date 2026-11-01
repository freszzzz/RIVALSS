package pl.rivals.cosmetic;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import pl.rivals.RivalsPlugin;
import pl.rivals.anim.Fx;
import pl.rivals.data.PlayerProfile;
import pl.rivals.shared.Cosmetic;
import pl.rivals.shared.SoundId;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/** Efekty zabójstwa: domyślny + 4 do kupienia (lód, konfetti, błyskawica, pikselowy rozpad). */
public final class KillEffects {

    private static final Material[] CONFETTI = {Material.CYAN_CONCRETE, Material.MAGENTA_CONCRETE, Material.YELLOW_CONCRETE,
            Material.LIME_CONCRETE, Material.ORANGE_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.PINK_CONCRETE};
    private static final Material[] PIXELS = {Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.CYAN_CONCRETE,
            Material.GRAY_CONCRETE, Material.LIGHT_BLUE_CONCRETE};

    private KillEffects() {
    }

    public static void play(RivalsPlugin plugin, PlayerProfile killer, Location feet, Collection<Player> viewers) {
        String id = killer == null ? "default" : killer.killEffect;
        Cosmetic c = Cosmetic.byId(id);
        if (c == null || !Cosmetics.owns(killer, c)) id = "default";
        Location body = feet.clone().add(0, 1, 0);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        switch (id) {
            case "kill_ice" -> {
                BlockData ice = Material.ICE.createBlockData();
                BlockData packed = Material.PACKED_ICE.createBlockData();
                for (int i = 0; i < 16; i++) {
                    Location l = feet.clone().add(r.nextDouble(-0.35, 0.35), r.nextDouble(0.1, 1.9), r.nextDouble(-0.35, 0.35));
                    Vector3f v = new Vector3f((float) r.nextDouble(-0.12, 0.12), (float) r.nextDouble(0.0, 0.2), (float) r.nextDouble(-0.12, 0.12));
                    plugin.fragments().spawn(l, i % 3 == 0 ? packed : ice, v, viewers, 0.28f, 0.05f, 24);
                }
                Fx.particle(viewers, Particle.SNOWFLAKE, body, 40, 0.4, 0.8, 0.4, 0.05, null);
                plugin.sounds().play(viewers, SoundId.CRYO_SHATTER, body);
            }
            case "kill_confetti" -> {
                for (int i = 0; i < 26; i++) {
                    BlockData d = CONFETTI[r.nextInt(CONFETTI.length)].createBlockData();
                    Vector3f v = new Vector3f((float) r.nextDouble(-0.2, 0.2), (float) r.nextDouble(0.25, 0.5), (float) r.nextDouble(-0.2, 0.2));
                    plugin.fragments().spawn(body, d, v, viewers, 0.12f, 0.035f, 30);
                }
                Fx.particle(viewers, Particle.FIREWORK, body, 20, 0.3, 0.3, 0.3, 0.15, null);
                plugin.sounds().play(viewers, SoundId.ROUND_WIN, body);
            }
            case "kill_lightning" -> {
                feet.getWorld().strikeLightningEffect(feet);
                Fx.particle(viewers, Particle.ELECTRIC_SPARK, body, 60, 0.4, 1.0, 0.4, 0.3, null);
                Fx.particle(viewers, Particle.DUST, body, 30, 0.3, 1.0, 0.3, 0, new Particle.DustOptions(Color.fromRGB(0xEEF2F6), 1.6f));
            }
            case "kill_pixel" -> {
                for (int y = 0; y < 8; y++) {
                    for (int k = 0; k < 3; k++) {
                        Location l = feet.clone().add(r.nextDouble(-0.3, 0.3), y * 0.24 + 0.1, r.nextDouble(-0.3, 0.3));
                        BlockData d = PIXELS[r.nextInt(PIXELS.length)].createBlockData();
                        Vector3f v = new Vector3f((float) r.nextDouble(-0.03, 0.03),
                                (float) r.nextDouble(0.03, 0.08), (float) r.nextDouble(-0.03, 0.03));
                        plugin.fragments().spawn(l, d, v, viewers, 0.14f, -0.004f, 26);
                    }
                }
                Fx.particle(viewers, Particle.END_ROD, body, 10, 0.3, 0.8, 0.3, 0.02, null);
            }
            default -> {
                Fx.particle(viewers, Particle.DUST, body, 30, 0.3, 0.6, 0.3, 0, new Particle.DustOptions(Color.fromRGB(0x38E8F0), 1.4f));
                Fx.particle(viewers, Particle.CLOUD, body, 12, 0.2, 0.4, 0.2, 0.03, null);
                Fx.particle(viewers, Particle.END_ROD, body, 8, 0.2, 0.5, 0.2, 0.08, null);
            }
        }
        plugin.sounds().play(viewers, SoundId.KILL, body);
    }
}
