package pl.rivals.shared;

/**
 * Dźwięki trybu. Każdy ma własny wpis rivals:&lt;id&gt; w sounds.json. Na start wpis odsyła do
 * dźwięku vanilla ze zmienioną wysokością (type: event) - podmień go na własny plik .ogg
 * w assets/rivals/sounds/ i zmień "name" w sounds.json.
 */
public enum SoundId {

    // mecz / UI
    MATCH_FOUND("match.found", "block.note_block.chime", 1.4f, 1f),
    QUEUE_JOIN("queue.join", "ui.button.click", 1.6f, 0.6f),
    COUNTDOWN_TICK("countdown.tick", "block.note_block.pling", 1.0f, 1f),
    COUNTDOWN_GO("countdown.go", "block.beacon.activate", 1.6f, 1f),
    WALL_CRACK("wall.crack", "block.stone.hit", 0.7f, 0.8f),
    WALL_BREAK("wall.break", "block.glass.break", 0.8f, 0.7f),
    WALL_EXPLODE("wall.explode", "entity.generic.explode", 1.2f, 0.9f),
    WALL_REBUILD("wall.rebuild", "block.stone.place", 1.3f, 0.6f),
    RING_WARNING("ring.warning", "block.beacon.deactivate", 1.5f, 1f),
    SUDDEN_DEATH("sudden_death", "entity.elder_guardian.curse", 1.4f, 0.5f),
    KILL("kill", "block.amethyst_block.break", 0.6f, 1f),
    ROUND_WIN("round.win", "entity.player.levelup", 1.4f, 0.8f),
    ROUND_LOSE("round.lose", "block.note_block.bass", 0.6f, 1f),
    MATCH_WIN("match.win", "ui.toast.challenge_complete", 1.0f, 0.8f),
    MATCH_LOSE("match.lose", "block.beacon.deactivate", 0.6f, 1f),
    HUD_HIT("hud.hit", "entity.arrow.hit_player", 1.6f, 0.5f),
    HUD_HEADSHOT("hud.headshot", "block.note_block.bell", 1.9f, 0.7f),
    UI_CLICK("ui.click", "ui.button.click", 1.3f, 0.5f),
    UI_BUY("ui.buy", "entity.experience_orb.pickup", 0.8f, 0.8f),
    UI_ERROR("ui.error", "block.note_block.bass", 0.5f, 0.8f),

    // monety / bonus
    COIN_SPAWN("coin.spawn", "block.amethyst_block.chime", 1.6f, 1f),
    COIN_PICKUP("coin.pickup", "entity.experience_orb.pickup", 1.4f, 1f),
    CAPSULE_FALL("capsule.fall", "entity.firework_rocket.launch", 0.6f, 1f),
    CAPSULE_LAND("capsule.land", "entity.iron_golem.damage", 0.6f, 1f),
    CAPSULE_OPEN("capsule.open", "block.iron_trapdoor.open", 0.8f, 1f),
    BONUS_PICKUP("bonus.pickup", "entity.player.levelup", 1.8f, 0.7f),

    // bronie
    BOW_SHOOT("weapon.bow.shoot", "entity.illusioner.cast_spell", 1.6f, 0.8f),
    CROSSBOW_SHOOT("weapon.crossbow.shoot", "item.crossbow.shoot", 0.8f, 1f),
    RIFLE_SHOOT("weapon.rifle.shoot", "entity.firework_rocket.blast", 1.6f, 0.9f),
    SHOTGUN_SHOOT("weapon.shotgun.shoot", "entity.generic.explode", 1.8f, 0.6f),
    PISTOL_SHOOT("weapon.pistol.shoot", "entity.firework_rocket.blast", 2.0f, 0.7f),
    SLINGSHOT_SHOOT("weapon.slingshot.shoot", "entity.snowball.throw", 0.7f, 1f),
    RELOAD("weapon.reload", "block.iron_trapdoor.close", 1.7f, 0.6f),
    IMPACT("weapon.impact", "block.stone.hit", 1.2f, 0.6f),
    HAMMER_CHARGE("weapon.hammer.charge", "block.respawn_anchor.charge", 1.4f, 0.6f),
    HAMMER_SLAM("weapon.hammer.slam", "entity.zombie.break_wooden_door", 0.6f, 1f),

