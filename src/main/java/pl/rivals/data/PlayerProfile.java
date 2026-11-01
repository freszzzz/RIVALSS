package pl.rivals.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dane gracza. Modyfikowane wyłącznie na głównym wątku; do bazy trafia kopia. */
public final class PlayerProfile {

    public final UUID uuid;
    public String name;
    public int elo;
    public int coins;
    public int wins;
    public int losses;
    public int streak;
    public int bestStreak;
    public int kills;
    public int deaths;
    public int roundsWon;
    public int matches;

    /** Zestaw: "główna,dodatkowa,biała,gadżet" (null = domyślny z configu). */
    public String kit;
    /** Wybrany efekt zabójstwa (id kosmetyku albo "default"). */
    public String killEffect = "default";
    /** Wybrany kolor śladu doskoku (id kosmetyku albo "default"). */
    public String trail = "default";
    /** Kupione kosmetyki (id). */
    public final Set<String> owned = new HashSet<>();
    /** Założone skórki: id broni -> id skórki. */
    public final Map<String, String> skins = new HashMap<>();

    public PlayerProfile(UUID uuid, String name, int elo) {
        this.uuid = uuid;
        this.name = name;
        this.elo = elo;
    }

    public PlayerProfile copy() {
        PlayerProfile p = new PlayerProfile(uuid, name, elo);
        p.coins = coins;
        p.wins = wins;
        p.losses = losses;
        p.streak = streak;
        p.bestStreak = bestStreak;
        p.kills = kills;
        p.deaths = deaths;
        p.roundsWon = roundsWon;
        p.matches = matches;
        p.kit = kit;
        p.killEffect = killEffect;
        p.trail = trail;
        p.owned.addAll(owned);
        p.skins.putAll(skins);
        return p;
    }
}
