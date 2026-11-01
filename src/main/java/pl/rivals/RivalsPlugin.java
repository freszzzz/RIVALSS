package pl.rivals;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.rivals.anim.FragmentManager;
import pl.rivals.arena.ArenaRegistry;
import pl.rivals.arena.ArenaWorld;
import pl.rivals.command.RivalsCommand;
import pl.rivals.config.Messages;
import pl.rivals.config.RivalsConfig;
import pl.rivals.config.SoundFx;
import pl.rivals.data.Database;
import pl.rivals.data.ProfileManager;
import pl.rivals.data.Ranks;
import pl.rivals.gadget.Gadgets;
import pl.rivals.gui.MenuListener;
import pl.rivals.hud.HudManager;
import pl.rivals.item.ItemFactory;
import pl.rivals.lobby.LobbyItems;
import pl.rivals.lobby.LobbyManager;
import pl.rivals.lobby.LobbyObjects;
import pl.rivals.match.MatchListener;
import pl.rivals.match.MatchManager;
import pl.rivals.match.QueueManager;
import pl.rivals.match.Status;
import pl.rivals.pack.PackServer;
import pl.rivals.weapon.WeaponListener;
import pl.rivals.weapon.Weapons;

import java.io.File;
import java.util.logging.Level;

public final class RivalsPlugin extends JavaPlugin {

    /** Wersje plików konfiguracyjnych - starsze są odkładane jako kopia i zastępowane nowymi. */
    private static final int CONFIG_VERSION = 2;

    private RivalsConfig settings;
    private Messages messages;
    private SoundFx sounds;
    private Ranks ranks;
    private ArenaRegistry arenas;

    private Database database;
    private ProfileManager profiles;
    private LobbyManager lobby;
    private LobbyItems lobbyItems;
    private LobbyObjects lobbyObjects;
    private ArenaWorld arenaWorld;
    private FragmentManager fragments;
    private MatchManager matches;
    private QueueManager queue;
    private HudManager hud;
    private Gadgets gadgets;
    private Weapons weapons;
    private PackServer pack;

    @Override
    public void onEnable() {
        Status.init(this);
        ItemFactory.init(this);
        migrate("config.yml");
        migrate("arenas.yml");
        migrate("messages.yml");
        saveDefaultConfig();
        saveResourceIfMissing("arenas.yml");
        saveResourceIfMissing("messages.yml");
        loadSettings();

        database = new Database(this);
        try {
            database.open(new File(getDataFolder(), "rivals.db"));
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Nie można otworzyć bazy SQLite – wyłączam plugin.", ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        profiles = new ProfileManager(this);
        lobby = new LobbyManager(this);
        lobbyItems = new LobbyItems(this);
        gadgets = new Gadgets();
        weapons = new Weapons(this);
        arenaWorld = new ArenaWorld(this);
        arenaWorld.init();
        fragments = new FragmentManager(this);
        fragments.start();
        matches = new MatchManager(this);
        matches.start();
        queue = new QueueManager(this);
        queue.start();
        hud = new HudManager(this);
        hud.start();
        pack = new PackServer(this);
        pack.start();
        lobbyObjects = new LobbyObjects(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(profiles, this);
        pm.registerEvents(lobby, this);
        pm.registerEvents(lobbyItems, this);
        pm.registerEvents(lobbyObjects, this);
        pm.registerEvents(hud, this);
        pm.registerEvents(pack, this);
        pm.registerEvents(new MatchListener(this), this);
        pm.registerEvents(new WeaponListener(this), this);
        pm.registerEvents(new MenuListener(), this);

        RivalsCommand command = new RivalsCommand(this);
        PluginCommand pc = getCommand("rivals");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        }

        // obiekty lobby po załadowaniu światów
        Bukkit.getScheduler().runTask(this, () -> lobbyObjects.start());

        for (Player p : Bukkit.getOnlinePlayers()) {
            profiles.load(p);
            lobby.sendToLobby(p);
            pack.send(p);
        }
        getLogger().info("RIVALS włączony – aren: " + arenas.all().size());
    }

    @Override
    public void onDisable() {
        if (queue != null) queue.clear();
        if (matches != null) matches.shutdown();
        if (lobbyObjects != null) lobbyObjects.stop();
        if (hud != null) hud.shutdown();
        if (fragments != null) fragments.shutdown();
        if (pack != null) pack.stop();
        if (profiles != null) profiles.saveAll();
        if (database != null) database.close();
    }

    /** (Prze)ładowuje config.yml, messages.yml i arenas.yml. Trwające mecze zachowują swoje areny. */
    public void loadSettings() {
        reloadConfig();
        settings = new RivalsConfig(getConfig());
        messages = Messages.load(this, new File(getDataFolder(), "messages.yml"));
        sounds = new SoundFx(getConfig().getConfigurationSection("sounds"));
        ranks = new Ranks(getConfig());
        arenas = ArenaRegistry.load(this, new File(getDataFolder(), "arenas.yml"));
    }

    private void saveResourceIfMissing(String name) {
        if (!new File(getDataFolder(), name).exists()) saveResource(name, false);
    }

    /** Plik bez aktualnej config-version: kopia *.old.yml, w jego miejsce nowa wersja z jara. */
    private void migrate(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists()) return;
        int v = YamlConfiguration.loadConfiguration(f).getInt("config-version", 1);
        if (v >= CONFIG_VERSION) return;
        Object lobbyLoc = name.equals("config.yml") ? YamlConfiguration.loadConfiguration(f).get("lobby.location") : null;
        File backup = new File(getDataFolder(), name.replace(".yml", ".old.yml"));
        if (f.renameTo(backup)) {
            getLogger().warning(name + " z poprzedniej wersji zapisano jako " + backup.getName() + " – utworzono nowy plik.");
            if (lobbyLoc != null) {
                // ustawione lobby przechodzi do nowego configu
                saveDefaultConfig();
                reloadConfig();
                getConfig().set("lobby.location", lobbyLoc);
                saveConfig();
            }
        }
    }

    public RivalsConfig settings() { return settings; }
    public Messages messages() { return messages; }
    public SoundFx sounds() { return sounds; }
    public Ranks ranks() { return ranks; }
    public ArenaRegistry arenas() { return arenas; }
    public Database database() { return database; }
    public ProfileManager profiles() { return profiles; }
    public LobbyManager lobby() { return lobby; }
    public LobbyItems lobbyItems() { return lobbyItems; }
    public LobbyObjects lobbyObjects() { return lobbyObjects; }
    public ArenaWorld arenaWorld() { return arenaWorld; }
    public FragmentManager fragments() { return fragments; }
    public MatchManager matches() { return matches; }
    public QueueManager queue() { return queue; }
    public HudManager hud() { return hud; }
    public Gadgets gadgets() { return gadgets; }
    public Weapons weapons() { return weapons; }
    public PackServer pack() { return pack; }
}
