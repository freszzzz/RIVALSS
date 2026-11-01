package pl.rivals.config;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import pl.rivals.shared.RivalsItem;

import java.time.ZoneId;
import java.util.Locale;

/**
 * Typowany widok config.yml. Czasy przeliczone na ticki. Statystyki broni i gadżetów czytane
 * na bieżąco przez {@link #stat(RivalsItem, String)} - brakujące klucze pochodzą z config.yml w jarze.
 */
public final class RivalsConfig {

    private final FileConfiguration raw;

    public final String serverAddress;

    // resource pack
    public final boolean packEnabled;
    public final boolean packRequired;
    public final int packPort;
    public final String packHost;
    public final String packUrl;
    public final int packSendDelay;

    // hud
    public final ZoneId zone;
    public final boolean damageNumbers;
    public final boolean hitmarker;

    // lobby
    public final GameMode lobbyGameMode;
    public final boolean lobbyProtect;
    public final boolean teleportOnJoin;
    public final boolean lobbyItems;
    public final int leaderboardRefreshTicks;
    public final int pedestalCycleTicks;

    // mecz
    public final int roundsToWin;
    public final int maxParallel;
    public final int kitSelectTicks;
    public final int countdownTicks;
    public final int roundMaxTicks;
    public final int suddenDeathStartTicks;
    public final int ringIntervalTicks;
    public final int ringCrackTicks;
    public final int ringMinRadius;
    public final int killcamTicks;
    public final int intermissionTicks;
    public final int endReturnTicks;
    public final boolean timeoutByHealth;
    public final boolean naturalRegeneration;
    public final boolean fallDamage;
    public final double maxHealth;
    public final int voidDepth;

    // kolejka
    public final int eloRangeStart;
    public final double eloRangeGrowth;

    // świat aren
    public final String arenaWorldName;
    public final int arenaSpacing;
    public final int arenaBaseY;
    public final int buildBlocksPerTick;

    // zestaw
    public final String defaultKit;

    // nagrody / monety / kapsuła
    public final int rewardRoundWin;
    public final int rewardMatchWin;
    public final int rewardKill;
    public final int streakBonusPerWin;
    public final int streakBonusMax;
    public final boolean botRewards;
    public final int coinsPerRound;
    public final int coinPickupValue;
    public final int coinSpawnMinTicks;
    public final int coinSpawnMaxTicks;
    public final double coinPickupRadius;
    public final boolean capsuleEnabled;
    public final int capsuleTicks;
    public final double capsuleHeal;
    public final double capsulePickupRadius;

    public final boolean decorations;
    public final int decorationsMax;

    // elo
    public final int eloStart;
    public final int eloK;
    public final int eloMin;

    // bot
    public final String botName;
    public final double botHealth;
    public final double botSpeed;
    public final double botDamage;

    public final AnimationSettings anim;

