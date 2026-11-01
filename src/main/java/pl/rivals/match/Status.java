package pl.rivals.match;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Efekty statusu realizowane atrybutami (bez mikstur): zamrożenie (szybkość i skok = 0),
 * grawitacja areny. Modyfikatory są przejściowe (nie zapisują się w danych gracza).
 */
public final class Status {

    public static NamespacedKey FREEZE;
    public static NamespacedKey GRAVITY;
    private static String namespace = "rivals";

    private static final List<Attribute> TOUCHED = List.of(
            Attribute.MOVEMENT_SPEED, Attribute.JUMP_STRENGTH, Attribute.GRAVITY, Attribute.SAFE_FALL_DISTANCE);

    private Status() {
    }

    public static void init(Plugin plugin) {
        FREEZE = new NamespacedKey(plugin, "freeze");
        GRAVITY = new NamespacedKey(plugin, "arena_gravity");
        namespace = FREEZE.getNamespace();
    }

    /** Szybkość i siła skoku × 0 (MULTIPLY_SCALAR_1 z -1). Różne klucze = niezależne źródła. */
    public static void freeze(LivingEntity e, NamespacedKey key) {
        set(e, Attribute.MOVEMENT_SPEED, key, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        set(e, Attribute.JUMP_STRENGTH, key, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }

    public static void unfreeze(LivingEntity e, NamespacedKey key) {
        remove(e, Attribute.MOVEMENT_SPEED, key);
        remove(e, Attribute.JUMP_STRENGTH, key);
    }

    /** Ustawia grawitację areny (vanilla 0.08) przez modyfikator ADD_NUMBER. */
    public static void setGravity(LivingEntity e, double gravity) {
        AttributeInstance ai = e.getAttribute(Attribute.GRAVITY);
        if (ai == null) return;
        remove(e, Attribute.GRAVITY, GRAVITY);
        double diff = gravity - ai.getBaseValue();
        if (Math.abs(diff) > 1.0e-6) set(e, Attribute.GRAVITY, GRAVITY, diff, AttributeModifier.Operation.ADD_NUMBER);
        // przy niskiej grawitacji upadki są dłuższe - zwiększamy bezpieczny dystans
        remove(e, Attribute.SAFE_FALL_DISTANCE, GRAVITY);
        if (gravity < ai.getBaseValue() - 1.0e-6) {
            set(e, Attribute.SAFE_FALL_DISTANCE, GRAVITY, 6, AttributeModifier.Operation.ADD_NUMBER);
        }
    }

    /** Zdejmuje wszystkie modyfikatory pluginu z obsługiwanych atrybutów. */
    public static void clearAll(LivingEntity e) {
        for (Attribute a : TOUCHED) {
            AttributeInstance ai = e.getAttribute(a);
            if (ai == null) continue;
            List<AttributeModifier> mine = new ArrayList<>();
            for (AttributeModifier m : ai.getModifiers()) {
                if (namespace.equals(m.getKey().getNamespace())) mine.add(m);
            }
            for (AttributeModifier m : mine) ai.removeModifier(m);
        }
    }

    public static void set(LivingEntity e, Attribute attribute, NamespacedKey key, double amount,
                           AttributeModifier.Operation op) {
        AttributeInstance ai = e.getAttribute(attribute);
        if (ai == null) return;
        remove(e, attribute, key);
        ai.addTransientModifier(new AttributeModifier(key, amount, op, EquipmentSlotGroup.ANY));
    }

    public static void remove(LivingEntity e, Attribute attribute, NamespacedKey key) {
        AttributeInstance ai = e.getAttribute(attribute);
        if (ai == null) return;
        AttributeModifier found = null;
        for (AttributeModifier m : ai.getModifiers()) {
            if (m.getKey().equals(key)) {
                found = m;
                break;
            }
        }
        if (found != null) ai.removeModifier(found);
    }
}
