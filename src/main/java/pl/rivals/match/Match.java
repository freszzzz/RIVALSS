package pl.rivals.match;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.data.BlockData;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.GameMode;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import pl.rivals.anim.DestructionAnimation;
import pl.rivals.arena.ArenaDecor;
import pl.rivals.arena.ArenaInstance;
import pl.rivals.arena.ArenaTemplate;
import pl.rivals.arena.BlockVec;
import pl.rivals.coin.ArenaCoin;
import pl.rivals.coin.BonusCapsule;
import pl.rivals.combat.Combat;
import pl.rivals.combat.MatchEffects;
import pl.rivals.config.Messages;
import pl.rivals.config.RivalsConfig;
import pl.rivals.cosmetic.Cosmetics;
import pl.rivals.cosmetic.KillEffects;
import pl.rivals.data.PlayerProfile;
import pl.rivals.gui.KitMenu;
import pl.rivals.hud.Hud;
import pl.rivals.hud.TitleHud;
import pl.rivals.item.ItemFactory;
import pl.rivals.item.Kit;
import pl.rivals.RivalsPlugin;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;
import pl.rivals.weapon.Weapons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static pl.rivals.config.Messages.ph;
import static pl.rivals.config.Messages.phc;

/**
 * Jeden mecz 1v1 - maszyna stanów:
 * PREPARING (wybór zestawu) -> COUNTDOWN -> FIGHT -> KILLCAM -> INTERMISSION -> COUNTDOWN -> ... -> ENDING -> FINISHED
 */
public final class Match {

    public enum State { PREPARING, COUNTDOWN, FIGHT, KILLCAM, INTERMISSION, ENDING, FINISHED }

    private static final AtomicInteger IDS = new AtomicInteger(1);

    private final RivalsPlugin plugin;
    private final MatchManager manager;
    private final int id = IDS.getAndIncrement();
    private final ArenaInstance arena;
    private final Combatant a;
    private final Combatant b;
    private final boolean training;
    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    private final MatchEffects effects = new MatchEffects();
    private ArenaDecor decor;

    private State state = State.PREPARING;
    private int stateTicks;
    private int round;
    private int roundTicks;
    private int ringsStarted;
    private boolean arenaReady;
    private boolean suddenDeath;
    private int coinSpawnAt;
    private Combat.Kind pendingKind;

    private DestructionAnimation wallAnim;
    private final List<DestructionAnimation> ringAnims = new ArrayList<>();
    private final List<DestructionAnimation> allAnims = new ArrayList<>();
    private final Map<UUID, KitMenu> openMenus = new HashMap<>();

    private Combatant roundWinner;
    private Combatant roundLoser;
    private final Map<UUID, int[]> eloResult = new HashMap<>();

    Match(RivalsPlugin plugin, MatchManager manager, ArenaInstance arena, Combatant a, Combatant b, boolean training) {
        this.plugin = plugin;
        this.manager = manager;
        this.arena = arena;
        this.a = a;
        this.b = b;
        this.training = training;
        a.side = 0;
        b.side = 1;
        a.opponent = b;
        b.opponent = a;
        Kit def = Kit.parse(plugin.settings().defaultKit, Kit.FALLBACK);
        for (Combatant c : both()) {
            PlayerProfile prof = c.isBot() ? null : plugin.profiles().get(c.id());
            c.state.kit = Kit.parse(prof == null ? null : prof.kit, def);
            c.state.kitReady = c.isBot();
        }
    }

    // POMOCNICZE
    private RivalsConfig cfg() {
        return plugin.settings();
    }

    private Messages msg() {
        return plugin.messages();
    }

    private TitleHud titles() {
        return plugin.hud().titles();
    }

    private List<Combatant> both() {
        return List.of(a, b);
    }