    public RivalsConfig(FileConfiguration c) {
        this.raw = c;
        serverAddress = c.getString("server-address", "play.rivals.pl");

        packEnabled = c.getBoolean("resource-pack.enabled", true);
        packRequired = c.getBoolean("resource-pack.required", true);
        packPort = c.getInt("resource-pack.port", 8164);
        packHost = c.getString("resource-pack.host", "");
        packUrl = c.getString("resource-pack.url", "");
        packSendDelay = Math.max(1, c.getInt("resource-pack.send-delay-ticks", 10));

        ZoneId z;
        try {
            z = ZoneId.of(c.getString("hud.timezone", "Europe/Warsaw"));
        } catch (Exception ex) {
            z = ZoneId.systemDefault();
        }
        zone = z;
        damageNumbers = c.getBoolean("hud.damage-numbers", true);
        hitmarker = c.getBoolean("hud.hitmarker", true);

        lobbyGameMode = parseGameMode(c.getString("lobby.gamemode", "ADVENTURE"));
        lobbyProtect = c.getBoolean("lobby.protect", true);
        teleportOnJoin = c.getBoolean("lobby.teleport-on-join", true);
        lobbyItems = c.getBoolean("lobby.items", true);
        leaderboardRefreshTicks = Math.max(200, ticks(c.getDouble("lobby.leaderboard-refresh-seconds", 60)));
        pedestalCycleTicks = Math.max(40, ticks(c.getDouble("lobby.pedestal-cycle-seconds", 6)));

        roundsToWin = Math.max(1, c.getInt("match.rounds-to-win", 5));
        maxParallel = Math.max(1, c.getInt("match.max-parallel", 10));
        kitSelectTicks = Math.max(40, ticks(c.getDouble("match.kit-select-seconds", 15)));
        countdownTicks = Math.max(20, ticks(c.getDouble("match.countdown-seconds", 3)));
        roundMaxTicks = Math.max(200, ticks(c.getDouble("match.round-max-seconds", 90)));
        suddenDeathStartTicks = ticks(c.getDouble("match.sudden-death.start-second", 60));
        ringIntervalTicks = Math.max(20, ticks(c.getDouble("match.sudden-death.ring-interval-seconds", 5)));
        ringCrackTicks = Math.max(5, ticks(c.getDouble("match.sudden-death.ring-crack-seconds", 2)));
        ringMinRadius = Math.max(1, c.getInt("match.sudden-death.min-radius", 7));
        killcamTicks = ticks(c.getDouble("match.killcam-seconds", 3));
        intermissionTicks = ticks(c.getDouble("match.intermission-seconds", 10));
        endReturnTicks = ticks(c.getDouble("match.end-return-seconds", 8));
        timeoutByHealth = !"DRAW".equalsIgnoreCase(c.getString("match.timeout-winner", "HEALTH"));
        naturalRegeneration = c.getBoolean("match.natural-regeneration", false);
        fallDamage = c.getBoolean("match.fall-damage", false);
        maxHealth = Math.max(1, c.getDouble("match.max-health", 20));
        voidDepth = Math.max(2, c.getInt("match.void-depth", 10));

        eloRangeStart = c.getInt("queue.elo-range-start", 100);
        eloRangeGrowth = c.getDouble("queue.elo-range-growth-per-second", 10);

        arenaWorldName = c.getString("arena-world.name", "rivals_arenas");
        arenaSpacing = Math.max(128, c.getInt("arena-world.spacing", 400));
        arenaBaseY = c.getInt("arena-world.base-y", 100);
        buildBlocksPerTick = Math.max(500, c.getInt("arena-world.build-blocks-per-tick", 6000));

        defaultKit = c.getString("kits.default", "rifle,pistol,sword,cryo_grenade");

        rewardRoundWin = c.getInt("rewards.round-win", 5);
        rewardMatchWin = c.getInt("rewards.match-win", 25);
        rewardKill = c.getInt("rewards.kill", 3);
        streakBonusPerWin = c.getInt("rewards.streak-bonus-per-win", 5);
        streakBonusMax = c.getInt("rewards.streak-bonus-max", 25);
        botRewards = c.getBoolean("rewards.bot-matches-give-rewards", false);
        coinsPerRound = Math.max(0, c.getInt("coins.per-round", 2));
        coinPickupValue = c.getInt("coins.pickup-value", 2);
        int cmin = ticks(c.getDouble("coins.spawn-after-min", 15));
        int cmax = ticks(c.getDouble("coins.spawn-after-max", 35));
        coinSpawnMinTicks = Math.max(20, Math.min(cmin, cmax));
        coinSpawnMaxTicks = Math.max(coinSpawnMinTicks, cmax);
        coinPickupRadius = c.getDouble("coins.pickup-radius", 1.2);
        capsuleEnabled = c.getBoolean("capsule.enabled", true);
        capsuleTicks = ticks(c.getDouble("capsule.after", 20));
        capsuleHeal = c.getDouble("capsule.heal", 6);
        capsulePickupRadius = c.getDouble("capsule.pickup-radius", 1.3);

        decorations = c.getBoolean("decorations.enabled", true);
        decorationsMax = c.getInt("decorations.max-per-arena", 60);

        eloStart = c.getInt("elo.start", 1000);
        eloK = c.getInt("elo.k-factor", 32);
        eloMin = c.getInt("elo.min", 0);

        botName = c.getString("training-bot.name", "<#FF4D5E>Trening");
        botHealth = Math.max(1, c.getDouble("training-bot.health", 20));
        botSpeed = c.getDouble("training-bot.speed", 0.28);
        botDamage = c.getDouble("training-bot.damage", 4);

        String w = "animation.wall.";
        anim = new AnimationSettings(
                c.getDouble(w + "crack-center-speedup", 0.35),
                c.getDouble(w + "wave-ticks-per-block", 1.0),
                Math.max(0, c.getInt(w + "fragments-min", 4)),
                Math.max(1, c.getInt(w + "fragments-max", 8)),
                Math.max(4, c.getInt(w + "fragment-life-ticks", 20)),
                (float) c.getDouble(w + "fragment-size-min", 0.22),
                (float) c.getDouble(w + "fragment-size-max", 0.40),
                (float) c.getDouble(w + "fragment-speed", 0.22),
                (float) c.getDouble(w + "fragment-gravity", 0.045),
                Math.max(1, c.getInt(w + "keyframe-ticks", 2)),
                Math.max(1, c.getInt(w + "rebuild-grow-ticks", 5)),
                Math.max(1, c.getInt(w + "max-break-sounds-per-tick", 2)),
                Math.max(0, c.getInt("animation.max-fragments", 300)),
                Math.max(5, c.getInt("animation.fragment-cleanup-ticks", 30)),
                Math.max(0, c.getInt("animation.max-rebuild-displays", 220))
        );
    }

    // statystyki przedmiotów
    private static String section(RivalsItem item) {
        return item.slot == RivalsItem.Slot.GADGET ? "gadgets." : "weapons.";
    }

    /** Liczba z sekcji weapons.&lt;id&gt; / gadgets.&lt;id&gt; (domyślne z jara). */
    public double stat(RivalsItem item, String key) {
        return raw.getDouble(section(item) + item.id + "." + key);
    }

    public int statInt(RivalsItem item, String key) {
        return (int) Math.round(stat(item, key));
    }

    /** Wartość w sekundach przeliczona na ticki. */
    public int statTicks(RivalsItem item, String key) {
        return Math.max(0, ticks(stat(item, key)));
    }

    public int price(String cosmeticId) {
        return raw.getInt("shop.prices." + cosmeticId, 100);
    }

    public FileConfiguration raw() {
        return raw;
    }

    public static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0);
    }

    private static GameMode parseGameMode(String s) {
        try {
            return GameMode.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return GameMode.ADVENTURE;
        }
    }
}
