package pl.rivals.pack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import pl.rivals.config.RivalsConfig;
import pl.rivals.hud.Hud;
import pl.rivals.RivalsPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.HexFormat;
import java.util.logging.Level;
import java.util.Map;
import java.util.UUID;

/**
 * Resource pack: plik z jara (wygenerowany przy budowie) trafia do plugins/Rivals/resourcepack/,
 * jest serwowany przez wbudowany serwer HTTP i wysyłany graczom przy wejściu z hashem SHA-1.
 * Własny plik: podmień rivals-pack.zip w tym folderze i ustaw resource-pack.keep-custom-file: true.
 */
public final class PackServer implements Listener {

    private static final UUID PACK_ID = UUID.nameUUIDFromBytes("rivals-pack".getBytes());

    private final RivalsPlugin plugin;
    private final Map<UUID, String> hosts = new ConcurrentHashMap<>();
    private HttpServer http;
    private byte[] data;
    private String sha1;

    public PackServer(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        RivalsConfig cfg = plugin.settings();
        if (!cfg.packEnabled) return;
        try {
            File dir = new File(plugin.getDataFolder(), "resourcepack");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("Nie można utworzyć " + dir);
            File file = new File(dir, "rivals-pack.zip");
            boolean keep = cfg.raw().getBoolean("resource-pack.keep-custom-file", false);
            if (!keep || !file.exists()) {
                try (InputStream in = plugin.getResource("rivals-pack.zip")) {
                    if (in == null) throw new IOException("Brak rivals-pack.zip w jarze pluginu");
                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            data = Files.readAllBytes(file.toPath());
            sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
            if (cfg.packUrl.isBlank()) {
                http = HttpServer.create(new InetSocketAddress(cfg.packPort), 0);
                http.createContext("/", this::serve);
                http.setExecutor(Executors.newFixedThreadPool(2, r -> {
                    Thread t = new Thread(r, "Rivals-PackHTTP");
                    t.setDaemon(true);
                    return t;
                }));
                http.start();
                plugin.getLogger().info("Resource pack: http na porcie " + cfg.packPort + ", SHA-1 " + sha1 + " (" + data.length / 1024 + " KiB)");
                selfTest(cfg.packPort);
            } else {
                plugin.getLogger().info("Resource pack: zewnętrzny URL " + cfg.packUrl + ", SHA-1 " + sha1);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się uruchomić resource packa", ex);
            data = null;
        }
    }

    /** Pobiera pack z własnego serwera HTTP (127.0.0.1) i porównuje SHA-1 - wynik w konsoli. */
    private void selfTest(int port) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String url = "http://127.0.0.1:" + port + "/rivals-test.zip";
            try {
                var conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(5000);
                byte[] got;
                try (InputStream in = conn.getInputStream()) {
                    got = in.readAllBytes();
                }
                String h = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(got));
                if (h.equals(sha1)) {
                    plugin.getLogger().info("Resource pack: test lokalny OK (" + url + "). Gracze spoza tego komputera"
                            + " muszą mieć dostęp do portu " + port + " (przekierowanie portu / firewall) albo ustaw resource-pack.url.");
                } else {
                    plugin.getLogger().warning("Resource pack: test lokalny – niezgodny SHA-1 pobranego pliku!");
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Resource pack: test lokalny NIEUDANY (" + url + "): " + ex);
            }
        });
    }

    public void stop() {
        if (http != null) http.stop(0);
        http = null;
    }

    private void serve(HttpExchange ex) throws IOException {
        try (ex) {
            if (data == null || !ex.getRequestURI().getPath().endsWith(".zip")) {
                ex.sendResponseHeaders(404, -1);
                return;
            }
            ex.getResponseHeaders().add("Content-Type", "application/zip");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(data);
            }
        }
    }

    /** Ostatnio wysłany URL dla gracza - do komunikatu przy błędzie. */
    private final Map<UUID, String> sentUrls = new ConcurrentHashMap<>();

    public String url(Player p) {
        RivalsConfig cfg = plugin.settings();
        if (!cfg.packUrl.isBlank()) return cfg.packUrl;
        String host = cfg.packHost;
        if (host == null || host.isBlank()) host = hosts.get(p.getUniqueId());
        if (host == null || host.isBlank()) host = Bukkit.getIp();
        if (host == null || host.isBlank()) host = "localhost";
        return "http://" + host + ":" + cfg.packPort + "/rivals-" + sha1.substring(0, 12) + ".zip";
    }

    public void send(Player p) {
        if (data == null) return;
        try {
            String url = url(p);
            sentUrls.put(p.getUniqueId(), url);
            plugin.getLogger().info("Resource pack → " + p.getName() + ": " + url);
            ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(PACK_ID, URI.create(url), sha1);
            ResourcePackRequest req = ResourcePackRequest.resourcePackRequest()
                    .packs(info)
                    .required(plugin.settings().packRequired)
                    .replace(true)
                    .prompt(plugin.messages().get("pack.prompt"))
                    .build();
            p.sendResourcePacks(req);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Nie wysłano packa do " + p.getName(), ex);
        }
    }

    /** Adres, którym gracz się połączył (bez portu) - link do packa działa też w sieci lokalnej. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        try {
            String h = cleanHost(e.getHostname());
            if (h != null) hosts.put(e.getUniqueId(), h);
        } catch (Throwable ignored) {
            // starsze/nowsze API bez getHostname - zostaje host z configu
        }
    }

    /**
     * Adres z handshake'u: bez znaczników klientów modowanych (po \0), bez końcowej kropki i portu.
     * @return null, gdy nie da się go użyć
     */
    static String cleanHost(String raw) {
        if (raw == null) return null;
        String h = raw;
        int nul = h.indexOf('\0');
        if (nul >= 0) h = h.substring(0, nul);
        h = h.trim();
        if (h.startsWith("[")) {
            int end = h.indexOf(']');
            h = end > 0 ? h.substring(0, end + 1) : h;
        } else {
            int colon = h.lastIndexOf(':');
            if (colon > 0 && h.indexOf(':') == colon) h = h.substring(0, colon);
        }
        while (h.endsWith(".")) h = h.substring(0, h.length() - 1);
        if (h.isEmpty() || h.contains(" ") || h.contains("/")) return null;
        return h;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) send(p);
        }, plugin.settings().packSendDelay);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        hosts.remove(e.getPlayer().getUniqueId());
        sentUrls.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent e) {
        Player p = e.getPlayer();
        String url = sentUrls.getOrDefault(p.getUniqueId(), "?");
        switch (e.getStatus()) {
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD -> plugin.getLogger().warning("Resource pack: " + p.getName()
                    + " – " + e.getStatus() + " (" + url + "). Klient nie może pobrać pliku z tego adresu – sprawdź,"
                    + " czy port " + plugin.settings().packPort + " jest dostępny z zewnątrz, albo ustaw resource-pack.host / resource-pack.url.");
            case SUCCESSFULLY_LOADED -> plugin.getLogger().info("Resource pack: " + p.getName() + " – wczytany.");
            default -> {
            }
        }
        if (!plugin.settings().packRequired) return;
        switch (e.getStatus()) {
            case DECLINED -> p.kick(plugin.messages().get("pack.declined"));
            case FAILED_DOWNLOAD, INVALID_URL -> p.kick(plugin.messages().get("pack.failed")
                    .append(Component.newline()).append(Component.newline())
                    .append(Hud.text(e.getStatus().name() + " · " + url, Hud.MUTED)));
            default -> {
            }
        }
    }
}