    public Collection<Player> viewers() {
        List<Player> list = new ArrayList<>(2);
        Player pa = a.player();
        if (pa != null) list.add(pa);
        Player pb = b.player();
        if (pb != null) list.add(pb);
        return list;
    }

    public Combatant combatant(Entity e) {
        if (a.is(e)) return a;
        if (b.is(e)) return b;
        return null;
    }

    public boolean isOpponent(Entity self, Entity other) {
        Combatant c = combatant(self);
        if (c == null) return false;
        return c.opponent.is(other) || effects.decoy(other) != null;
    }

    private void setState(State s) {
        state = s;
        stateTicks = 0;
    }

    private static double maxHealth(LivingEntity e) {
        AttributeInstance ai = e.getAttribute(Attribute.MAX_HEALTH);
        return ai == null ? 20 : ai.getValue();
    }

    private TagResolver scores(Combatant self) {
        return TagResolver.resolver(
                ph("score_self", self.score), ph("score_enemy", self.opponent.score),
                ph("score_a", a.score), ph("score_b", b.score),
                ph("name_a", a.name()), ph("name_b", b.name()),
                ph("round", round));
    }

    private void broadcast(String key, TagResolver... r) {
        for (Player p : viewers()) msg().send(p, key, r);
    }

    // START
    void start() {
        arena.build(() -> {
            arenaReady = true;
            decor = ArenaDecor.spawn(plugin, arena, this::viewers);
        });
        for (Combatant c : both()) {
            Player p = c.player();
            if (p == null) continue;
            TagResolver r = TagResolver.resolver(ph("opponent", c.opponent.name()), ph("self", c.name()),
                    ph("arena", arena.template().displayName()), ph("id", id), ph("rounds", cfg().roundsToWin));
            titles().show(p, TitleHud.MESSAGE, msg().get("match.found-title", r), msg().get("match.found-subtitle", r), 5, 50, 10);
            msg().send(p, "match.found-chat", r);
            if (training) msg().send(p, "match.training-chat", ph("reward_note", cfg().botRewards ? "" : " i bez monet"));
            plugin.sounds().play(p, SoundId.MATCH_FOUND);
            openKitMenu(c, cfg().kitSelectTicks);
        }
    }

    // GUI ZESTAWU
    private void openKitMenu(Combatant c, int ticks) {
        Player p = c.player();
        if (p == null) return;
        c.state.kitReady = false;
        KitMenu menu = new KitMenu(plugin, p, c.state.kit, kit -> {
            c.state.kit = kit;
            PlayerProfile prof = plugin.profiles().get(c.id());
            if (prof != null) {
                prof.kit = kit.serialize();
                plugin.profiles().save(prof);
            }
        }, () -> c.state.kitReady = true);
        menu.secondsLeft((ticks + 19) / 20);
        openMenus.put(p.getUniqueId(), menu);
        menu.open();
    }

    private void tickMenus(int ticksLeft) {
        int secs = Math.max(0, (ticksLeft + 19) / 20);
        for (KitMenu m : openMenus.values()) m.secondsLeft(secs);
    }

