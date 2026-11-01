package pl.rivals.arena;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import pl.rivals.RivalsPlugin;
import pl.rivals.config.RivalsConfig;

import java.util.function.Supplier;

/**
 * Świat z arenami. Każdy mecz dostaje własny slot (osobną kopię areny), sloty są oddalone
 * o arena-world.spacing bloków - gracze i efekty różnych meczów się nie widzą.
 */
public final class ArenaWorld {

    private final RivalsPlugin plugin;
    private World world;
    private ArenaInstance[] slots;

    public ArenaWorld(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        RivalsConfig cfg = plugin.settings();
        world = Bukkit.getWorld(cfg.arenaWorldName);
        if (world == null) {
            world = new WorldCreator(cfg.arenaWorldName)
                    .generator(new VoidGenerator())
                    .generateStructures(false)
                    .createWorld();
        }
        if (world == null) throw new IllegalStateException("Nie udało się utworzyć świata aren");

        world.setDifficulty(Difficulty.NORMAL);
        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);
        safe("pvp", () -> world.setPVP(true));
        // Gamerule zmieniają się między wersjami (1.21.9 usunęło spawnChunkRadius, 1.21.11 zmieniło nazwy).
        // Każda ustawiana osobno - brak reguły w danej wersji to tylko ostrzeżenie, nie wyłączenie pluginu.
        rule("doDaylightCycle", () -> GameRule.DO_DAYLIGHT_CYCLE, false);
        rule("doWeatherCycle", () -> GameRule.DO_WEATHER_CYCLE, false);
        rule("doMobSpawning", () -> GameRule.DO_MOB_SPAWNING, false);
        rule("doFireTick", () -> GameRule.DO_FIRE_TICK, false);
        rule("mobGriefing", () -> GameRule.MOB_GRIEFING, false);
        rule("keepInventory", () -> GameRule.KEEP_INVENTORY, true);
        rule("announceAdvancements", () -> GameRule.ANNOUNCE_ADVANCEMENTS, false);
        rule("showDeathMessages", () -> GameRule.SHOW_DEATH_MESSAGES, false);
        rule("doImmediateRespawn", () -> GameRule.DO_IMMEDIATE_RESPAWN, true);
        rule("naturalRegeneration", () -> GameRule.NATURAL_REGENERATION, cfg.naturalRegeneration);
        rule("fallDamage", () -> GameRule.FALL_DAMAGE, cfg.fallDamage);

        slots = new ArenaInstance[cfg.maxParallel];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new ArenaInstance(plugin, world, i, i * cfg.arenaSpacing, cfg.arenaBaseY, 0);
        }
    }

    /** Stała GameRule pobierana w lambdzie - brak pola w nowszej wersji (NoSuchFieldError) da się złapać. */
    private <T> void rule(String name, Supplier<GameRule<T>> rule, T value) {
        safe("gamerule " + name, () -> world.setGameRule(rule.get(), value));
    }

    private void safe(String what, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            plugin.getLogger().warning("Pominięto ustawienie świata aren (" + what + "): " + t);
        }
    }

    public World world() {
        return world;
    }

    public boolean isArenaWorld(World w) {
        return w != null && world != null && w.getUID().equals(world.getUID());
    }

    public boolean hasFree() {
        for (ArenaInstance s : slots) if (!s.inUse()) return true;
        return false;
    }

    /** Rezerwuje slot dla szablonu (preferuje slot, na którym ta arena już stoi). */
    public ArenaInstance acquire(ArenaTemplate template) {
        ArenaInstance pick = null;
        for (ArenaInstance s : slots) {
            if (s.inUse()) continue;
            if (s.builtTemplate() == template) {
                pick = s;
                break;
            }
            if (pick == null) pick = s;
        }
        if (pick == null) return null;
        pick.claim(template);
        return pick;
    }

    public void release(ArenaInstance instance) {
        instance.free();
    }

    public ArenaInstance[] slots() {
        return slots;
    }
}
