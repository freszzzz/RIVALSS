package pl.rivals.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.RivalsPlugin;
import pl.rivals.combat.Displays;
import pl.rivals.data.PlayerProfile;
import pl.rivals.hud.Hud;
import pl.rivals.item.ItemFactory;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.WorldModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Obiekty 3D w lobby (lobby.yml): tablica rankingowa (text_display w ramce z block_display),
 * postumenty z obracającymi się modelami gadżetów oraz NPC kolejki.
 * Byty są nietrwałe - tworzone przy starcie pluginu, chunk trzymany ticketem.
 */
public final class LobbyObjects implements Listener {

    private final RivalsPlugin plugin;
    private final File file;
    private final List<Entity> spawned = new ArrayList<>();
    private final Set<UUID> npcIds = new HashSet<>();
    private final List<Pedestal> pedestals = new ArrayList<>();
    private TextDisplay leaderboard;
    private BukkitTask task;
    private long tick;

    private static final class Pedestal {
        ItemDisplay model;
        int index;
        float angle;
    }

    public LobbyObjects(RivalsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lobby.yml");
    }

    private YamlConfiguration yaml() {
        return YamlConfiguration.loadConfiguration(file);
    }

    private void save(YamlConfiguration y) {
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Nie zapisano lobby.yml", ex);
        }
    }

    // cykl życia
    public void start() {
        respawn();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 1L);
    }

    public void stop() {
        if (task != null) task.cancel();
        despawn();
    }

    private void despawn() {
        for (Entity e : spawned) if (e.isValid()) e.remove();
        spawned.clear();
        npcIds.clear();
        pedestals.clear();
        labels.clear();
        leaderboard = null;
    }

    public void respawn() {
        despawn();
        YamlConfiguration y = yaml();
        Location lb = y.getLocation("leaderboard");
        if (lb != null && lb.getWorld() != null) spawnLeaderboard(lb);
        for (Object o : y.getList("pedestals", List.of())) {
            if (o instanceof Location l && l.getWorld() != null) spawnPedestal(l, pedestals.size());
        }
        Location npc = y.getLocation("npc");
        if (npc != null && npc.getWorld() != null) spawnNpc(npc);
        refreshLeaderboard();
    }

    private void keepLoaded(Location l) {
        l.getWorld().addPluginChunkTicket(l.getBlockX() >> 4, l.getBlockZ() >> 4, plugin);
    }

    // ustawianie (komendy)
    public void setLeaderboard(Location l) {
        YamlConfiguration y = yaml();
        y.set("leaderboard", center(l));
        save(y);
        respawn();
    }

    public void addPedestal(Location l) {
        YamlConfiguration y = yaml();
        List<Object> list = new ArrayList<>(y.getList("pedestals", List.of()));
        list.add(center(l));
        y.set("pedestals", list);
        save(y);
        respawn();
    }

    public void setNpc(Location l) {
        YamlConfiguration y = yaml();
        Location c = center(l);
        c.setYaw(l.getYaw());
        y.set("npc", c);
        save(y);
        respawn();
    }

    public void clear() {
        YamlConfiguration y = new YamlConfiguration();
        save(y);
        despawn();
    }

    private static Location center(Location l) {
        Location c = l.getBlock().getLocation().add(0.5, 0, 0.5);
        c.setYaw(Math.round(l.getYaw() / 45f) * 45f);
        c.setPitch(0);
        return c;
    }

    // tablica rankingowa
    private void spawnLeaderboard(Location base) {
        keepLoaded(base);
        Location textLoc = base.clone().add(0, 1.2, 0);
        leaderboard = base.getWorld().spawn(textLoc, TextDisplay.class, t -> {
            t.setPersistent(false);
            t.setBillboard(Display.Billboard.FIXED);
            t.setRotation(base.getYaw(), 0);
            t.setLineWidth(220);
            t.setBackgroundColor(Color.fromARGB(215, 14, 18, 25));
            t.setShadowed(false);
            t.setAlignment(TextDisplay.TextAlignment.CENTER);
            t.setTransformation(Displays.trs(new Vector3f(), new Quaternionf(), 0.8f));
            t.text(Hud.pixel("RANKING", Hud.ACCENT));
            t.setBrightness(new Display.Brightness(15, 15));
        });
        spawned.add(leaderboard);
        // minimalistyczna ramka: 4 listwy + świecące narożniki
        float w = 2.6f, h = 3.4f, t = 0.06f;
        Vector3f c = new Vector3f(0, 1.2f + h / 2 - 0.15f, 0);
        frameBar(base, new Vector3f(c.x - w / 2, c.y + h / 2, -0.03f), new Vector3f(w, t, t), Material.LIGHT_GRAY_CONCRETE);
        frameBar(base, new Vector3f(c.x - w / 2, c.y - h / 2 - t, -0.03f), new Vector3f(w, t, t), Material.LIGHT_GRAY_CONCRETE);
        frameBar(base, new Vector3f(c.x - w / 2 - t, c.y - h / 2 - t, -0.03f), new Vector3f(t, h + 2 * t, t), Material.LIGHT_GRAY_CONCRETE);
        frameBar(base, new Vector3f(c.x + w / 2, c.y - h / 2 - t, -0.03f), new Vector3f(t, h + 2 * t, t), Material.LIGHT_GRAY_CONCRETE);
        for (float sx : new float[]{-1, 1}) {
            for (float sy : new float[]{-1, 1}) {
                frameBar(base, new Vector3f(c.x + sx * w / 2 - 0.08f, c.y + sy * h / 2 - 0.08f, -0.06f),
                        new Vector3f(0.16f, 0.16f, 0.1f), Material.SEA_LANTERN);
            }
        }
    }

    private void frameBar(Location base, Vector3f translation, Vector3f size, Material mat) {
        BlockDisplay d = base.getWorld().spawn(base, BlockDisplay.class, b -> {
            b.setPersistent(false);
            b.setBlock(mat.createBlockData());
            b.setRotation(base.getYaw(), 0);
            b.setTransformation(new Transformation(translation, new Quaternionf(), size, new Quaternionf()));
            b.setBrightness(new Display.Brightness(15, 15));
        });
        spawned.add(d);
    }

    private void refreshLeaderboard() {
        if (leaderboard == null) return;
        plugin.database().top(10).whenComplete((list, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (err != null || leaderboard == null || !leaderboard.isValid()) return;
            TextComponent.Builder b = Component.text();
            b.append(Hud.header("RANKING")).append(Component.newline()).append(Component.newline());
            int i = 1;
            for (PlayerProfile p : list) {
                b.append(Hud.text(i + ". ", i <= 3 ? Hud.GOLD : Hud.MUTED))
                        .append(Hud.text(p.name, Hud.TEXT))
                        .append(Hud.text("  " + p.elo, Hud.ACCENT))
                        .append(Component.newline());
                i++;
            }
            if (list.isEmpty()) b.append(Hud.text("Brak graczy", Hud.MUTED));
            leaderboard.text(b.build());
        }));
    }

    // postumenty
    private void spawnPedestal(Location l, int index) {
        keepLoaded(l);
        BlockDisplay pillar = l.getWorld().spawn(l, BlockDisplay.class, b -> {
            b.setPersistent(false);
            b.setBlock(Material.SMOOTH_QUARTZ.createBlockData());
            b.setTransformation(new Transformation(new Vector3f(-0.4f, 0, -0.4f), new Quaternionf(),
                    new Vector3f(0.8f, 1f, 0.8f), new Quaternionf()));
        });
        spawned.add(pillar);
        ItemDisplay top = l.getWorld().spawn(l.clone().add(0, 1.5, 0), ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.setItemStack(ItemFactory.model(WorldModel.PEDESTAL_TOP));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setBrightness(new Display.Brightness(15, 15));
        });
        spawned.add(top);
        Pedestal ped = new Pedestal();
        ped.index = index * 4;
        RivalsItem item = gadget(ped.index);
        ped.model = l.getWorld().spawn(l.clone().add(0, 1.65, 0), ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.setItemStack(ItemFactory.model(item.modelKey()));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setTransformation(Displays.trs(new Vector3f(), new Quaternionf(), 0.9f));
            d.setBrightness(new Display.Brightness(15, 15));
        });
        spawned.add(ped.model);
        TextDisplay label = l.getWorld().spawn(l.clone().add(0, 2.55, 0), TextDisplay.class, t -> {
            t.setPersistent(false);
            t.setBillboard(Display.Billboard.CENTER);
            t.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            t.setShadowed(false);
            t.text(Hud.pixel(item.displayName, Hud.TEXT));
            t.setTransformation(Displays.trs(new Vector3f(), new Quaternionf(), 0.7f));
        });
        spawned.add(label);
        pedestals.add(ped);
        labels.add(label);
    }

    private final List<TextDisplay> labels = new ArrayList<>();

    private static RivalsItem gadget(int i) {
        List<RivalsItem> g = RivalsItem.of(RivalsItem.Slot.GADGET);
        return g.get(Math.floorMod(i, g.size()));
    }

    // NPC
    private void spawnNpc(Location l) {
        keepLoaded(l);
        LivingEntity npc = null;
        try {
            Entity e = l.getWorld().spawnEntity(l, EntityType.valueOf("MANNEQUIN"));
            try {
                e.getClass().getMethod("setImmovable", boolean.class).invoke(e, true);
                e.getClass().getMethod("setDescription", Component.class).invoke(e, Component.empty());
            } catch (ReflectiveOperationException ignored) {
            }
            if (e instanceof LivingEntity le) npc = le;
        } catch (IllegalArgumentException | LinkageError ignored) {
            // < 1.21.9 - zastępczy NPC
        }
        if (npc == null) {
            npc = l.getWorld().spawn(l, Villager.class, v -> {
                v.setAI(false);
                v.setProfession(Villager.Profession.CARTOGRAPHER);
            });
        }
        npc.setPersistent(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setCollidable(false);
        npc.setRotation(l.getYaw(), 0);
        npcIds.add(npc.getUniqueId());
        spawned.add(npc);
        TextDisplay name = l.getWorld().spawn(l.clone().add(0, 2.35, 0), TextDisplay.class, t -> {
            t.setPersistent(false);
            t.setBillboard(Display.Billboard.CENTER);
            t.setBackgroundColor(Color.fromARGB(200, 14, 18, 25));
            t.setShadowed(false);
            t.text(Component.text().append(Hud.header("KOLEJKA 1V1")).append(Component.newline())
                    .append(Hud.text("Kliknij, aby grać", Hud.TEXT)).build());
            t.setBrightness(new Display.Brightness(15, 15));
        });
        spawned.add(name);
    }

    @EventHandler
    public void onInteractNpc(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !npcIds.contains(e.getRightClicked().getUniqueId())) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (plugin.queue().isQueued(p.getUniqueId())) plugin.queue().leave(p, false);
        else plugin.queue().join(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onNpcDamage(EntityDamageEvent e) {
        if (npcIds.contains(e.getEntity().getUniqueId())) e.setCancelled(true);
    }

    // tick
    private void tick() {
        tick++;
        int cycle = plugin.settings().pedestalCycleTicks;
        for (int i = 0; i < pedestals.size(); i++) {
            Pedestal p = pedestals.get(i);
            if (p.model == null || !p.model.isValid()) continue;
            long phase = tick % cycle;
            if (phase == cycle - 6) {
                Displays.animate(p.model, 5, Displays.trs(new Vector3f(), new Quaternionf().rotationY(p.angle), 0.01f));
            } else if (phase == 0) {
                p.index++;
                RivalsItem item = gadget(p.index);
                p.model.setItemStack(ItemFactory.model(item.modelKey()));
                if (i < labels.size()) labels.get(i).text(Hud.pixel(item.displayName, Hud.TEXT));
                Displays.animate(p.model, 6, Displays.trs(new Vector3f(), new Quaternionf().rotationY(p.angle), 0.9f));
            } else if (phase % 10 == 7 && phase < cycle - 10) {
                p.angle += (float) Math.toRadians(90);
                float bob = (float) Math.sin(tick / 15.0) * 0.08f;
                Displays.animate(p.model, 10, Displays.trs(new Vector3f(0, bob, 0), new Quaternionf().rotationY(p.angle), 0.9f));
            }
        }
        if (tick % plugin.settings().leaderboardRefreshTicks == 0) refreshLeaderboard();
    }
}
