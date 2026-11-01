package pl.rivals.command;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import pl.rivals.arena.ArenaInstance;
import pl.rivals.arena.ArenaTemplate;
import pl.rivals.config.Messages;
import pl.rivals.data.PlayerProfile;
import pl.rivals.gui.ShopMenu;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;
import pl.rivals.match.TrainingBot;
import pl.rivals.RivalsPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static pl.rivals.config.Messages.ph;
import static pl.rivals.config.Messages.phc;

/** /rivals - komendy graczy i administracji. */
public final class RivalsCommand implements TabExecutor {

    private final RivalsPlugin plugin;

    public RivalsCommand(RivalsPlugin plugin) {
        this.plugin = plugin;
    }

    private Messages msg() {
        return plugin.messages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            msg().sendLines(sender, "general.help");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "graj", "play", "join" -> {
                Player p = player(sender);
                if (p != null) plugin.queue().join(p);
            }
            case "opusc", "opuść", "leave" -> {
                Player p = player(sender);
                if (p == null) break;
                Match m = plugin.matches().ofPlayer(p.getUniqueId());
                if (m != null) m.forfeit(p);
                else plugin.queue().leave(p, false);
            }
            case "statystyki", "stats", "profil" -> stats(sender, args);
            case "zestaw", "kit" -> {
                Player p = player(sender);
                if (p == null) break;
                if (plugin.matches().ofPlayer(p.getUniqueId()) != null) msg().send(p, "queue.in-match");
                else plugin.lobbyItems().openKit(p);
            }
            case "sklep", "shop" -> {
                Player p = player(sender);
                if (p != null) new ShopMenu(plugin, p).open();
            }
            case "pack" -> {
                Player p = player(sender);
                if (p != null) plugin.pack().send(p);
            }
            case "test" -> test(sender, args);
            case "admin" -> admin(sender, args);
            default -> msg().sendLines(sender, "general.help");
        }
        return true;
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player p) return p;
        msg().send(sender, "general.players-only");
        return null;
    }

    // gracz
    private void stats(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                msg().send(sender, "general.player-not-found", ph("name", args[1]));
                return;
            }
        } else {
            target = player(sender);
            if (target == null) return;
        }
        PlayerProfile p = plugin.profiles().get(target.getUniqueId());
        if (p == null) {
            msg().send(sender, "general.profile-loading");
            return;
        }
        TagResolver r = TagResolver.resolver(
                ph("name", p.name), phc("rank", Messages.mm().deserialize(plugin.ranks().of(p.elo).mini())),
                ph("elo", p.elo), ph("coins", p.coins), ph("streak", p.streak), ph("best", p.bestStreak),
                ph("matches", p.matches), ph("wins", p.wins), ph("losses", p.losses),
                ph("kills", p.kills), ph("deaths", p.deaths));
        msg().sendLines(sender, "stats.lines", r);
    }

    private void test(CommandSender sender, String[] args) {
        Player p = player(sender);
        if (p == null) return;
        if (!p.hasPermission("rivals.test")) {
            msg().send(p, "general.no-permission");
            return;
        }
        ArenaTemplate t = arenaArg(sender, args, 1);
        if (t == null) return;
        Match m = plugin.matches().create(Combatant.of(p), Combatant.bot(new TrainingBot(plugin)), t, true);
        if (m == null) msg().send(p, "admin.forcestart-fail");
    }

    /** Arena z argumentu albo losowa; null + komunikat, gdy nieznana. */
    private ArenaTemplate arenaArg(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            ArenaTemplate t = plugin.arenas().get(args[index]);
            if (t == null) {
                msg().send(sender, "general.unknown-arena", ph("arena", args[index]), ph("list", String.join(", ", plugin.arenas().ids())));
            }
            return t;
        }
        ArenaTemplate t = plugin.arenas().random();
        if (t == null) msg().send(sender, "general.unknown-arena", ph("arena", "-"), ph("list", "-"));
        return t;
    }

    // admin
    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rivals.admin")) {
            msg().send(sender, "general.no-permission");
            return;
        }
        if (args.length < 2) {
            msg().send(sender, "admin.usage");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "setlobby" -> {
                Player p = player(sender);
                if (p == null) return;
                plugin.lobby().setLobby(p.getLocation());
                msg().send(p, "admin.lobby-set");
            }
            case "reload" -> {
                plugin.loadSettings();
                msg().send(sender, "admin.reloaded", ph("count", plugin.arenas().all().size()));
            }
            case "forcestart" -> {
                if (args.length < 4) {
                    msg().send(sender, "admin.usage");
                    return;
                }
                Player p1 = Bukkit.getPlayerExact(args[2]);
                Player p2 = Bukkit.getPlayerExact(args[3]);
                if (p1 == null || p2 == null || p1.equals(p2)) {
                    msg().send(sender, "general.player-not-found", ph("name", p1 == null ? args[2] : args[3]));
                    return;
                }
                ArenaTemplate t = arenaArg(sender, args, 4);
                if (t == null) return;
                Match m = plugin.matches().create(Combatant.of(p1), Combatant.of(p2), t, false);
                if (m == null) msg().send(sender, "admin.forcestart-fail");
                else msg().send(sender, "admin.forcestart-ok", ph("id", m.id()));
            }
            case "stop" -> {
                if (args.length < 3) {
                    msg().send(sender, "admin.usage");
                    return;
                }
                Player t = Bukkit.getPlayerExact(args[2]);
                Match m = t == null ? null : plugin.matches().ofPlayer(t.getUniqueId());
                if (m == null) {
                    msg().send(sender, "admin.not-in-match");
                    return;
                }
                int id = m.id();
                m.abort();
                msg().send(sender, "admin.stopped", ph("id", id));
            }
            case "arenas" -> {
                msg().send(sender, "admin.arenas-header", ph("matches", plugin.matches().all().size()), ph("max", plugin.settings().maxParallel));
                for (ArenaTemplate t : plugin.arenas().all()) {
                    sender.sendMessage(msg().get("admin.arenas-template", ph("id", t.id()), ph("display", t.displayName()),
                            ph("gravity", t.gravity()), ph("state", t.enabled() ? "włączona" : "wyłączona")));
                }
                for (ArenaInstance s : plugin.arenaWorld().slots()) {
                    String state = !s.inUse() ? "wolny" + (s.builtTemplate() != null ? " (" + s.builtTemplate().id() + ")" : "")
                            : (s.building() ? "budowa: " : "mecz: ") + s.template().id();
                    sender.sendMessage(msg().get("admin.arenas-slot", ph("slot", s.slot()), ph("state", state)));
                }
            }
            case "lobby" -> {
                Player p = player(sender);
                if (p == null) return;
                String what = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
                switch (what) {
                    case "leaderboard", "ranking" -> plugin.lobbyObjects().setLeaderboard(p.getLocation());
                    case "pedestal", "postument" -> plugin.lobbyObjects().addPedestal(p.getLocation());
                    case "npc" -> plugin.lobbyObjects().setNpc(p.getLocation());
                    case "clear" -> plugin.lobbyObjects().clear();
                    default -> {
                        msg().send(sender, "admin.lobby-usage");
                        return;
                    }
                }
                msg().send(sender, "admin.lobby-object", ph("what", what));
            }
            case "coins" -> {
                if (args.length < 4) {
                    msg().send(sender, "admin.usage");
                    return;
                }
                Player t = Bukkit.getPlayerExact(args[2]);
                PlayerProfile prof = t == null ? null : plugin.profiles().get(t.getUniqueId());
                if (prof == null) {
                    msg().send(sender, "general.player-not-found", ph("name", args[2]));
                    return;
                }
                try {
                    prof.coins = Math.max(0, prof.coins + Integer.parseInt(args[3]));
                } catch (NumberFormatException ex) {
                    msg().send(sender, "admin.usage");
                    return;
                }
                plugin.profiles().save(prof);
                msg().send(sender, "admin.coins", ph("name", t.getName()), ph("coins", prof.coins));
            }
            case "wallpreview" -> {
                Player p = player(sender);
                if (p == null) return;
                Material mat = Material.PACKED_ICE;
                if (args.length >= 3) {
                    Material parsed = Material.matchMaterial(args[2]);
                    if (parsed != null && parsed.isBlock() && !parsed.isAir()) mat = parsed;
                }
                msg().send(p, "admin.wallpreview");
                WallPreview.run(plugin, p, mat);
            }
            default -> msg().send(sender, "admin.usage");
        }
    }

    // tab
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("graj", "opusc", "statystyki", "zestaw", "sklep", "pack"));
            if (sender.hasPermission("rivals.test")) out.add("test");
            if (sender.hasPermission("rivals.admin")) out.add("admin");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            out.addAll(plugin.arenas().ids());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("statystyki")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("rivals.admin")) {
            if (args.length == 2) {
                out.addAll(List.of("setlobby", "reload", "forcestart", "stop", "arenas", "wallpreview", "lobby", "coins"));
            } else if ((args.length == 3 || args.length == 4) && args[1].equalsIgnoreCase("forcestart")
                    || args.length == 3 && args[1].equalsIgnoreCase("stop")) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            } else if (args.length == 5 && args[1].equalsIgnoreCase("forcestart")) {
                out.addAll(plugin.arenas().ids());
            } else if (args.length == 3 && args[1].equalsIgnoreCase("lobby")) {
                out.addAll(List.of("leaderboard", "pedestal", "npc", "clear"));
            } else if (args.length == 3 && args[1].equalsIgnoreCase("coins")) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            } else if (args.length == 3 && args[1].equalsIgnoreCase("wallpreview")) {
                out.addAll(List.of("packed_ice", "magenta_stained_glass", "iron_block", "stone_bricks"));
            }
        }
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
