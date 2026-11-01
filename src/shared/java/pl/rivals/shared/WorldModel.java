package pl.rivals.shared;

/**
 * Modele używane tylko przez display entities w świecie oraz ikony GUI/lobby.
 * Klucz item_model: rivals:world/&lt;nazwa&gt; albo rivals:ui/&lt;nazwa&gt;.
 */
public enum WorldModel {

    // gadżety - części w świecie
    CAGE_MINE_WORLD("world/cage_mine_world"),
    CAGE_BAR("world/cage_bar"),
    NET_BALL("world/net_ball"),
    NET_OPEN("world/net_open"),
    NET_WRAPPED("world/net_wrapped"),
    FLASHBANG_ARMED("world/flashbang_armed"),
    CHAIN_LINK("world/chain_link"),
    ROPE("world/rope"),
    GRAPPLE_HOOK("world/grapple_hook"),
    TRAMPOLINE_FOLDED("world/trampoline_folded"),
    TRAMPOLINE_DEPLOYED("world/trampoline_deployed"),
    HOLOGRAM_BODY("world/hologram_body"),
    STICKY_LIT("world/sticky_grenade_lit"),
    ROCKET("world/rocket"),
    SHIELD_WALL("world/shield_wall"),
    SHIELD_WALL_CRACKED("world/shield_wall_cracked"),
    MIRROR_PANE("world/mirror_pane"),
    MEDKIT_BASE("world/medkit_base"),
    MEDKIT_LID("world/medkit_lid"),
    SHOCKWAVE_RING("world/shockwave_ring"),
    ENERGY_BOLT("world/energy_bolt"),
    CROSSBOW_BOLT("world/crossbow_bolt"),
    PEBBLE("world/pebble"),

    // monety / bonus
    COIN("world/coin"),
    CAPSULE_BOTTOM("world/capsule_bottom"),
    CAPSULE_LID("world/capsule_lid"),
    HEAL_ORB("world/heal_orb"),
    GADGET_ORB("world/gadget_orb"),

    // dekoracje aren / lobby
    FAN_HOUSING("world/fan_housing"),
    FAN_BLADES("world/fan_blades"),
    LAMP_ON("world/lamp_on"),
    LAMP_OFF("world/lamp_off"),
    LOGO("world/logo"),
    PIPE("world/pipe"),
    SCREEN("world/screen"),
    PEDESTAL_TOP("world/pedestal_top"),

    // ikony GUI i przedmioty lobby
    UI_PLAY("ui/play"),
    UI_KIT("ui/kit"),
    UI_SHOP("ui/shop"),
    UI_PROFILE("ui/profile"),
    UI_READY("ui/ready"),
    UI_LOCKED("ui/locked"),
    UI_TAB_SKINS("ui/tab_skins"),
    UI_TAB_EFFECTS("ui/tab_effects"),
    UI_TAB_TRAILS("ui/tab_trails");

    public final String path;

    WorldModel(String path) {
        this.path = path;
    }

    public String key() {
        return "rivals:" + path;
    }
}