    private void closeMenus() {
        for (Map.Entry<UUID, KitMenu> e : openMenus.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && p.getOpenInventory().getTopInventory().getHolder() == e.getValue()) p.closeInventory();
        }
        openMenus.clear();
    }

    // TICK
    void tick() {
        stateTicks++;
        if (decor != null) decor.tick();
        switch (state) {
            case PREPARING -> tickPreparing();
            case COUNTDOWN -> tickCountdown();
            case FIGHT -> tickFight();
            case KILLCAM -> {
                if (stateTicks >= cfg().killcamTicks) afterRoundPause();
            }
            case INTERMISSION -> tickIntermission();
            case ENDING -> {
                if (stateTicks >= cfg().endReturnTicks) finish();
            }
            case FINISHED -> {
            }
        }
    }

    private void tickPreparing() {
        int left = cfg().kitSelectTicks - stateTicks;
        if (stateTicks % 10 == 0) {
            tickMenus(left);
            Component bar = msg().get(arenaReady ? "match.kit-select" : "match.preparing", ph("seconds", Math.max(0, (left + 19) / 20)));
            for (Player p : viewers()) p.sendActionBar(bar);
        }
        boolean allReady = a.state.kitReady && b.state.kitReady;
        if (arenaReady && (left <= 0 || allReady) && stateTicks >= 20) {
            closeMenus();
            resetCombatants();
            beginCountdown();
        }
    }

    // ODLICZANIE
    private void beginCountdown() {
        round++;
        closeMenus();
        setState(State.COUNTDOWN);
        roundTicks = 0;
        ringsStarted = 0;
        suddenDeath = false;
        roundWinner = null;
        roundLoser = null;
        int lo = cfg().coinSpawnMinTicks, hi = cfg().coinSpawnMaxTicks;
        coinSpawnAt = lo + (hi > lo ? ThreadLocalRandom.current().nextInt(hi - lo + 1) : 0);
        for (Combatant c : both()) {
            c.state.resetRound();
            giveKit(c);
        }

        wallAnim = new DestructionAnimation(plugin, arena.world(), arena.wallBlocks(), arena.wallCenter(), this::viewers);
        allAnims.add(wallAnim);
        wallAnim.startCrack(cfg().countdownTicks);
        showCountdown((cfg().countdownTicks + 19) / 20);
        updateBossBar();
    }

    private void tickCountdown() {
        int total = cfg().countdownTicks;
        if (stateTicks >= total) {
            go();
            return;
        }
        if (stateTicks % 20 == 0) showCountdown((total - stateTicks + 19) / 20);
    }

    private void showCountdown(int n) {
        for (Combatant c : both()) {
            Player p = c.player();
            if (p == null) continue;
            TagResolver r = TagResolver.resolver(scores(c), ph("n", n));
            titles().zoom(p, TitleHud.COUNTDOWN, String.valueOf(n), Hud.ACCENT, msg().get("match.countdown-sub", r), 0, 14);
            plugin.sounds().play(p, SoundId.COUNTDOWN_TICK, 1f + (3 - Math.min(3, n)) * 0.12f);
        }
    }

    private void go() {
        setState(State.FIGHT);
        wallAnim.shatter(null);
        for (Combatant c : both()) {
            LivingEntity e = c.entity();
            if (e != null) Status.unfreeze(e, Status.FREEZE);
            if (c.isBot()) {
                c.bot().freeze(false);
                c.bot().target(c.opponent.entity());
            }
            Player p = c.player();
            if (p != null) titles().zoom(p, TitleHud.COUNTDOWN, "WALCZ!", Hud.ACCENT, Component.empty(), 1, 10);
        }
        plugin.sounds().play(viewers(), SoundId.COUNTDOWN_GO, arena.center());
    }

    // WALKA
    private void tickFight() {
        roundTicks++;
        for (Combatant c : both()) {
            LivingEntity e = c.entity();
            if (e != null && e.getLocation().getY() < arena.voidY()) {
                onKill(c, c.opponent, true);
                return;
            }
        }
        effects.tick();
        if (state != State.FIGHT) return;
        if (roundTicks % 2 == 0) updateBossBar();

        if (roundTicks == coinSpawnAt) {
            for (Location l : ArenaCoin.positions(arena, cfg().coinsPerRound)) effects.add(new ArenaCoin(plugin, this, l));
        }
        if (cfg().capsuleEnabled && roundTicks == cfg().capsuleTicks) {
            BlockVec c = arena.abs(new BlockVec(0, 1, 0));
            effects.add(new BonusCapsule(plugin, this, new Location(arena.world(), c.x() + 0.5, c.y(), c.z() + 0.5)));
        }

        int sd = cfg().suddenDeathStartTicks;
        if (sd < cfg().roundMaxTicks && roundTicks >= sd) {
            if (roundTicks == sd) startSuddenDeath();
            if ((roundTicks - sd) % cfg().ringIntervalTicks == 0) collapseNextRing();
        }
        if (roundTicks >= cfg().roundMaxTicks) timeout();
    }

    private void startSuddenDeath() {
        suddenDeath = true;
        for (Player p : viewers()) {
            titles().show(p, TitleHud.MESSAGE, msg().get("match.sudden-death-title"), msg().get("match.sudden-death-sub"), 4, 30, 8);
        }
        plugin.sounds().play(viewers(), SoundId.SUDDEN_DEATH, arena.center());
    }

    private void collapseNextRing() {
        int r = arena.template().half() - ringsStarted;
        if (r < cfg().ringMinRadius) return;
        ringsStarted++;
        Map<BlockVec, BlockData> ring = arena.ringBlocks(r);
        if (ring.isEmpty()) return;
        DestructionAnimation anim = new DestructionAnimation(plugin, arena.world(), ring, arena.floorCenter(), this::viewers);
        ringAnims.add(anim);
        allAnims.add(anim);
        anim.crackThenShatter(cfg().ringCrackTicks);
        plugin.sounds().play(viewers(), SoundId.RING_WARNING, arena.center());
    }

    /** Koniec rundy: zatrzymuje pękanie pierścieni i usuwa wszystkie obiekty gadżetów. */
    private void stopRoundHazards() {
        for (DestructionAnimation an : ringAnims) an.cancelPending();
        effects.clear();
    }

    private void timeout() {
        stopRoundHazards();
        LivingEntity ea = a.entity();
        LivingEntity eb = b.entity();
        double ha = ea == null ? 0 : ea.getHealth() / maxHealth(ea);
        double hb = eb == null ? 0 : eb.getHealth() / maxHealth(eb);
        if (cfg().timeoutByHealth && Math.abs(ha - hb) > 1.0e-3) {
            Combatant w = ha > hb ? a : b;
            roundWinner = w;
            roundLoser = w.opponent;
            w.score++;
            w.coinsEarned += cfg().rewardRoundWin;
            broadcast("match.timeout-win", ph("winner", w.name()));
            Player wp = w.player();
            if (wp != null) plugin.sounds().play(wp, SoundId.ROUND_WIN);
            Player lp = w.opponent.player();
            if (lp != null) plugin.sounds().play(lp, SoundId.ROUND_LOSE);
        } else {
            roundWinner = null;
            roundLoser = null;
            broadcast("match.timeout-draw");
        }
        for (Combatant c : both()) {
            LivingEntity e = c.entity();
            if (e != null) Status.freeze(e, Status.FREEZE);
            if (c.isBot()) c.bot().freeze(true);
            Player p = c.player();
            if (p != null) titles().show(p, TitleHud.MESSAGE, msg().get("match.timeout-title"), Component.empty(), 4, 30, 8);
        }
        setState(State.KILLCAM);
    }

    /** Śmiertelny cios albo upadek w pustkę. */
    void onKill(Combatant victim, Combatant killer, boolean voidDeath) {
        if (state != State.FIGHT) return;
        roundWinner = killer;
        roundLoser = victim;
        killer.score++;
        killer.stats.kills++;
        victim.stats.deaths++;
        killer.coinsEarned += cfg().rewardKill + cfg().rewardRoundWin;
        LivingEntity ve = victim.entity();
        LivingEntity ke = killer.entity();
        Location deathLoc = ve != null && !voidDeath ? ve.getLocation() : arena.center();
        stopRoundHazards();
        setState(State.KILLCAM);

        PlayerProfile kprof = killer.isBot() ? null : plugin.profiles().get(killer.id());
        KillEffects.play(plugin, kprof, deathLoc, viewers());

        Player vp = victim.player();
        if (vp != null) {
            vp.setGameMode(GameMode.SPECTATOR);
            vp.setVelocity(new Vector());
            if (ke != null) {
                vp.teleport(ke.getLocation());
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (state == State.KILLCAM && vp.isOnline() && vp.getGameMode() == GameMode.SPECTATOR && ke.isValid()) {
                        vp.setSpectatorTarget(ke);
                    }
                }, 2L);
            }
        }
        if (victim.isBot()) {
            victim.bot().freeze(true);
            victim.bot().teleport(arena.spawn(victim.side));
        }
        if (killer.isBot()) killer.bot().freeze(true);

        Player kp = killer.player();
        if (kp != null) {
            TagResolver r = scores(killer);
            titles().zoom(kp, TitleHud.MESSAGE, "ZABÓJSTWO", Hud.ACCENT, msg().get("match.kill-sub", r), 3, 40);
            plugin.sounds().play(kp, SoundId.ROUND_WIN);
        }
        if (vp != null) {
            TagResolver r = TagResolver.resolver(scores(victim), ph("killer", killer.name()));
            titles().zoom(vp, TitleHud.MESSAGE, "POKONANY", Hud.RED, msg().get("match.death-sub", r), 3, 40);
            plugin.sounds().play(vp, SoundId.ROUND_LOSE);
        }
        TagResolver r = TagResolver.resolver(scores(killer), ph("killer", killer.name()), ph("victim", victim.name()));
        broadcast(voidDeath ? "match.void-chat" : "match.kill-chat", r);
    }

    private void afterRoundPause() {
        if (roundWinner != null && roundWinner.score >= cfg().roundsToWin) {
            endMatch(roundWinner, false);
            return;
        }
        beginIntermission();
    }

    // PRZERWA
    private void beginIntermission() {
        setState(State.INTERMISSION);
        arena.clearLooseEntities();
        resetCombatants();
        if (wallAnim != null) wallAnim.rebuild(null);
        for (DestructionAnimation an : ringAnims) an.rebuild(null);
        ringAnims.clear();
        updateBossBar();

        Player lp = roundLoser != null ? roundLoser.player() : null;
        if (lp != null) {
            msg().send(lp, "match.loser-kit", ph("seconds", cfg().intermissionTicks / 20));
            openKitMenu(roundLoser, cfg().intermissionTicks);
        }
    }

    private void tickIntermission() {
        int left = cfg().intermissionTicks - stateTicks;
        boolean loserReady = roundLoser == null || roundLoser.isBot() || roundLoser.state.kitReady;
        if (left <= 0 || (loserReady && roundLoser != null && stateTicks >= 40)) {
            beginCountdown();
            return;
        }
        if (stateTicks % 10 == 1) {
            tickMenus(left);
            TagResolver r = TagResolver.resolver(ph("round", round + 1), ph("seconds", (left + 19) / 20));
            for (Player p : viewers()) p.sendActionBar(msg().get("match.intermission", r));
        }
    }

    // RESET / ZESTAW
    private void resetCombatants() {
        for (Combatant c : both()) reset(c);
    }

    private void reset(Combatant c) {
        Location spawn = arena.spawn(c.side);
        ArenaTemplate t = arena.template();
        if (c.isBot()) {
            TrainingBot bot = c.bot();
            if (!bot.isSpawned()) {
                bot.spawn(spawn);
                manager.register(bot.entity().getUniqueId(), this);
            } else {
                bot.teleport(spawn);
            }
            bot.freeze(true);
            bot.heal();
            Status.setGravity(bot.entity(), t.gravity());
            return;
        }
        Player p = c.player();
        if (p == null) return;
        if (p.getGameMode() == GameMode.SPECTATOR) p.setSpectatorTarget(null);
        p.setGameMode(GameMode.ADVENTURE);
        p.teleport(spawn);
        p.setVelocity(new Vector());
        p.setFallDistance(0);
        p.setFireTicks(0);
        p.setFreezeTicks(0);
        for (PotionEffect pe : p.getActivePotionEffects()) p.removePotionEffect(pe.getType());
        AttributeInstance mh = p.getAttribute(Attribute.MAX_HEALTH);
        if (mh != null) mh.setBaseValue(cfg().maxHealth);
        p.setHealth(maxHealth(p));
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.getInventory().clear();
        Status.clearAll(p);
        Status.freeze(p, Status.FREEZE);
        Status.setGravity(p, t.gravity());
        p.setPlayerTime(t.time(), false);
        p.showBossBar(bossBar);
    }

    /** Wydaje zestaw (ze skórkami gracza): sloty 1-4 + strzała w ukrytym slocie dla łuku/kuszy. */
    private void giveKit(Combatant c) {
        Player p = c.player();
        if (p == null) return;
        PlayerProfile prof = plugin.profiles().get(c.id());
        PlayerInventory inv = p.getInventory();
        inv.clear();
        RivalsItem[] items = c.state.kit.items();
        for (int i = 0; i < items.length; i++) {
            inv.setItem(i, ItemFactory.create(plugin, items[i], Cosmetics.skinFor(prof, items[i])));
        }
        Weapons.ensureArrow(p);
        inv.setHeldItemSlot(0);
        p.setCooldown(Material.BOW, 0);
        p.setCooldown(Material.CROSSBOW, 0);
        p.setCooldown(Material.TRIDENT, 0);
    }

    // BOSSBAR
    public int roundTimeLeftTicks() {
        return state == State.FIGHT || state == State.COUNTDOWN ? Math.max(0, cfg().roundMaxTicks - roundTicks) : cfg().roundMaxTicks;
    }

    private void updateBossBar() {
        TagResolver base = TagResolver.resolver(ph("name_a", a.name()), ph("name_b", b.name()),
                ph("score_a", a.score), ph("score_b", b.score), ph("round", Math.max(1, round)));
        if (state == State.FIGHT || state == State.COUNTDOWN) {
            int max = cfg().roundMaxTicks;
            int left = roundTimeLeftTicks();
            float progress = Math.max(0f, Math.min(1f, left / (float) max));
            int secs = (left + 19) / 20;
            String time = String.format("%d:%02d", secs / 60, secs % 60);
            TagResolver r = TagResolver.resolver(base, ph("time", time));
            BossBar.Color color = suddenDeath ? BossBar.Color.RED
                    : progress > 0.5f ? BossBar.Color.BLUE
                    : progress > 0.34f ? BossBar.Color.YELLOW : BossBar.Color.RED;
            bossBar.name(msg().get(suddenDeath ? "match.bossbar-sd" : "match.bossbar", r));
            bossBar.progress(progress);
            bossBar.color(color);
        } else {
            bossBar.name(msg().get("match.bossbar-pause", base));
            bossBar.progress(1f);
            bossBar.color(BossBar.Color.WHITE);
        }
    }

    // OBRAŻENIA / ZDARZENIA
    /** Atakujący: byt albo strzelec pocisku. */
    public static Entity resolveAttacker(Entity damager) {
        if (damager instanceof Projectile p && p.getShooter() instanceof Entity shooter) return shooter;
        return damager;
    }

    /** Rodzaj obrażeń zadawanych właśnie przez {@link Combat#damage}. */
    public void pendingKind(Combat.Kind kind) {
        this.pendingKind = kind;
    }

    void handleDamage(EntityDamageEvent e, LivingEntity victimEntity) {
        Combatant victim = combatant(victimEntity);
        if (victim == null) return;
        if (state != State.FIGHT) {
            e.setCancelled(true);
            return;
        }
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && !cfg().fallDamage) {
            e.setCancelled(true);
            return;
        }
        Combatant attacker = null;
        if (e instanceof EntityDamageByEntityEvent ed) {
            Entity source = resolveAttacker(ed.getDamager());
            Combatant c = combatant(source);
            if (c == null) {
                e.setCancelled(true);
                return;
            }
            if (c != victim) {
                attacker = c;
                if (ed.getDamager() instanceof Projectile) attacker.stats.hits++;
            }
        }
        double dmg = e.getFinalDamage();
        double health = victimEntity.getHealth();
        if (attacker != null) {
            attacker.stats.damageDealt += Math.min(dmg, health);
            Player ap = attacker.player();
            if (ap != null) plugin.hud().hit(ap, victimEntity, Math.min(dmg, health), pendingKind == Combat.Kind.RANGED_HEAD);
        }
        effects.notifyDamage(victim, dmg);

        if (dmg >= health) {
            e.setCancelled(true);
            onKill(victim, attacker != null ? attacker : victim.opponent, false);
        }
    }

    void handleLaunch(LivingEntity shooter, Projectile projectile) {
        Combatant c = combatant(shooter);
        if (c == null) return;
        c.stats.shots++;
        if (projectile instanceof AbstractArrow arrow) arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
    }

    void handleQuit(Player p) {
        Combatant c = combatant(p);
        if (c == null) return;
        p.hideBossBar(bossBar);
        openMenus.remove(p.getUniqueId());
        c.markLeft();
        if (state == State.ENDING || state == State.FINISHED) return;
        Player op = c.opponent.player();
        if (op != null) msg().send(op, "match.opponent-left", ph("name", c.name()));
        endMatch(c.opponent, true);
    }

    public void forfeit(Player p) {
        Combatant c = combatant(p);
        if (c == null || state == State.ENDING || state == State.FINISHED) return;
        msg().send(p, "match.forfeit");
        c.coinsEarned = 0;
        Player op = c.opponent.player();
        if (op != null) msg().send(op, "match.opponent-left", ph("name", c.name()));
        endMatch(c.opponent, true);
    }

    // KONIEC
    private void endMatch(Combatant winner, boolean forfeit) {
        if (state == State.ENDING || state == State.FINISHED) return;
        closeMenus();
        stopRoundHazards();
        setState(State.ENDING);
        Combatant loser = winner.opponent;

        for (Combatant c : both()) {
            if (c.isBot()) c.bot().freeze(true);
            Player p = c.player();
            if (p == null) continue;
            p.hideBossBar(bossBar);
            if (p.getGameMode() == GameMode.SPECTATOR) {
                p.setSpectatorTarget(null);
                p.setGameMode(GameMode.ADVENTURE);
                p.teleport(arena.spawn(c.side));
            }
            Status.unfreeze(p, Status.FREEZE);
            p.getInventory().clear();
        }

        applyRewards(winner, loser, forfeit);

        Player wp = winner.player();
        if (wp != null) {
            TagResolver r = scores(winner);
            titles().zoom(wp, TitleHud.MESSAGE, "ZWYCIĘSTWO", Hud.ACCENT, msg().get("match.victory-sub", r), 3, 90);
            plugin.sounds().play(wp, SoundId.MATCH_WIN);
            sendSummary(winner, true);
        }
        Player lp = loser.player();
        if (lp != null) {
            TagResolver r = scores(loser);
            titles().zoom(lp, TitleHud.MESSAGE, "PORAŻKA", Hud.RED, msg().get("match.defeat-sub", r), 3, 90);
            plugin.sounds().play(lp, SoundId.MATCH_LOSE);
            sendSummary(loser, false);
        }
    }

    private void applyRewards(Combatant winner, Combatant loser, boolean forfeit) {
        boolean give = !training || cfg().botRewards;
        PlayerProfile pw = winner.isBot() ? null : plugin.profiles().get(winner.id());
        PlayerProfile pl = loser.isBot() ? null : plugin.profiles().get(loser.id());
        if (!give) {
            winner.coinsEarned = 0;
            loser.coinsEarned = 0;
            return;
        }
        winner.coinsEarned += cfg().rewardMatchWin;
        if (forfeit) loser.coinsEarned = 0;

        if (pw != null) {
            pw.wins++;
            pw.streak++;
            pw.bestStreak = Math.max(pw.bestStreak, pw.streak);
            if (pw.streak >= 2) {
                winner.streakBonus = Math.min(cfg().streakBonusMax, cfg().streakBonusPerWin * (pw.streak - 1));
                winner.coinsEarned += winner.streakBonus;
            }
        }
        if (pl != null) {
            pl.losses++;
            pl.streak = 0;
        }
        if (pw != null && pl != null && !training) {
            int d = Elo.delta(pw.elo, pl.elo, cfg().eloK);
            eloResult.put(winner.id(), new int[]{pw.elo, pw.elo + d});
            int newLoser = Math.max(cfg().eloMin, pl.elo - d);
            eloResult.put(loser.id(), new int[]{pl.elo, newLoser});
            pw.elo += d;
            pl.elo = newLoser;
        }
        for (Combatant c : both()) {
            PlayerProfile p = c == winner ? pw : pl;
            if (p == null) continue;
            p.matches++;
            p.coins += c.coinsEarned;
            p.kills += c.stats.kills;
            p.deaths += c.stats.deaths;
            p.roundsWon += c.score;
            plugin.profiles().save(p);
        }
    }

    private void sendSummary(Combatant c, boolean won) {
        Player p = c.player();
        if (p == null) return;
        PlayerProfile prof = plugin.profiles().get(c.id());
        int[] elo = eloResult.get(c.id());
        int eloOld = elo != null ? elo[0] : prof != null ? prof.elo : 0;
        int eloNew = elo != null ? elo[1] : eloOld;
        int delta = eloNew - eloOld;
        Component deltaC = Messages.mm().deserialize(delta >= 0 ? "<#38E8F0>(+" + delta + ")" : "<#FF4D5E>(" + delta + ")");
        TagResolver r = TagResolver.resolver(
                scores(c),
                phc("result", msg().get(won ? "match.result-win" : "match.result-loss")),
                ph("opponent", c.opponent.name()),
                ph("damage", String.format("%.1f", c.stats.damageDealt)),
                ph("accuracy", c.stats.accuracyPercent()),
                ph("hits", c.stats.hits), ph("shots", c.stats.shots),
                ph("kills", c.stats.kills), ph("gadgets", c.stats.gadgetsTotal()),
                ph("coins", c.coinsEarned), ph("streak_bonus", c.streakBonus),
                ph("elo_old", eloOld), ph("elo_new", eloNew), phc("elo_delta", deltaC));
        msg().sendLines(p, "match.summary", r);
    }

    void finish() {
        if (state == State.FINISHED) return;
        setState(State.FINISHED);
        closeMenus();
        effects.clear();
        if (decor != null) decor.remove();
        for (DestructionAnimation an : allAnims) an.cancel();
        allAnims.clear();
        ringAnims.clear();
        for (Combatant c : both()) {
            Player p = c.player();
            if (p != null) {
                p.hideBossBar(bossBar);
                titles().clear(p);
                plugin.lobby().sendToLobby(p);
            }
            if (c.isBot()) c.bot().remove();
        }
        arena.clearLooseEntities();
        manager.remove(this);
    }

    public void abort() {
        if (state == State.FINISHED) return;
        for (Player p : viewers()) msg().send(p, "match.aborted");
        finish();
    }

    // GETTERY
    public RivalsPlugin plugin() { return plugin; }
    public int id() { return id; }
    public State state() { return state; }
    public boolean fighting() { return state == State.FIGHT; }
    public ArenaInstance arena() { return arena; }
    public Combatant a() { return a; }
    public Combatant b() { return b; }
    public int round() { return round; }
    public MatchEffects effects() { return effects; }
}
