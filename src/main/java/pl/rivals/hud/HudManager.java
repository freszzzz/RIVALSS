package pl.rivals.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import pl.rivals.combat.CombatState;
import pl.rivals.data.PlayerProfile;
import pl.rivals.data.Ranks;
import pl.rivals.item.ItemFactory;
import pl.rivals.match.Combatant;
import pl.rivals.match.Match;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.FontWidths;
import pl.rivals.shared.Glyphs;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Scoreboard (lobby/mecz), actionbar z zestawem, tytuły, hitmarker i liczby obrażeń. */
public final class HudManager implements Listener {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String TITLE = "RIVALS";

    private final RivalsPlugin plugin;
    private final Map<UUID, SidebarHud> boards = new HashMap<>();
    private final TitleHud titles = new TitleHud();
    private final DamageNumbers numbers;
    private BukkitTask task;
    private long tick;

    public HudManager(RivalsPlugin plugin) {
        this.plugin = plugin;
        this.numbers = new DamageNumbers(plugin);
    }

    public void start() {
        for (Player p : Bukkit.getOnlinePlayers()) boards.put(p.getUniqueId(), new SidebarHud(p));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        numbers.clear();
        for (Player p : Bukkit.getOnlinePlayers()) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        boards.clear();
    }

    public TitleHud titles() {
        return titles;
    }

