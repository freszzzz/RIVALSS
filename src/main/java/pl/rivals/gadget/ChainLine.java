package pl.rivals.gadget;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import pl.rivals.combat.Displays;
import pl.rivals.shared.WorldModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Łańcuch / linka z segmentów item_display między dwoma punktami. Segmenty są ustawiane
 * wzdłuż linii (yaw/pitch) i przesuwane z interpolacją teleportu.
 */
public final class ChainLine {

    private final Plugin plugin;
    private final WorldModel model;
    private final Collection<Player> viewers;
    private final double spacing;
    private final float scale;
    private final List<ItemDisplay> segments = new ArrayList<>();

    public ChainLine(Plugin plugin, WorldModel model, Collection<Player> viewers, double spacing, float scale) {
        this.plugin = plugin;
        this.model = model;
        this.viewers = viewers;
        this.spacing = spacing;
        this.scale = scale;
    }

    public void update(Location from, Location to) {
        World w = from.getWorld();
        Vector a = from.toVector();
        Vector d = to.toVector().subtract(a);
        double len = d.length();
        int need = Math.max(1, Math.min(40, (int) Math.ceil(len / spacing)));
        while (segments.size() < need) {
            segments.add(Displays.world(plugin, from, model, viewers, scale));
        }
        while (segments.size() > need) Displays.remove(segments.remove(segments.size() - 1));
        Vector dir = len < 1e-4 ? new Vector(0, 0, 1) : d.clone().multiply(1 / len);
        for (int i = 0; i < segments.size(); i++) {
            double t = (i + 0.5) / need;
            Location l = a.clone().add(d.clone().multiply(t)).toLocation(w);
            l.setDirection(dir);
            segments.get(i).teleport(l);
        }
    }

    public void remove() {
        for (ItemDisplay d : segments) Displays.remove(d);
        segments.clear();
    }
}
