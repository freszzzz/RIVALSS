package pl.rivals.combat;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pl.rivals.anim.Fx;
import pl.rivals.item.ItemFactory;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.WorldModel;

import java.util.Collection;

/** Skróty do item_display: tworzenie (widoczne tylko dla graczy meczu) i animacja przez interpolację. */
public final class Displays {

    private Displays() {
    }

    public static ItemDisplay spawn(Plugin plugin, Location loc, ItemStack stack, Collection<? extends Player> viewers,
                                    ItemDisplay.ItemDisplayTransform mode, float scale) {
        Transformation t = trs(new Vector3f(), new Quaternionf(), scale);
        return Fx.spawnFor(plugin, loc, ItemDisplay.class, viewers, d -> {
            d.setItemStack(stack);
            d.setItemDisplayTransform(mode);
            d.setTransformation(t);
            d.setInterpolationDuration(0);
            d.setTeleportDuration(2);
            d.setViewRange(1.2f);
        });
    }

    public static ItemDisplay world(Plugin plugin, Location loc, WorldModel model, Collection<? extends Player> viewers, float scale) {
        return spawn(plugin, loc, ItemFactory.model(model), viewers, ItemDisplay.ItemDisplayTransform.NONE, scale);
    }

    /** Model przedmiotu z ręki (np. granat w locie) - kontekst FIXED. */
    public static ItemDisplay item(Plugin plugin, Location loc, RivalsItem item, Collection<? extends Player> viewers, float scale) {
        return spawn(plugin, loc, ItemFactory.model(item.modelKey()), viewers, ItemDisplay.ItemDisplayTransform.FIXED, scale);
    }

    public static Transformation trs(Vector3f translation, Quaternionf rotation, float scale) {
        return new Transformation(translation, rotation, new Vector3f(scale, scale, scale), new Quaternionf());
    }

    public static Transformation trs(Vector3f translation, Quaternionf rotation, Vector3f scale) {
        return new Transformation(translation, rotation, scale, new Quaternionf());
    }

    /** Ustawia stan docelowy - klient płynnie dojdzie do niego w ciągu duration ticków. */
    public static void animate(Display d, int duration, Transformation target) {
        if (d == null || !d.isValid()) return;
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(Math.max(0, duration));
        d.setTransformation(target);
    }

    public static void remove(Display d) {
        if (d != null && d.isValid()) d.remove();
    }
}
