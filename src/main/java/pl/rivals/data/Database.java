package pl.rivals.data;

import pl.rivals.RivalsPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * SQLite (sterownik org.sqlite.JDBC jest dołączony do Papera).
 * Całe I/O odbywa się na jednym wątku roboczym - połączenie nigdy nie jest współdzielone.
 */
public final class Database {

    private static final String COLUMNS = "uuid, name, elo, coins, wins, losses, streak, best_streak, kills, deaths, "
            + "rounds_won, matches, kit, kill_effect, trail";

    private final RivalsPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Rivals-SQLite");
        t.setDaemon(true);
        return t;
    });
    private Connection connection;

    public Database(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(File file) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Nie można utworzyć " + parent);
        Class.forName("org.sqlite.JDBC");
        executor.submit(() -> {
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS players (
                          uuid TEXT PRIMARY KEY,
                          name TEXT NOT NULL,
                          elo INTEGER NOT NULL,
                          coins INTEGER NOT NULL DEFAULT 0,
                          wins INTEGER NOT NULL DEFAULT 0,
                          losses INTEGER NOT NULL DEFAULT 0,
                          streak INTEGER NOT NULL DEFAULT 0,
                          best_streak INTEGER NOT NULL DEFAULT 0,
                          kills INTEGER NOT NULL DEFAULT 0,
                          deaths INTEGER NOT NULL DEFAULT 0,
                          rounds_won INTEGER NOT NULL DEFAULT 0,
                          matches INTEGER NOT NULL DEFAULT 0
                        )""");
                st.execute("CREATE INDEX IF NOT EXISTS idx_players_elo ON players(elo DESC)");
                st.execute("CREATE TABLE IF NOT EXISTS cosmetics_owned (uuid TEXT NOT NULL, item TEXT NOT NULL, PRIMARY KEY(uuid, item))");
                st.execute("CREATE TABLE IF NOT EXISTS skins_equipped ("
                        + "uuid TEXT NOT NULL, weapon TEXT NOT NULL, skin TEXT NOT NULL, PRIMARY KEY(uuid, weapon))");
            }
            // starsze bazy nie mają tych kolumn
            Set<String> cols = new HashSet<>();
            try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("PRAGMA table_info(players)")) {
                while (rs.next()) cols.add(rs.getString("name"));
            }
            try (Statement st = connection.createStatement()) {
                if (!cols.contains("kit")) st.execute("ALTER TABLE players ADD COLUMN kit TEXT");
                if (!cols.contains("kill_effect")) st.execute("ALTER TABLE players ADD COLUMN kill_effect TEXT DEFAULT 'default'");
                if (!cols.contains("trail")) st.execute("ALTER TABLE players ADD COLUMN trail TEXT DEFAULT 'default'");
            }
            return null;
        }).get(10, TimeUnit.SECONDS);
    }

    public CompletableFuture<PlayerProfile> load(UUID uuid, String name, int startElo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadSync(uuid, name, startElo);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }, executor);
    }

    private PlayerProfile loadSync(UUID uuid, String name, int startElo) throws SQLException {
        PlayerProfile p = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT " + COLUMNS + " FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) p = read(rs);
            }
        }
        if (p == null) {
            p = new PlayerProfile(uuid, name, startElo);
            saveSync(p);
            return p;
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT item FROM cosmetics_owned WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) p.owned.add(rs.getString(1));
            }
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT weapon, skin FROM skins_equipped WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) p.skins.put(rs.getString(1), rs.getString(2));
            }
        }
        return p;
    }

    private static PlayerProfile read(ResultSet rs) throws SQLException {
        PlayerProfile p = new PlayerProfile(UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getInt("elo"));
        p.coins = rs.getInt("coins");
        p.wins = rs.getInt("wins");
        p.losses = rs.getInt("losses");
        p.streak = rs.getInt("streak");
        p.bestStreak = rs.getInt("best_streak");
        p.kills = rs.getInt("kills");
        p.deaths = rs.getInt("deaths");
        p.roundsWon = rs.getInt("rounds_won");
        p.matches = rs.getInt("matches");
        p.kit = rs.getString("kit");
        String ke = rs.getString("kill_effect");
        p.killEffect = ke == null ? "default" : ke;
        String tr = rs.getString("trail");
        p.trail = tr == null ? "default" : tr;
        return p;
    }

    /** Zapis asynchroniczny - przekaż kopię profilu ({@link PlayerProfile#copy()}). */
    public void save(PlayerProfile snapshot) {
        if (executor.isShutdown()) return;
        executor.execute(() -> {
            try {
                saveSync(snapshot);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.WARNING, "Błąd zapisu profilu " + snapshot.name, ex);
            }
        });
    }

    private void saveSync(PlayerProfile p) throws SQLException {
        boolean auto = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO players (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                            "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, elo=excluded.elo, coins=excluded.coins, " +
                            "wins=excluded.wins, losses=excluded.losses, streak=excluded.streak, best_streak=excluded.best_streak, " +
                            "kills=excluded.kills, deaths=excluded.deaths, rounds_won=excluded.rounds_won, matches=excluded.matches, " +
                            "kit=excluded.kit, kill_effect=excluded.kill_effect, trail=excluded.trail")) {
                ps.setString(1, p.uuid.toString());
                ps.setString(2, p.name);
                ps.setInt(3, p.elo);
                ps.setInt(4, p.coins);
                ps.setInt(5, p.wins);
                ps.setInt(6, p.losses);
                ps.setInt(7, p.streak);
                ps.setInt(8, p.bestStreak);
                ps.setInt(9, p.kills);
                ps.setInt(10, p.deaths);
                ps.setInt(11, p.roundsWon);
                ps.setInt(12, p.matches);
                ps.setString(13, p.kit);
                ps.setString(14, p.killEffect);
                ps.setString(15, p.trail);
                ps.executeUpdate();
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM cosmetics_owned WHERE uuid = ?")) {
                del.setString(1, p.uuid.toString());
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement("INSERT INTO cosmetics_owned (uuid, item) VALUES (?, ?)")) {
                for (String item : p.owned) {
                    ins.setString(1, p.uuid.toString());
                    ins.setString(2, item);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM skins_equipped WHERE uuid = ?")) {
                del.setString(1, p.uuid.toString());
                del.executeUpdate();
            }
            try (PreparedStatement ins = connection.prepareStatement("INSERT INTO skins_equipped (uuid, weapon, skin) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, String> e : p.skins.entrySet()) {
                    ins.setString(1, p.uuid.toString());
                    ins.setString(2, e.getKey());
                    ins.setString(3, e.getValue());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(auto);
        }
    }

    /** Ranking ELO (tablica 3D w lobby). */
    public CompletableFuture<List<PlayerProfile>> top(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerProfile> out = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT " + COLUMNS + " FROM players ORDER BY elo DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(read(rs));
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            return out;
        }, executor);
    }

    /** Czeka na zakończenie zaległych zapisów i zamyka połączenie. */
    public void close() {
        executor.execute(() -> {
            try {
                if (connection != null) connection.close();
            } catch (SQLException ignored) {
            }
        });
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Baza danych nie zakończyła zapisów w 10 s.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