    // gadżety (osobny wpis dla każdego gadżetu)
    CRYO_THROW("gadget.cryo.throw", "entity.snowball.throw", 0.6f, 1f),
    CRYO_EXPLODE("gadget.cryo.explode", "block.glass.break", 0.5f, 1f),
    CRYO_SHATTER("gadget.cryo.shatter", "block.glass.break", 1.4f, 1f),
    CAGE_PLACE("gadget.cage.place", "block.iron_trapdoor.close", 1.2f, 0.8f),
    CAGE_TRIGGER("gadget.cage.trigger", "block.iron_door.close", 0.7f, 1f),
    NET_SHOOT("gadget.net.shoot", "entity.fishing_bobber.throw", 0.6f, 1f),
    NET_HIT("gadget.net.hit", "block.wool.place", 0.6f, 1f),
    FLASH_THROW("gadget.flash.throw", "entity.snowball.throw", 1.2f, 1f),
    FLASH_BANG("gadget.flash.bang", "entity.firework_rocket.large_blast", 1.4f, 1f),
    ANCHOR_THROW("gadget.anchor.throw", "item.trident.throw", 0.6f, 1f),
    ANCHOR_PULL("gadget.anchor.pull", "item.armor.equip_chain", 0.6f, 1f),
    GRAPPLE_FIRE("gadget.grapple.fire", "entity.fishing_bobber.throw", 1.4f, 1f),
    GRAPPLE_PULL("gadget.grapple.pull", "entity.fishing_bobber.retrieve", 0.8f, 1f),
    TRAMPOLINE_DEPLOY("gadget.trampoline.deploy", "block.piston.extend", 1.4f, 0.8f),
    TRAMPOLINE_BOUNCE("gadget.trampoline.bounce", "block.slime_block.fall", 1.2f, 1f),
    RECALL_MARK("gadget.recall.mark", "block.amethyst_block.resonate", 1.4f, 1f),
    RECALL_WARP("gadget.recall.warp", "entity.enderman.teleport", 1.4f, 1f),
    DASH_USE("gadget.dash.use", "entity.breeze.wind_burst", 1.3f, 0.8f),
    BOOMERANG_THROW("gadget.boomerang.throw", "item.trident.riptide_1", 1.6f, 0.8f),
    BOOMERANG_HIT("gadget.boomerang.hit", "item.trident.hit", 1.4f, 1f),
    STICKY_STICK("gadget.sticky.stick", "block.slime_block.place", 1.2f, 1f),
    STICKY_BEEP("gadget.sticky.beep", "block.note_block.hat", 1.8f, 0.6f),
    STICKY_EXPLODE("gadget.sticky.explode", "entity.generic.explode", 1.1f, 1f),
    ROCKET_FIRE("gadget.rocket.fire", "entity.firework_rocket.launch", 0.7f, 1f),
    ROCKET_EXPLODE("gadget.rocket.explode", "entity.generic.explode", 0.9f, 1f),
    SHIELD_DEPLOY("gadget.shield.deploy", "block.beacon.power_select", 1.6f, 0.8f),
    SHIELD_BLOCK("gadget.shield.block", "block.amethyst_block.hit", 1.6f, 0.8f),
    SHIELD_BREAK("gadget.shield.break", "block.glass.break", 0.7f, 1f),
    DECOY_USE("gadget.decoy.use", "entity.illusioner.mirror_move", 1.2f, 1f),
    DECOY_POP("gadget.decoy.pop", "entity.illusioner.prepare_blindness", 1.6f, 0.6f),
    MIRROR_SWAP("gadget.mirror.swap", "block.glass.break", 1.0f, 1f),
    MEDKIT_OPEN("gadget.medkit.open", "block.barrel.open", 1.4f, 1f),
    MEDKIT_HEAL("gadget.medkit.heal", "entity.player.levelup", 2.0f, 0.6f);

    public final String id;
    public final String vanillaEvent;
    public final float pitch;
    public final float volume;

    SoundId(String id, String vanillaEvent, float pitch, float volume) {
        this.id = id;
        this.vanillaEvent = vanillaEvent;
        this.pitch = pitch;
        this.volume = volume;
    }

    public String key() {
        return "rivals:" + id;
    }
}
