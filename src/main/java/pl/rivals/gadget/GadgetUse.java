package pl.rivals.gadget;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.rivals.RivalsPlugin;
import pl.rivals.combat.Displays;
import pl.rivals.combat.SimProjectile;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.util.Collection;

/** Kontekst jednego użycia gadżetu. */
public record GadgetUse(RivalsPlugin plugin, Match match, Combatant user, Player player, RivalsItem item) {

    public Combatant target() {
        return user.opponent();
    }

    public Collection<Player> viewers() {
        return match.viewers();
    }

    public Location eye() {
        return player.getEyeLocation();
    }

    public Vector dir() {
        return player.getEyeLocation().getDirection().normalize();
    }

    public double stat(String key) {
        return plugin.settings().stat(item, key);
    }

    public int ticks(String key) {
        return plugin.settings().statTicks(item, key);
    }

    public void sound(SoundId id) {
        plugin.sounds().play(viewers(), id, player.getLocation());
    }

    public void sound(SoundId id, Location at) {
        plugin.sounds().play(viewers(), id, at);
    }

    /** Rzut przedmiotem: pocisk z modelem gadżetu (kontekst FIXED) startujący sprzed oczu. */
    public SimProjectile throwItem(double speed, double gravity, float scale) {
        Location start = eye().add(dir().multiply(0.6)).add(0, -0.15, 0);
        SimProjectile p = new SimProjectile(match, user, start, dir().multiply(speed).add(player.getVelocity().multiply(0.5)));
        p.gravity = gravity;
        p.display = Displays.item(plugin, start, item, viewers(), scale);
        p.displayScale = scale;
        match.effects().add(p);
        return p;
    }

    /** Pocisk z modelem świata (kontekst NONE). */
    public SimProjectile throwModel(WorldModel model, double speed, double gravity, float scale) {
        Location start = eye().add(dir().multiply(0.6)).add(0, -0.15, 0);
        SimProjectile p = new SimProjectile(match, user, start, dir().multiply(speed));
        p.gravity = gravity;
        ItemDisplay d = Displays.world(plugin, start, model, viewers(), scale);
        p.display = d;
        p.displayScale = scale;
        match.effects().add(p);
        return p;
    }
}
