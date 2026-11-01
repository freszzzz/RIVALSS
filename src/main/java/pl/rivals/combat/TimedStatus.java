package pl.rivals.combat;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import pl.rivals.match.Status;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Efekt statusu na czas: zamrożenie (szybkość i skok = 0) albo spowolnienie (szybkość × (1 - slow), bez skoku).
 * Każda instancja ma własny klucz modyfikatora - nakładające się efekty nie zdejmują się nawzajem.
 */
public final class TimedStatus implements MatchObject {

    private static final AtomicInteger IDS = new AtomicInteger();

    private final LivingEntity target;
    private final NamespacedKey key;
    private int ticksLeft;
    private boolean done;

    private TimedStatus(Plugin plugin, LivingEntity target, int ticks) {
        this.target = target;
        this.key = new NamespacedKey(plugin, "fx_" + IDS.incrementAndGet());
        this.ticksLeft = ticks;
    }

    public static TimedStatus freeze(Plugin plugin, LivingEntity target, int ticks) {
        TimedStatus s = new TimedStatus(plugin, target, ticks);
        Status.freeze(target, s.key);
        return s;
    }

    /** slow = 0.7 -> szybkość × 0.3; skok zablokowany. */
    public static TimedStatus slow(Plugin plugin, LivingEntity target, double slow, int ticks) {
        TimedStatus s = new TimedStatus(plugin, target, ticks);
        Status.set(target, Attribute.MOVEMENT_SPEED, s.key, -Math.max(0, Math.min(1, slow)), AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        Status.set(target, Attribute.JUMP_STRENGTH, s.key, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        return s;
    }

    /** Skraca efekt (np. rozbicie lodu). */
    public void end() {
        ticksLeft = 0;
    }

    public boolean active() {
        return !done && ticksLeft > 0;
    }

    @Override
    public boolean tick() {
        return --ticksLeft > 0 && target.isValid();
    }

    @Override
    public void remove() {
        if (done) return;
        done = true;
        Status.unfreeze(target, key);
    }
}