    public SidebarHud board(Player p) {
        return boards.get(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent e) {
        boards.put(e.getPlayer().getUniqueId(), new SidebarHud(e.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        boards.remove(e.getPlayer().getUniqueId());
        titles.clear(e.getPlayer());
    }

    /** Trafienie: hitmarker + dźwięk + liczba obrażeń nad przeciwnikiem. */
    public void hit(Player attacker, LivingEntity victim, double dmg, boolean head) {
        if (plugin.settings().hitmarker) {
            titles.hitmarker(attacker, head);
            plugin.sounds().play(attacker, head ? SoundId.HUD_HEADSHOT : SoundId.HUD_HIT);
        }
        if (plugin.settings().damageNumbers && dmg > 0.05) numbers.spawn(attacker, victim, dmg, head);
    }

    // tick
    private void tick() {
        tick++;
        titles.tick();
        numbers.tick();
        if (tick % 2 != 0) return;
        Component title = shine();
        for (Player p : Bukkit.getOnlinePlayers()) {
            SidebarHud board = boards.get(p.getUniqueId());
            if (board == null) continue;
            if (p.getScoreboard() != board.board()) p.setScoreboard(board.board());
            board.title(title);
            Match m = plugin.matches().ofPlayer(p.getUniqueId());
            Combatant c = m == null ? null : m.combatant(p);
            if (c != null) {
                board.lines(matchLines(board, m, c));
                if (m.state() == Match.State.FIGHT || m.state() == Match.State.COUNTDOWN) p.sendActionBar(actionBar(p, c));
            } else {
                board.lines(lobbyLines(board, p));
            }
        }
    }

    /** Tytuł z przesuwającym się połyskiem - 8 klatek. */
    private Component shine() {
        int frame = (int) ((tick / 6) % 8);
        int hi = frame - 1;
        TextComponent.Builder b = Component.text();
        for (int i = 0; i < TITLE.length(); i++) {
            TextColor c = i == hi ? TextColor.color(0xE6FFFF) : Math.abs(i - hi) == 1 ? TextColor.color(0x8FF4F8) : Hud.ACCENT;
            b.append(Hud.pixel(String.valueOf(TITLE.charAt(i)), c));
        }
        return b.build();
    }

    private Component clockLine(String left, String right) {
        return Component.text().append(Hud.glyph(Glyphs.CLOCK)).append(Hud.space(2))
                .append(Hud.text(left, Hud.TEXT)).append(Hud.text(" | ", Hud.MUTED)).append(Hud.text(right, Hud.TEXT)).build();
    }

    private static final int INNER = Glyphs.PANEL_WIDTH - 4;

    /** Wiersz panelu: ikona + etykieta z lewej, wartość wyrównana do prawej krawędzi panelu. */
    private Component row(char panel, char icon, String label, SidebarHud board, String key, String value, TextColor color) {
        Hud.Row r = Hud.row().icon(icon).gap(2).text(label, Hud.TEXT);
        int vw = FontWidths.vanilla(value);
        r.gap(Math.max(4, INNER - 4 - r.width() - vw));
        r.add(key == null ? Hud.text(value, color) : board.value(key, value, color, tick), vw);
        return Hud.panelRow(panel, r);
    }

    private Component header(String name) {
        return Hud.panelRow(Glyphs.PANEL_TOP, Hud.headerRow(name));
    }

    private static final Component BLANK = Component.text(" ");

    private Component address() {
        return Hud.pixel(plugin.settings().serverAddress, Hud.ACCENT);
    }

    private char coinIcon(boolean animating) {
        return animating ? Glyphs.COIN_BIG : Glyphs.COIN_FRAMES[(int) ((tick / 4) % 4)];
    }

    // lobby
    private List<Component> lobbyLines(SidebarHud board, Player p) {
        List<Component> l = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(plugin.settings().zone);
        l.add(clockLine(DATE.format(now), TIME.format(now)));
        l.add(BLANK);
        PlayerProfile prof = plugin.profiles().get(p.getUniqueId());
        l.add(header("PROFIL"));
        if (prof == null) {
            l.add(Hud.panelRow(Glyphs.PANEL_BOT, Hud.row().text("Wczytywanie…", Hud.MUTED)));
        } else {
            Ranks.Rank rank = plugin.ranks().of(prof.elo);
            TextColor rc = TextColor.fromHexString(rank.color());
            l.add(row(Glyphs.PANEL_MID, Glyphs.RANK, "Ranga", board, null, rank.name(), rc == null ? Hud.TEXT : rc));
            l.add(row(Glyphs.PANEL_MID, Glyphs.TROPHY, "ELO", board, "elo", String.valueOf(prof.elo), Hud.TEXT));
            int[] coins = board.tween("coins", prof.coins);
            l.add(row(Glyphs.PANEL_MID, coinIcon(coins[1] == 1), "Monety", board, "coins", String.valueOf(coins[0]), Hud.GOLD));
            l.add(row(Glyphs.PANEL_BOT, Glyphs.FLAME, "Seria", board, "streak", String.valueOf(prof.streak), Hud.TEXT));
        }
        l.add(BLANK);
        l.add(header("KOLEJKA"));
        if (plugin.queue().isQueued(p.getUniqueId())) {
            long sec = plugin.queue().waitSeconds(p.getUniqueId());
            l.add(row(Glyphs.PANEL_MID, Glyphs.QUEUE, "Szukanie…", board, null, String.format("%d:%02d", sec / 60, sec % 60), Hud.ACCENT));
            l.add(Hud.panelRow(Glyphs.PANEL_BOT, Hud.row().text("/rivals opusc", Hud.MUTED).text(" – wyjdź", Hud.MUTED)));
        } else {
            l.add(Hud.panelRow(Glyphs.PANEL_MID, Hud.row().text("Wpisz ", Hud.TEXT).text("/rivals graj", Hud.ACCENT)));
            l.add(Hud.panelRow(Glyphs.PANEL_BOT, Hud.row().text("lub podejdź do NPC", Hud.MUTED)));
        }
        l.add(BLANK);
        l.add(address());
        return l;
    }

    // mecz
    private List<Component> matchLines(SidebarHud board, Match m, Combatant c) {
        List<Component> l = new ArrayList<>();
        int left = m.roundTimeLeftTicks();
        int secs = (left + 19) / 20;
        l.add(clockLine("Runda " + Math.max(1, m.round()), String.format("%d:%02d", secs / 60, secs % 60)));
        l.add(BLANK);

        Combatant o = c.opponent();
        l.add(header("WYNIK"));
        String oname = o.name().length() > 12 ? o.name().substring(0, 12) : o.name();
        String ss = String.valueOf(c.score), es = String.valueOf(o.score);
        Hud.Row score = Hud.row().icon(Glyphs.YOU).gap(2).text("Ty ", Hud.TEXT)
                .add(board.value("score_self", ss, Hud.ACCENT, tick), FontWidths.vanilla(ss))
                .text(" : ", Hud.MUTED)
                .add(board.value("score_enemy", es, Hud.RED, tick), FontWidths.vanilla(es));
        int nameW = FontWidths.vanilla(oname) + 10;
        score.gap(Math.max(4, INNER - 4 - score.width() - nameW)).icon(Glyphs.ENEMY).gap(2).text(oname, Hud.RED);
        l.add(Hud.panelRow(Glyphs.PANEL_MID, score));
        LivingEntity oe = o.entity();
        String hp = oe == null ? "-" : String.valueOf((int) Math.ceil(oe.getHealth()));
        l.add(row(Glyphs.PANEL_BOT, Glyphs.HEART, "HP przeciwnika", board, "ohp", hp, Hud.RED));
        l.add(BLANK);

        l.add(header("ZESTAW"));
        RivalsItem g = c.state.kit.gadget();
        String gname = g.displayName.length() > 14 ? g.displayName.substring(0, 13) + "." : g.displayName;
        Hud.Row gadget = Hud.row().icon(Glyphs.itemIcon(g)).gap(2).text(gname, Hud.TEXT);
        int barW = c.state.ready(g) ? FontWidths.pixel("GOTOWY") : 30;
        gadget.gap(Math.max(4, INNER - 4 - gadget.width() - barW)).add(cooldownBar(c.state, g), barW);
        l.add(Hud.panelRow(Glyphs.PANEL_BOT, gadget));
        l.add(BLANK);

        l.add(header("ARENA"));
        l.add(row(Glyphs.PANEL_MID, Glyphs.MAP, m.arena().template().displayName(), board, null, "", Hud.TEXT));
        int[] earned = board.tween("earned", c.coinsEarned);
        l.add(row(Glyphs.PANEL_BOT, coinIcon(earned[1] == 1), "Zdobyte", board, "earned", "+" + earned[0], Hud.GOLD));
        l.add(BLANK);
        l.add(address());
        return l;
    }

    /** Pasek 5 segmentów albo pulsujący napis GOTOWY. */
    private Component cooldownBar(CombatState st, RivalsItem g) {
        TextComponent.Builder b = Component.text();
        if (st.ready(g)) {
            boolean pulse = (tick / 10) % 2 == 0;
            b.append(Hud.pixel("GOTOWY", pulse ? Hud.ACCENT : TextColor.color(0xE6FFFF)));
        } else {
            int filled = (int) Math.floor((1 - st.fraction(g)) * 5);
            for (int i = 0; i < 5; i++) {
                b.append(Hud.glyph(i < filled ? Glyphs.SEG_FULL : Glyphs.SEG_EMPTY));
            }
        }
        if (st.bonusCharges > 0) b.append(Hud.text(" +" + st.bonusCharges, Hud.GOLD));
        return b.build();
    }

    /** Actionbar: ikony 4 slotów zestawu z cooldownami / amunicją. */
    private Component actionBar(Player p, Combatant c) {
        TextComponent.Builder b = Component.text();
        RivalsItem held = ItemFactory.read(p.getInventory().getItemInMainHand());
        RivalsItem[] items = c.state.kit.items();
        for (int i = 0; i < items.length; i++) {
            RivalsItem it = items[i];
            if (i > 0) b.append(Hud.space(10));
            b.append(Hud.icon(it)).append(Hud.space(2));
            TextColor base = it == held ? Hud.TEXT : Hud.MUTED;
            CombatState st = c.state;
            int mag = plugin.settings().statInt(it, "magazine");
            if (it.slot != RivalsItem.Slot.GADGET && it.slot != RivalsItem.Slot.MELEE && mag > 0) {
                if (st.reloading(it) && !st.ready(it)) {
                    b.append(Hud.text(String.format(Locale.ROOT, "%.1fs", st.remaining(it) / 20.0), Hud.RED));
                } else {
                    int ammo = st.reloading(it) ? mag : st.ammo(it, mag);
                    b.append(Hud.text(ammo + "/" + mag, base));
                }
            } else if (!st.ready(it)) {
                b.append(Hud.text(String.format(Locale.ROOT, "%.1fs", st.remaining(it) / 20.0), Hud.MUTED));
            } else {
                b.append(Hud.glyph(Glyphs.DOT).color(it == held ? Hud.ACCENT : Hud.MUTED));
            }
        }
        return b.build();
    }
}
