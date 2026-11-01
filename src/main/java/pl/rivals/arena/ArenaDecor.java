package pl.rivals.arena;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.RivalsPlugin;
import pl.rivals.anim.Fx;
import pl.rivals.combat.Displays;
import pl.rivals.item.ItemFactory;
import pl.rivals.shared.WorldModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Dekoracje areny z display entities, widoczne tylko dla graczy meczu. Ruch (wentylator, logo)
 * to stany docelowe co 10-20 ticków interpolowane po stronie klienta; lampy migają podmianą modelu.
 */
public final class ArenaDecor {

    private interface Piece {
        void tick(int age);

        void remove();
    }

    private final List<Piece> pieces = new ArrayList<>();
    private int age;

    public static ArenaDecor spawn(RivalsPlugin plugin, ArenaInstance arena, Supplier<Collection<Player>> viewersSupplier) {
        ArenaDecor decor = new ArenaDecor();
        if (!plugin.settings().decorations) return decor;
        Collection<Player> viewers = viewersSupplier.get();
        int budget = plugin.settings().decorationsMax;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (ArenaTemplate.Decoration d : arena.template().decorations()) {
            if (budget <= 0) break;
            Location l = new Location(arena.world(), arena.ox() + d.x() + 0.5, arena.oy() + d.y() + 0.5, arena.oz() + d.z() + 0.5, d.yaw(), 0);
            switch (d.type()) {
                case "fan" -> {
                    ItemDisplay housing = Displays.world(plugin, l, WorldModel.FAN_HOUSING, viewers, 1f);
                    ItemDisplay blades = Displays.world(plugin, l, WorldModel.FAN_BLADES, viewers, 1f);
                    budget -= 2;
                    float[] angle = {0};
                    decor.pieces.add(new Piece() {
                        public void tick(int age) {
                            if (age % 10 == 0) {
                                angle[0] += (float) Math.toRadians(120);
                                Displays.animate(blades, 10, Displays.trs(new Vector3f(), new Quaternionf().rotationZ(angle[0]), 1f));
                            }
                        }

                        public void remove() {
                            Displays.remove(housing);
                            Displays.remove(blades);
                        }
                    });
                }
                case "lamp" -> {
                    ItemDisplay lamp = Displays.world(plugin, l, WorldModel.LAMP_ON, viewers, 1f);
                    lamp.setBrightness(new Display.Brightness(15, 15));
                    budget--;
                    int phase = r.nextInt(60);
                    decor.pieces.add(new Piece() {
                        public void tick(int age) {
                            int t = (age + phase) % 60;
                            if (t == 0) lamp.setItemStack(ItemFactory.model(WorldModel.LAMP_OFF));
                            else if (t == 3) lamp.setItemStack(ItemFactory.model(WorldModel.LAMP_ON));
                            else if (t == 6) lamp.setItemStack(ItemFactory.model(WorldModel.LAMP_OFF));
                            else if (t == 8) lamp.setItemStack(ItemFactory.model(WorldModel.LAMP_ON));
                        }

                        public void remove() {
                            Displays.remove(lamp);
                        }
                    });
                }
                case "logo" -> {
                    ItemDisplay logo = Displays.world(plugin, l, WorldModel.LOGO, viewers, 2.5f);
                    logo.setBrightness(new Display.Brightness(15, 15));
                    budget--;
                    float[] angle = {0};
                    decor.pieces.add(new Piece() {
                        public void tick(int age) {
                            if (age % 20 == 0) {
                                angle[0] += (float) Math.toRadians(60);
                                float bob = (float) Math.sin(age / 20.0) * 0.25f;
                                Displays.animate(logo, 20, Displays.trs(new Vector3f(0, bob, 0), new Quaternionf().rotationY(angle[0]), 2.5f));
                            }
                        }

                        public void remove() {
                            Displays.remove(logo);
                        }
                    });
                }
                case "pipe" -> {
                    ItemDisplay pipe = Displays.world(plugin, l, WorldModel.PIPE, viewers, 1f);
                    budget--;
                    Vector along = new Vector(1, 0, 0).rotateAroundY(Math.toRadians(-d.yaw()));
                    Location vent = l.clone().add(along.multiply(1.5));
                    int phase = r.nextInt(40);
                    decor.pieces.add(new Piece() {
                        public void tick(int age) {
                            if ((age + phase) % 40 == 0) {
                                Fx.particle(viewersSupplier.get(), Particle.CLOUD, vent, 6, 0.05, 0.05, 0.05, 0.03, null);
                                Fx.particle(viewersSupplier.get(), Particle.WHITE_SMOKE, vent, 4, 0.05, 0.1, 0.05, 0.02, null);
                            }
                        }

                        public void remove() {
                            Displays.remove(pipe);
                        }
                    });
                }
                case "screen" -> {
                    ItemDisplay screen = Displays.world(plugin, l, WorldModel.SCREEN, viewers, 1.5f);
                    screen.setBrightness(new Display.Brightness(15, 15));
                    budget--;
                    decor.pieces.add(new Piece() {
                        public void tick(int age) {
                        }

                        public void remove() {
                            Displays.remove(screen);
                        }
                    });
                }
                default -> {
                }
            }
        }
        return decor;
    }

    public void tick() {
        age++;
        for (Piece p : pieces) p.tick(age);
    }

    public void remove() {
        for (Piece p : pieces) p.remove();
        pieces.clear();
    }
}
