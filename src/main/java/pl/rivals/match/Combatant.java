package pl.rivals.match;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import pl.rivals.combat.CombatState;

import java.util.UUID;

/** Uczestnik meczu - gracz albo bot treningowy. */
public final class Combatant {

    private final UUID id;
    private final String name;
    private final Player player;
    private final TrainingBot bot;
    private boolean left;

    int side;
    Combatant opponent;

    public int score;
    public int coinsEarned;
    public int streakBonus;
    public final MatchStats stats = new MatchStats();
    /** Zestaw, amunicja i cooldowny. */
    public final CombatState state = new CombatState();

    private Combatant(UUID id, String name, Player player, TrainingBot bot) {
        this.id = id;
        this.name = name;
        this.player = player;
        this.bot = bot;
    }

    public static Combatant of(Player p) {
        return new Combatant(p.getUniqueId(), p.getName(), p, null);
    }

    public static Combatant bot(TrainingBot bot) {
        return new Combatant(UUID.randomUUID(), bot.plainName(), null, bot);
    }

    /** Byt w świecie (null, gdy gracz wyszedł albo bot nie istnieje). */
    public LivingEntity entity() {
        if (bot != null) return bot.isSpawned() ? bot.entity() : null;
        return player();
    }

    /** Gracz online i wciąż w meczu (null dla bota). */
    public Player player() {
        return player != null && !left && player.isOnline() ? player : null;
    }

    public boolean is(Entity e) {
        if (e == null) return false;
        if (bot != null) return bot.isSpawned() && bot.entity().getUniqueId().equals(e.getUniqueId());
        return player != null && player.getUniqueId().equals(e.getUniqueId());
    }

    void markLeft() {
        left = true;
    }

    public boolean isBot() { return bot != null; }
    public TrainingBot bot() { return bot; }
    public UUID id() { return id; }
    public String name() { return name; }
    public Combatant opponent() { return opponent; }
}
