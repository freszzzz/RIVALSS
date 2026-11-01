package pl.rivals.match;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import pl.rivals.RivalsPlugin;
import pl.rivals.config.Messages;
import pl.rivals.config.RivalsConfig;

/** Przeciwnik do testów w pojedynkę (/rivals test). Husk nie płonie w dzień. */
public final class TrainingBot {

    private final RivalsPlugin plugin;
    private final Component displayName;
    private Husk husk;

    public TrainingBot(RivalsPlugin plugin) {
        this.plugin = plugin;
        this.displayName = Messages.mm().deserialize(plugin.settings().botName);
    }

    public String plainName() {
        return PlainTextComponentSerializer.plainText().serialize(displayName);
    }

    public boolean isSpawned() {
        return husk != null && husk.isValid();
    }

    public LivingEntity entity() {
        return husk;
    }

    public void spawn(Location loc) {
        RivalsConfig c = plugin.settings();
        husk = loc.getWorld().spawn(loc, Husk.class, h -> {
            h.setPersistent(false);
            h.setRemoveWhenFarAway(false);
            h.setCanPickupItems(false);
            h.setAdult();
            h.customName(displayName);
            h.setCustomNameVisible(true);
            h.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            h.getEquipment().setItemInMainHandDropChance(0f);
            attr(h, Attribute.MAX_HEALTH, c.botHealth);
            attr(h, Attribute.MOVEMENT_SPEED, c.botSpeed);
            attr(h, Attribute.ATTACK_DAMAGE, c.botDamage);
            attr(h, Attribute.FOLLOW_RANGE, 48);
            attr(h, Attribute.SPAWN_REINFORCEMENTS, 0);
            h.setHealth(c.botHealth);
            h.setAI(false);
        });
    }

    private static void attr(LivingEntity e, Attribute a, double value) {
        AttributeInstance ai = e.getAttribute(a);
        if (ai != null) ai.setBaseValue(value);
    }

    public void heal() {
        if (!isSpawned()) return;
        AttributeInstance max = husk.getAttribute(Attribute.MAX_HEALTH);
        husk.setHealth(max == null ? 20 : max.getValue());
        husk.setFireTicks(0);
    }

    /** NoAI = stoi w miejscu (odliczanie, killcam). */
    public void freeze(boolean frozen) {
        if (!isSpawned()) return;
        husk.setAI(!frozen);
        if (frozen) husk.setTarget(null);
    }

    public void target(LivingEntity target) {
        if (isSpawned() && target != null) husk.setTarget(target);
    }

    public void teleport(Location loc) {
        if (isSpawned()) husk.teleport(loc);
    }

    public void remove() {
        if (husk != null && husk.isValid()) husk.remove();
        husk = null;
    }
}
