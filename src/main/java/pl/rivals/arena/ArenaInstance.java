package pl.rivals.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Projectile;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import pl.rivals.RivalsPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/** Jedna fizyczna kopia areny w świecie aren (slot). */
public final class ArenaInstance {

    private final RivalsPlugin plugin;
    private final World world;
    private final int slot;
    private final int ox;
    private final int oy;
    private final int oz;

    private ArenaTemplate template;
    private ArenaTemplate builtTemplate;
    private boolean inUse;
    private BuildTask buildTask;
    private boolean ticketsAdded;
    private int ticketHalf;

    ArenaInstance(RivalsPlugin plugin, World world, int slot, int ox, int oy, int oz) {
        this.plugin = plugin;
        this.world = world;
        this.slot = slot;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
    }

    // cykl życia
    void claim(ArenaTemplate t) {
        this.template = t;
        this.inUse = true;
        addTickets();
    }

    void free() {
        if (buildTask != null) buildTask.cancel();
        buildTask = null;
        inUse = false;
        removeTickets();
    }

    /** Stawia arenę (tylko bloki różniące się od planu). done wywoływane na głównym wątku. */
    public void build(Runnable done) {
        if (buildTask != null) buildTask.cancel();
        builtTemplate = null;
        clearLooseEntities();
        buildTask = new BuildTask(plugin, this, () -> {
            builtTemplate = template;
            buildTask = null;
            done.run();
        });
        buildTask.runTaskTimer(plugin, 1L, 1L);
    }

    private void addTickets() {
        if (ticketsAdded) return;
        ticketHalf = plugin.arenas().clearHalf() + 2;
        int h = ticketHalf;
        for (int cx = (ox - h) >> 4; cx <= (ox + h) >> 4; cx++)
            for (int cz = (oz - h) >> 4; cz <= (oz + h) >> 4; cz++)
                world.addPluginChunkTicket(cx, cz, plugin);
        ticketsAdded = true;
    }

    private void removeTickets() {
        if (!ticketsAdded) return;
        int h = ticketHalf;
        for (int cx = (ox - h) >> 4; cx <= (ox + h) >> 4; cx++)
            for (int cz = (oz - h) >> 4; cz <= (oz + h) >> 4; cz++)
                world.removePluginChunkTicket(cx, cz, plugin);
        ticketsAdded = false;
    }

    /** Usuwa strzały, przedmioty i kule doświadczenia z obszaru areny. */
    public void clearLooseEntities() {
        for (Entity e : world.getNearbyEntities(bounds())) {
            if (e instanceof Projectile || e instanceof Item || e instanceof ExperienceOrb) e.remove();
        }
    }

    // geometria
    public BoundingBox bounds() {
        int h = plugin.arenas().clearHalf();
        return new BoundingBox(ox - h, oy - 40, oz - h, ox + h + 1, oy + plugin.arenas().clearTop() + 20, oz + h + 1);
    }

    public BlockVec abs(BlockVec rel) {
        return new BlockVec(ox + rel.x(), oy + rel.y(), oz + rel.z());
    }

    /** Strona 0 startuje na z = -d (patrzy na +z), strona 1 na z = +d (patrzy na -z). */
    public Location spawn(int side) {
        int d = template.spawnDistance();
        int z = side == 0 ? -d : d;
        float yaw = side == 0 ? 0f : 180f;
        return new Location(world, ox + 0.5, oy + 1, oz + z + 0.5, yaw, 0f);
    }

    public Location center() {
        return new Location(world, ox + 0.5, oy + 1, oz + 0.5);
    }

    public Vector wallCenter() {
        return new Vector(ox + 0.5, oy + 1 + template.wallHeight() / 2.0, oz + 0.5);
    }

    public Vector floorCenter() {
        return new Vector(ox + 0.5, oy + 0.5, oz + 0.5);
    }

    public double voidY() {
        return oy - plugin.settings().voidDepth;
    }

    /** Bloki ściany (współrzędne absolutne). */
    public Map<BlockVec, BlockData> wallBlocks() {
        return toAbs(template.blueprint().wall());
    }

    /** Bloki pierścienia o promieniu r (współrzędne absolutne). */
    public Map<BlockVec, BlockData> ringBlocks(int r) {
        return toAbs(template.blueprint().ring(r));
    }

    private Map<BlockVec, BlockData> toAbs(Map<BlockVec, BlockData> rel) {
        Map<BlockVec, BlockData> out = new LinkedHashMap<>();
        for (Map.Entry<BlockVec, BlockData> e : rel.entrySet()) out.put(abs(e.getKey()), e.getValue());
        return out;
    }

    // gettery
    public World world() { return world; }
    public int slot() { return slot; }
    public int ox() { return ox; }
    public int oy() { return oy; }
    public int oz() { return oz; }
    public ArenaTemplate template() { return template; }
    public ArenaTemplate builtTemplate() { return builtTemplate; }
    public boolean inUse() { return inUse; }
    public boolean building() { return buildTask != null; }
}
