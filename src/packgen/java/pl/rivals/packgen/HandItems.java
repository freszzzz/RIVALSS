package pl.rivals.packgen;

import pl.rivals.shared.RivalsItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static pl.rivals.packgen.Pal.*;

/**
 * Modele przedmiotów w ręce (bronie i gadżety) wraz z wariantami animacji.
 * Styl: jasny korpus (BODY), drugi korpus (BODY2), wykończenia (TRIM), ciemne detale (CORE)
 * i jeden świecący kolor akcentu.
 */
public final class HandItems {

    private HandItems() {
    }

    public enum Def { BASIC, BOW, CROSSBOW, HAMMER, NET }

    public static final class ItemSet {
        public final String id;
        public final Def def;
        public final Model base;
        public final Map<String, Model> variants = new LinkedHashMap<>();

        ItemSet(String id, Def def, Model base) {
            this.id = id;
            this.def = def;
            this.base = base;
        }

        ItemSet variant(String key, Model m) {
            variants.put(key, m);
            return this;
        }
    }

    public static List<ItemSet> all() {
        List<ItemSet> list = new ArrayList<>();
        list.add(energyBow());
        list.add(crossbow());
        list.add(rifle());
        list.add(shotgun());
        list.add(pistol());
        list.add(slingshot());
        list.add(sword());
        list.add(katana());
        list.add(hammer());
        list.add(basic(RivalsItem.CRYO, cryo()));
        list.add(basic(RivalsItem.CAGE, cage()));
        list.add(netLauncher());
        list.add(basic(RivalsItem.FLASH, flashbang(true, "flashbang")));
        list.add(basic(RivalsItem.ANCHOR, anchor()));
        list.add(basic(RivalsItem.GRAPPLE, grapple()));
        list.add(basic(RivalsItem.TRAMPOLINE, trampolineFolded("trampoline", Model.Kind.SMALL)));
        list.add(basic(RivalsItem.RECALL, recall("recall_crystal", false)));
        list.add(new ItemSet("recall_crystal_active", Def.BASIC, recall("recall_crystal_active", true)));
        list.add(basic(RivalsItem.DASH, dash()));
        list.add(basic(RivalsItem.BOOMERANG, boomerang()));
        list.add(basic(RivalsItem.STICKY, sticky("sticky_grenade", false, Model.Kind.SMALL)));
        list.add(basic(RivalsItem.ROCKET, rocketLauncher()));
        list.add(basic(RivalsItem.SHIELD, shieldDisc()));
        list.add(basic(RivalsItem.DECOY, decoy()));
        list.add(basic(RivalsItem.MIRROR, mirror()));
        list.add(basic(RivalsItem.MEDKIT, medkit()));
        return list;
    }

    private static ItemSet basic(RivalsItem item, Model m) {
        return new ItemSet(item.id, Def.BASIC, m);
    }

    // BRONIE GŁÓWNE
    static ItemSet energyBow() {
        ItemSet set = new ItemSet("energy_bow", Def.BOW, bow("energy_bow", -1));
        for (int i = 0; i < 3; i++) set.variant("pull" + i, bow("energy_bow_pull" + i, i));
        return set;
    }

    /** Łuk pionowy (strzała w stronę +X); pull = -1 spoczynek, 0..2 naciąg. */
    private static Model bow(String name, int pull) {
        Model m = new Model(name, Model.Kind.GUN).grip(9.75f, 8, 8).centered().gui(5, -20, 0);
        m.box(9, 5, 7.25f, 10.5f, 11, 8.75f, BODY);                                // majdan
        m.box(9.1f, 6.6f, 7.15f, 10.4f, 9.4f, 8.85f, CORE);                         // chwyt
        m.glow(10.5f, 7, 7.6f, 10.85f, 9, 8.4f, ACCENT);                            // rdzeń
        m.box(8.8f, 11, 7.4f, 10, 16.5f, 8.6f, BODY).rot("z", 22.5f, 9.4f, 11, 8);  // ramię górne
        m.glow(10, 11.5f, 7.7f, 10.3f, 16, 8.3f, ACCENT).rot("z", 22.5f, 9.4f, 11, 8);
        m.box(8.8f, -0.5f, 7.4f, 10, 5, 8.6f, BODY).rot("z", -22.5f, 9.4f, 5, 8);   // ramię dolne
        m.glow(10, 0, 7.7f, 10.3f, 4.5f, 8.3f, ACCENT).rot("z", -22.5f, 9.4f, 5, 8);
        m.box(6.6f, 15.4f, 7.5f, 8, 16.8f, 8.5f, CORE);                             // końcówki
        m.box(6.6f, -0.8f, 7.5f, 8, 0.6f, 8.5f, CORE);
        float sx = pull < 0 ? 7.2f : 6.2f - pull * 1.4f;
        m.glow(sx, 0.3f, 7.9f, sx + 0.3f, 15.7f, 8.1f, ACCENT);                     // cięciwa
        if (pull >= 0) {
            m.glow(sx, 15.4f, 7.9f, 7.2f, 15.7f, 8.1f, ACCENT);
            m.glow(sx, 0.3f, 7.9f, 7.2f, 0.6f, 8.1f, ACCENT);
            m.glow(sx, 7.65f, 7.75f, 15.5f, 8.35f, 8.25f, ACCENT);                  // pocisk energii
            m.glow(15.5f, 7.2f, 7.5f, 16.8f, 8.8f, 8.5f, pull == 2 ? WHITE : ACCENT);
        }
        return m;
    }

    static ItemSet crossbow() {
        ItemSet set = new ItemSet("crossbow", Def.CROSSBOW, crossbowModel("crossbow", 13.6f, false));
        set.variant("pull0", crossbowModel("crossbow_pull0", 11.6f, false));
        set.variant("pull1", crossbowModel("crossbow_pull1", 9.8f, false));
        set.variant("pull2", crossbowModel("crossbow_pull2", 8.2f, false));
        set.variant("charged", crossbowModel("crossbow_charged", 8.2f, true));
        return set;
    }

    private static Model crossbowModel(String name, float sx, boolean bolt) {
        Model m = new Model(name, Model.Kind.GUN).grip(4.5f, 4, 8).gui(35, -50, 15);
        m.box(-2, 4.2f, 7.1f, -0.5f, 7.8f, 8.9f, CORE);                // stopka
        m.box(-0.5f, 5, 7.2f, 9, 7.6f, 8.8f, BODY2);                   // kolba
        m.box(5, 6.8f, 7, 11, 8.6f, 9, BODY);                          // korpus
        m.box(9, 7.6f, 7.5f, 17, 8.4f, 8.5f, TRIM);                    // prowadnica
        m.box(12, 7, 6.6f, 14.5f, 9.2f, 9.4f, CORE);                   // gniazdo łęczyska
        m.box(12.5f, 7.4f, 1.5f, 14, 8.8f, 14.5f, BODY);               // łęczysko
        m.glow(12.6f, 7.6f, 0.9f, 13.9f, 8.6f, 1.5f, ACCENT);
        m.glow(12.6f, 7.6f, 14.5f, 13.9f, 8.6f, 15.1f, ACCENT);
        m.box(16.6f, 6.4f, 7.2f, 17.4f, 9.6f, 8.8f, TRIM);             // strzemię
        m.box(3.4f, 1.8f, 7.3f, 5.6f, 5.4f, 8.7f, CORE).rot("z", -22.5f, 4.5f, 5.4f, 8);
        m.glow(sx, 7.9f, 1.8f, sx + 0.3f, 8.3f, 14.2f, ACCENT);        // cięciwa
        if (sx < 13.4f) {
            m.glow(sx, 7.9f, 1.5f, 12.6f, 8.3f, 1.8f, ACCENT);
            m.glow(sx, 7.9f, 14.2f, 12.6f, 8.3f, 14.5f, ACCENT);
        }
        if (bolt) {
            m.glow(sx, 8.4f, 7.8f, 17, 8.9f, 8.2f, ACCENT);
            m.box(17, 8.2f, 7.6f, 18.5f, 9.1f, 8.4f, BODY);
        }
        return m;
    }

    static ItemSet rifle() {
        Model m = new Model("rifle", Model.Kind.GUN).grip(6.2f, 4, 8);
        m.box(-3, 4.2f, 7.2f, -1.5f, 8.6f, 8.8f, CORE);           // stopka
        m.box(-1.5f, 4.8f, 7.3f, 4, 8.6f, 8.7f, BODY);            // kolba
        m.box(-0.5f, 5.8f, 7.2f, 3, 7.4f, 8.8f, CORE);            // wycięcie
        m.box(4, 6.6f, 6.6f, 12, 10, 9.4f, BODY);                 // komora
        m.box(4, 6, 6.5f, 12, 6.6f, 9.5f, BODY2);
        m.glow(5, 7.6f, 9.4f, 11, 8.2f, 9.55f, ACCENT);
        m.glow(5, 7.6f, 6.45f, 11, 8.2f, 6.6f, ACCENT);
        m.box(5, 10, 7.3f, 11.5f, 10.5f, 8.7f, TRIM);             // szyna
        m.box(12, 6.4f, 6.9f, 17.5f, 9.6f, 9.1f, BODY2);          // łoże
        m.box(12.5f, 8.8f, 6.8f, 17, 9.1f, 9.2f, CORE);
        m.box(12.5f, 7.6f, 6.8f, 17, 7.9f, 9.2f, CORE);
        m.box(17.5f, 7.4f, 7.4f, 21, 8.6f, 8.6f, TRIM);           // lufa
        m.box(21, 7.1f, 7.1f, 22.2f, 8.9f, 8.9f, CORE);
        m.box(5, 2, 7.3f, 7.2f, 6, 8.7f, CORE).rot("z", -22.5f, 6.1f, 6, 8);
        m.box(9, 2.6f, 7.45f, 11, 6.2f, 8.55f, TRIM).rot("z", 22.5f, 10, 6.2f, 8);
        m.box(7.2f, 5, 7.6f, 8.8f, 5.4f, 8.4f, TRIM);
        m.box(6, 10.5f, 7.5f, 7, 11.2f, 8.5f, TRIM);              // luneta
        m.box(9.5f, 10.5f, 7.5f, 10.5f, 11.2f, 8.5f, TRIM);
        m.box(5.5f, 11.2f, 7.1f, 11.5f, 13, 8.9f, CORE);
        m.glow(11.5f, 11.4f, 7.3f, 11.8f, 12.8f, 8.7f, ACCENT);
        return new ItemSet("rifle", Def.BASIC, m);
    }

    static ItemSet shotgun() {
        Model m = new Model("shotgun", Model.Kind.GUN).grip(5.3f, 4, 8);
        m.box(-2.5f, 4.4f, 7.2f, -1, 8.4f, 8.8f, CORE);
        m.box(-1, 4.8f, 7.3f, 4, 8.4f, 8.7f, BODY2);
        m.box(4, 6.5f, 6.5f, 10, 10, 9.5f, BODY);
        m.box(4, 6, 6.4f, 10, 6.5f, 9.6f, TRIM);
        m.glow(5, 10, 7.4f, 9, 10.4f, 8.6f, ORANGE);
        m.box(10, 8.1f, 6.7f, 19, 9.5f, 7.95f, TRIM);             // lufy
        m.box(10, 8.1f, 8.05f, 19, 9.5f, 9.3f, TRIM);
        m.box(18.6f, 7.9f, 6.5f, 19.4f, 9.7f, 9.5f, CORE);
        m.box(10, 6.6f, 7.2f, 17, 7.9f, 8.8f, BODY2);             // magazynek rurowy
        m.box(11.5f, 6.2f, 6.6f, 15, 8.2f, 9.4f, BODY);           // pompka
        m.box(12, 6.1f, 6.5f, 12.4f, 8.3f, 9.5f, CORE);
        m.box(13, 6.1f, 6.5f, 13.4f, 8.3f, 9.5f, CORE);
        m.box(14, 6.1f, 6.5f, 14.4f, 8.3f, 9.5f, CORE);
        m.box(4.2f, 2, 7.3f, 6.4f, 6, 8.7f, CORE).rot("z", -22.5f, 5.3f, 6, 8);
        m.box(6.5f, 5, 7.6f, 8, 5.4f, 8.4f, TRIM);
        return new ItemSet("shotgun", Def.BASIC, m);
    }

    // BRONIE DODATKOWE
    static ItemSet pistol() {
        Model m = new Model("pistol", Model.Kind.GUN).grip(5, 4.5f, 8);
        m.box(3, 8, 7, 13, 10.6f, 9, BODY);                       // zamek
        m.box(3.5f, 8.6f, 6.95f, 3.9f, 10, 9.05f, CORE);
        m.box(4.3f, 8.6f, 6.95f, 4.7f, 10, 9.05f, CORE);
        m.box(5.1f, 8.6f, 6.95f, 5.5f, 10, 9.05f, CORE);
        m.glow(5, 10.6f, 7.8f, 12, 10.9f, 8.2f, ACCENT);
        m.glow(9, 8.9f, 9, 12.4f, 9.4f, 9.1f, ACCENT);
        m.glow(9, 8.9f, 6.9f, 12.4f, 9.4f, 7, ACCENT);
        m.box(3, 6.5f, 7.15f, 11.5f, 8, 8.85f, BODY2);            // szkielet
        m.box(13, 8.7f, 7.6f, 13.6f, 9.7f, 8.4f, CORE);
        m.box(3.6f, 2, 7.3f, 6.1f, 6.5f, 8.7f, CORE).rot("z", -22.5f, 4.85f, 6.5f, 8);
        m.box(3.4f, 3.6f, 7.25f, 6.3f, 4, 8.75f, TRIM).rot("z", -22.5f, 4.85f, 6.5f, 8);
        m.box(6.6f, 5.2f, 7.6f, 8.4f, 5.6f, 8.4f, TRIM);
        m.box(6.4f, 5.6f, 7.6f, 6.8f, 6.5f, 8.4f, TRIM);
        return new ItemSet("pistol", Def.BASIC, m);
    }

    static ItemSet slingshot() {
        Model m = new Model("slingshot", Model.Kind.MELEE).grip(8, 3, 8).gui(10, -25, -20);
        m.box(7.2f, 0, 7.2f, 8.8f, 6, 8.8f, BODY2);                                   // rękojeść
        m.box(7.1f, 1.2f, 7.1f, 8.9f, 1.8f, 8.9f, CORE);
        m.box(7.1f, 3, 7.1f, 8.9f, 3.6f, 8.9f, CORE);
        m.box(6.2f, 5.6f, 7.3f, 9.8f, 7, 8.7f, BODY);                                  // rozwidlenie
        m.box(5.3f, 6.5f, 7.4f, 6.7f, 12.5f, 8.6f, BODY).rot("z", 22.5f, 6, 6.5f, 8);   // lewe ramię
        m.box(9.3f, 6.5f, 7.4f, 10.7f, 12.5f, 8.6f, BODY).rot("z", -22.5f, 10, 6.5f, 8); // prawe ramię
        m.glow(3, 11.4f, 7.3f, 4.4f, 12.8f, 8.7f, ACCENT);                              // końcówki
        m.glow(11.6f, 11.4f, 7.3f, 13, 12.8f, 8.7f, ACCENT);
        m.glow(4.4f, 11.9f, 7.9f, 11.6f, 12.2f, 8.1f, ACCENT);                          // guma
        m.box(7.1f, 11.3f, 7.5f, 8.9f, 12.7f, 8.5f, CORE);                             // koszyk z kamieniem
        return new ItemSet("slingshot", Def.BASIC, m);
    }

    // BIAŁA
    static ItemSet sword() {
        Model m = new Model("sword", Model.Kind.MELEE).grip(8, 2.4f, 8);
        m.box(7, -0.2f, 7, 9, 1, 9, TRIM);                         // głowica
        m.glow(7.5f, 0.1f, 6.9f, 8.5f, 0.7f, 9.1f, ACCENT);
        m.box(7.35f, 1, 7.35f, 8.65f, 4.2f, 8.65f, CORE);          // rękojeść
        m.box(7.25f, 1.8f, 7.25f, 8.75f, 2.2f, 8.75f, TRIM);
        m.box(7.25f, 3, 7.25f, 8.75f, 3.4f, 8.75f, TRIM);
        m.box(4.6f, 4.2f, 6.9f, 11.4f, 5.3f, 9.1f, BODY2);         // jelec
        m.box(4, 4.4f, 7.1f, 4.6f, 5.1f, 8.9f, TRIM);
        m.box(11.4f, 4.4f, 7.1f, 12, 5.1f, 8.9f, TRIM);
        m.glow(7.4f, 4.3f, 9.1f, 8.6f, 5.2f, 9.2f, ACCENT);
        m.glow(7.4f, 4.3f, 6.8f, 8.6f, 5.2f, 6.9f, ACCENT);
        m.box(6.6f, 5.3f, 7.6f, 9.4f, 14.2f, 8.4f, BODY);          // głownia
        m.box(6.4f, 5.3f, 7.75f, 6.6f, 14, 8.25f, BODY2);
        m.box(9.4f, 5.3f, 7.75f, 9.6f, 14, 8.25f, BODY2);
        m.glow(7.7f, 6, 7.55f, 8.3f, 13.5f, 8.45f, ACCENT);       // zbrocze
        m.box(7, 13.2f, 7.6f, 9, 15.2f, 8.4f, BODY).rot("z", 45, 8, 14.2f, 8);
        return new ItemSet("sword", Def.BASIC, m);
    }

    static ItemSet katana() {
        Model m = new Model("katana", Model.Kind.MELEE).grip(8, 2.5f, 8);
        m.box(7.2f, -0.4f, 7.2f, 8.8f, 0.2f, 8.8f, GOLD_DARK);     // kashira
        m.box(7.4f, 0.2f, 7.4f, 8.6f, 5, 8.6f, CORE);              // tsuka
        m.box(7.3f, 0.8f, 7.3f, 8.7f, 1.3f, 8.7f, RED);
        m.box(7.3f, 2.2f, 7.3f, 8.7f, 2.7f, 8.7f, RED);
        m.box(7.3f, 3.6f, 7.3f, 8.7f, 4.1f, 8.7f, RED);
        m.box(6, 5, 6, 10, 5.8f, 10, GOLD);                        // tsuba
        m.box(6.6f, 5.8f, 6.6f, 9.4f, 6.1f, 9.4f, GOLD_DARK);
        m.box(7.4f, 6.1f, 7.6f, 8.8f, 7, 8.4f, GOLD_LIGHT);         // habaki
        m.box(7.5f, 7, 7.75f, 8.8f, 17, 8.25f, BODY);              // głownia
        m.glow(8.8f, 7.2f, 7.85f, 9.1f, 16.6f, 8.15f, RED);
        m.box(7.3f, 16.6f, 7.75f, 8.5f, 19.4f, 8.25f, BODY).rot("z", -22.5f, 8, 16.6f, 8);
        return new ItemSet("katana", Def.BASIC, m);
    }

    static ItemSet hammer() {
        ItemSet set = new ItemSet("hammer", Def.HAMMER, hammerModel("hammer", -1));
        for (int i = 0; i < 3; i++) set.variant("c" + i, hammerModel("hammer_c" + i, i));
        return set;
    }

    /** charge = -1 spoczynek, 0..2 etapy ładowania (rdzeń i obuchy coraz mocniej świecą). */
    private static Model hammerModel(String name, int charge) {
        Model m = new Model(name, Model.Kind.MELEE).grip(8, 3, 8);
        float g = charge == 2 ? 0.4f : 0;
        m.box(7.2f, -1.6f, 7.2f, 8.8f, -0.8f, 8.8f, BODY2);        // głowica
        m.box(7.5f, -0.8f, 7.5f, 8.5f, 10.5f, 8.5f, TRIM);         // trzonek
        m.box(7.35f, 0, 7.35f, 8.65f, 4, 8.65f, CORE);             // owijka
        m.box(7.1f, 9.5f, 7.1f, 8.9f, 10.5f, 8.9f, BODY2);         // kołnierz
        m.box(3.5f - g, 10.5f, 5 - g, 12.5f + g, 15 + g, 11 + g, BODY);   // obuch
        m.box(5, 15 + g, 6, 11, 15.6f + g, 10, BODY2);
        int face = charge >= 2 ? ORANGE : CORE;
        if (charge >= 2) {
            m.glow(2.8f - g, 11, 5.5f, 3.5f - g, 14.5f, 10.5f, face);
            m.glow(12.5f + g, 11, 5.5f, 13.2f + g, 14.5f, 10.5f, face);
        } else {
            m.box(2.8f, 11, 5.5f, 3.5f, 14.5f, 10.5f, face);
            m.box(12.5f, 11, 5.5f, 13.2f, 14.5f, 10.5f, face);
        }
        int core = charge < 0 ? ACCENT : charge == 0 ? GOLD : ORANGE;
        m.glow(6.5f, 11.3f, 11 + g, 9.5f, 14.2f, 11.15f + g, core);
        m.glow(6.5f, 11.3f, 4.85f - g, 9.5f, 14.2f, 5 - g, core);
        if (charge >= 1) {
            m.glow(3.5f - g, 15 + g, 5.3f, 12.5f + g, 15.3f + g, 5.6f, ORANGE);
            m.glow(3.5f - g, 15 + g, 10.4f, 12.5f + g, 15.3f + g, 10.7f, ORANGE);
        }
        return m;
    }

    // GADŻETY
    static Model cryo() {
        // kula z lodowej skorupy (ośmiokąt z dwóch prostopadłościanów), metalowa obręcz, świecący pulsujący rdzeń w okienkach
        Model m = new Model("cryo_grenade", Model.Kind.SMALL);
        m.box(5.5f, 5, 6.2f, 10.5f, 11, 9.8f, ICE);
        m.box(6.2f, 5, 5.5f, 9.8f, 11, 10.5f, ICE);
        m.box(5, 5.6f, 6.6f, 11, 10.4f, 9.4f, ICE_DARK);
        m.box(6.6f, 5.6f, 5, 9.4f, 10.4f, 11, ICE_DARK);
        m.anim(6.9f, 8.4f, 4.85f, 9.1f, 10, 5.05f, "core_pulse", ICE);       // okienka rdzenia
        m.anim(6.9f, 8.4f, 10.95f, 9.1f, 10, 11.15f, "core_pulse", ICE);
        m.anim(4.85f, 8.4f, 6.9f, 5.05f, 10, 9.1f, "core_pulse", ICE);
        m.anim(10.95f, 8.4f, 6.9f, 11.15f, 10, 9.1f, "core_pulse", ICE);
        m.box(4.8f, 6.6f, 6.4f, 11.2f, 7.6f, 9.6f, BODY2);                  // obręcz
        m.box(6.4f, 6.6f, 4.8f, 9.6f, 7.6f, 11.2f, BODY2);
        m.box(6.8f, 11, 6.8f, 9.2f, 12, 9.2f, BODY);                         // nakrętka
        m.box(9.2f, 11.2f, 7.6f, 11.2f, 11.7f, 8.4f, TRIM);                  // dźwignia
        m.box(10.7f, 8.6f, 7.6f, 11.2f, 11.7f, 8.4f, TRIM);
        m.box(7, 4.4f, 7, 9, 5, 9, BODY);
        return m;
    }

    static Model cage() {
        Model m = new Model("cage_mine", Model.Kind.SMALL).iconTop();
        m.box(4, 6.5f, 5.5f, 12, 8, 10.5f, BODY);
        m.box(5.5f, 6.5f, 4, 10.5f, 8, 12, BODY);
        m.box(4.5f, 6.4f, 6, 11.5f, 6.6f, 10, CORE);
        m.box(5.5f, 8, 5.5f, 10.5f, 8.5f, 10.5f, TRIM);
        m.box(6.5f, 8.5f, 6.5f, 9.5f, 8.7f, 9.5f, CORE);
        m.anim(7.5f, 8.7f, 7.5f, 8.5f, 9.2f, 8.5f, "led_blink", RED);
        return m;
    }

    static ItemSet netLauncher() {
        ItemSet set = new ItemSet("net_launcher", Def.NET, netModel("net_launcher", 0));
        set.variant("cd_rot22", netModel("net_launcher_cd_rot22", 22.5f).cooldownVariant());
        set.variant("cd_rot45", netModel("net_launcher_cd_rot45", 45).cooldownVariant());
        return set;
    }

    private static Model netModel(String name, float drumRot) {
        Model m = new Model(name, Model.Kind.GUN).grip(4.2f, 4.5f, 8);
        m.box(1, 6, 7, 9, 9.2f, 9, BODY);
        m.box(1, 9.2f, 7.4f, 7, 9.7f, 8.6f, TRIM);
        m.box(8, 6.5f, 5.5f, 11, 9.5f, 10.5f, BODY2).rot("x", drumRot, 9.5f, 8, 8);
        m.box(8, 5.5f, 6.5f, 11, 10.5f, 9.5f, BODY2).rot("x", drumRot, 9.5f, 8, 8);
        m.glow(8.2f, 10.4f, 7.6f, 10.8f, 10.7f, 8.4f, ACCENT).rot("x", drumRot, 9.5f, 8, 8);
        m.box(8.6f, 7.4f, 5.3f, 10.4f, 8.6f, 10.7f, CORE).rot("x", drumRot, 9.5f, 8, 8);
        m.box(11, 6.5f, 6.5f, 15, 9.5f, 9.5f, CORE);               // szeroka lufa
        m.box(14.5f, 6.2f, 6.2f, 15.5f, 9.8f, 9.8f, TRIM);
        m.box(15, 7, 7, 15.3f, 9, 9, LIGHT);                        // zwinięta sieć w wylocie
        m.box(3, 2, 7.25f, 5.2f, 6, 8.75f, CORE).rot("z", -22.5f, 4.1f, 6, 8);
        return m;
    }

    static Model flashbang(boolean pin, String name) {
        Model m = new Model(name, pin ? Model.Kind.SMALL : Model.Kind.WORLD);
        m.box(6, 3, 6, 10, 12, 10, BODY);
        m.box(5.5f, 4, 6.5f, 10.5f, 11, 9.5f, BODY);
        m.box(6.5f, 4, 5.5f, 9.5f, 11, 10.5f, BODY);
        m.box(5.4f, 5, 6.4f, 10.6f, 5.8f, 9.6f, CORE);
        m.box(6.4f, 5, 5.4f, 9.6f, 5.8f, 10.6f, CORE);
        m.glow(5.4f, 8, 6.4f, 10.6f, 9, 9.6f, WHITE);
        m.glow(6.4f, 8, 5.4f, 9.6f, 9, 10.6f, WHITE);
        m.box(6.5f, 12, 6.5f, 9.5f, 13, 9.5f, TRIM);
        if (pin) {
            m.box(9.5f, 12.3f, 7.75f, 11.5f, 12.8f, 8.25f, BODY2);     // dźwignia
            m.box(11, 11, 7.6f, 13, 13.4f, 8.4f, GOLD);                 // zawleczka
            m.box(11.5f, 11.5f, 7.5f, 12.5f, 12.9f, 8.5f, CORE);
        }
        return m;
    }

    static Model anchor() {
        Model m = new Model("gravity_anchor", Model.Kind.MELEE).grip(8, 7, 8).gui(10, -25, -20);
        m.box(7.4f, 3, 7.4f, 8.6f, 13, 8.6f, BODY2);
        m.box(6.5f, 13, 7.4f, 9.5f, 15.4f, 8.6f, TRIM);
        m.box(7.3f, 13.6f, 7.2f, 8.7f, 14.8f, 8.8f, CORE);
        m.box(5, 11, 7.4f, 11, 12, 8.6f, TRIM);
        m.box(2.5f, 3, 7.4f, 7.4f, 4.5f, 8.6f, BODY);
        m.box(8.6f, 3, 7.4f, 13.5f, 4.5f, 8.6f, BODY);
        m.box(2, 3.4f, 7.3f, 3.6f, 6.6f, 8.7f, BODY2);
        m.box(12.4f, 3.4f, 7.3f, 14, 6.6f, 8.7f, BODY2);
        m.box(6.6f, 2.2f, 7.3f, 9.4f, 3.4f, 8.7f, TRIM);
        m.glow(7.1f, 5.5f, 7.1f, 8.9f, 9.5f, 8.9f, PURPLE);
        return m;
    }

    static Model grapple() {
        Model m = new Model("grapple", Model.Kind.GUN).grip(4, 4.5f, 8);
        m.box(1, 6, 7, 10, 9, 9, BODY);
        m.box(5, 9, 7.2f, 8.5f, 10.3f, 8.8f, BODY2);                 // zwój linki
        m.box(5.5f, 9.2f, 7.1f, 8, 10.1f, 8.9f, WOOD);
        m.glow(1.5f, 7.3f, 9, 9, 7.8f, 9.1f, ACCENT);
        m.box(10, 6.6f, 7.25f, 12.5f, 8.4f, 8.75f, TRIM);
        m.box(12.5f, 7, 7.5f, 15.5f, 8, 8.5f, CORE);                  // grot
        m.box(12.5f, 8, 7.5f, 14.5f, 9.6f, 8.5f, CORE).rot("z", -22.5f, 12.5f, 8, 8);
        m.box(12.5f, 5.4f, 7.5f, 14.5f, 7, 8.5f, CORE).rot("z", 22.5f, 12.5f, 7, 8);
        m.box(3, 2, 7.25f, 5.2f, 6, 8.75f, CORE).rot("z", -22.5f, 4.1f, 6, 8);
        return m;
    }

    static Model trampolineFolded(String name, Model.Kind kind) {
        Model m = new Model(name, kind).iconTop();
        m.box(3, 6, 3, 13, 9, 13, BODY);
        m.box(2.5f, 6.8f, 2.5f, 13.5f, 8.2f, 13.5f, BODY2);
        m.box(3.5f, 9, 3.5f, 12.5f, 9.3f, 12.5f, CORE);
        m.glow(4.5f, 9.3f, 4.5f, 11.5f, 9.6f, 11.5f, ACCENT);
        return m;
    }

    static Model recall(String name, boolean active) {
        Model m = new Model(name, Model.Kind.SMALL);
        m.box(6, 3, 6, 10, 4.5f, 10, BODY);
        m.box(6.5f, 2, 6.5f, 9.5f, 3, 9.5f, TRIM);
        m.box(5.6f, 4.5f, 7.6f, 6.4f, 7, 8.4f, BODY2);
        m.box(9.6f, 4.5f, 7.6f, 10.4f, 7, 8.4f, BODY2);
        if (active) {
            m.glow(6.5f, 4.5f, 6.5f, 9.5f, 12.5f, 9.5f, argb(0xC0, ACCENT)).rot("y", 45, 8, 8, 8);
            m.glow(7.25f, 5.5f, 7.25f, 8.75f, 11.5f, 8.75f, WHITE).rot("y", 45, 8, 8, 8);
        } else {
            m.box(6.5f, 4.5f, 6.5f, 9.5f, 12.5f, 9.5f, argb(0xB0, ICE)).rot("y", 45, 8, 8, 8);
            m.glow(7.25f, 5.5f, 7.25f, 8.75f, 11.5f, 8.75f, ACCENT_DARK).rot("y", 45, 8, 8, 8);
        }
        return m;
    }

    static Model dash() {
        Model m = new Model("dash_gauntlet", Model.Kind.SMALL);
        m.box(5, 5, 5.5f, 11, 11, 10.5f, BODY);                      // pancerz dłoni
        m.box(11, 5.5f, 6, 12.6f, 10.5f, 10, CORE);                   // palce
        m.box(12.6f, 6, 6.3f, 13.2f, 10, 9.7f, TRIM);
        m.box(3, 5.5f, 6, 5, 10.5f, 10, BODY2);                       // mankiet
        m.box(2.8f, 6.5f, 5.8f, 3.6f, 9.5f, 10.2f, CORE);
        m.box(6, 11, 6.5f, 10, 11.6f, 9.5f, TRIM);
        m.glow(2.2f, 6.2f, 6.4f, 2.8f, 7.6f, 7.8f, ACCENT);           // dysze
        m.glow(2.2f, 8.4f, 8.2f, 2.8f, 9.8f, 9.6f, ACCENT);
        m.glow(2.2f, 8.4f, 6.4f, 2.8f, 9.8f, 7.8f, ACCENT);
        m.glow(2.2f, 6.2f, 8.2f, 2.8f, 7.6f, 9.6f, ACCENT);
        m.glow(6.5f, 7.5f, 10.5f, 9.5f, 8.5f, 10.6f, ACCENT);
        return m;
    }

    static Model boomerang() {
        Model m = new Model("boomerang", Model.Kind.SMALL).iconTop().gui(55, -40, 0);
        m.box(8, 7.5f, 6.8f, 15.5f, 8.5f, 9.6f, BODY).rot("y", 45, 8, 8, 8);
        m.box(8, 7.5f, 6.4f, 15.5f, 8.5f, 9.2f, BODY).rot("y", -45, 8, 8, 8);
        m.glow(8, 8.5f, 9.3f, 15.5f, 8.7f, 9.6f, ACCENT).rot("y", 45, 8, 8, 8);
        m.glow(8, 8.5f, 6.4f, 15.5f, 8.7f, 6.7f, ACCENT).rot("y", -45, 8, 8, 8);
        m.box(6.5f, 7.2f, 6.5f, 9.5f, 8.8f, 9.5f, CORE);
        m.box(7.3f, 8.8f, 7.3f, 8.7f, 9.1f, 8.7f, TRIM);
        return m;
    }

    static Model sticky(String name, boolean lit, Model.Kind kind) {
        Model m = new Model(name, kind);
        m.box(5.5f, 5, 5.5f, 10.5f, 10, 10.5f, CORE);
        m.box(5, 5.8f, 5, 11, 7, 11, GREEN);
        m.box(6, 4.4f, 6, 10, 5, 10, GREEN);
        m.box(6.5f, 10, 6.5f, 9.5f, 10.6f, 9.5f, TRIM);
        if (lit) m.glow(7.5f, 10.6f, 7.5f, 8.5f, 11.4f, 8.5f, RED);
        else m.box(7.5f, 10.6f, 7.5f, 8.5f, 11.4f, 8.5f, RED_DARK);
        return m;
    }

    static Model rocketLauncher() {
        Model m = new Model("rocket_launcher", Model.Kind.GUN).grip(7, 4.5f, 8);
        m.box(0, 6.25f, 6.25f, 16, 9.75f, 9.75f, BODY);              // tuba
        m.box(15, 5.75f, 5.75f, 17, 10.25f, 10.25f, CORE);
        m.box(-1, 5.75f, 5.75f, 1, 10.25f, 10.25f, CORE);
        m.box(3, 5.9f, 5.9f, 4, 10.1f, 10.1f, TRIM);
        m.box(11, 5.9f, 5.9f, 12, 10.1f, 10.1f, TRIM);
        m.glow(4.5f, 9.75f, 7.6f, 10.5f, 10.05f, 8.4f, ORANGE);
        m.box(5, 9.75f, 6.8f, 8, 11.2f, 7.4f, TRIM);                 // celownik
        m.box(5.5f, 11.2f, 6.6f, 7.5f, 12.4f, 7.6f, CORE);
        m.box(6, 2.4f, 7.25f, 8, 6.25f, 8.75f, CORE);                 // chwyt
        m.box(10, 3.4f, 7.3f, 11.6f, 6.25f, 8.7f, CORE);              // przedni chwyt
        m.glow(17, 7, 7, 17.3f, 9, 9, ORANGE);
        return m;
    }

    static Model shieldDisc() {
        Model m = new Model("shield_disc", Model.Kind.SMALL).iconTop();
        m.box(4, 7, 5.5f, 12, 9, 10.5f, BODY);
        m.box(5.5f, 7, 4, 10.5f, 9, 12, BODY);
        m.box(4.5f, 6.8f, 6, 11.5f, 7, 10, CORE);
        m.box(6, 9, 6, 10, 9.4f, 10, TRIM);
        m.anim(6.5f, 9.4f, 6.5f, 9.5f, 9.7f, 9.5f, "energy", ACCENT);
        return m;
    }

    static Model decoy() {
        Model m = new Model("decoy", Model.Kind.SMALL);
        m.box(5, 3.5f, 5, 11, 5.5f, 11, BODY);
        m.box(5.5f, 3.2f, 5.5f, 10.5f, 3.5f, 10.5f, CORE);
        m.box(6, 5.5f, 6, 10, 6, 10, TRIM);
        m.glow(6.5f, 6, 6.5f, 9.5f, 6.3f, 9.5f, ACCENT);
        m.glow(7.25f, 6.3f, 7.5f, 8.75f, 10.3f, 8.5f, argb(0x90, ACCENT));
        m.glow(7.4f, 10.3f, 7.4f, 8.6f, 11.6f, 8.6f, argb(0x90, ACCENT));
        return m;
    }

    static Model mirror() {
        Model m = new Model("mirror", Model.Kind.MELEE).grip(8, 3, 8).gui(5, -20, -20);
        m.box(7.5f, 0, 7.5f, 8.5f, 6, 8.5f, GOLD_DARK);
        m.box(7.3f, 1, 7.3f, 8.7f, 1.6f, 8.7f, GOLD);
        m.box(4, 6, 7.25f, 12, 15, 8.75f, GOLD);
        m.box(3.6f, 7, 7.35f, 4, 14, 8.65f, GOLD_DARK);
        m.box(12, 7, 7.35f, 12.4f, 14, 8.65f, GOLD_DARK);
        m.box(5, 7, 8.75f, 11, 14, 8.85f, ICE);
        m.glow(5.5f, 12, 8.85f, 7, 13.5f, 8.9f, WHITE);
        m.box(5, 7, 7.15f, 11, 14, 7.25f, GOLD_DARK);
        return m;
    }

    static Model medkit() {
        Model m = new Model("medkit", Model.Kind.SMALL);
        m.box(3, 4, 5, 13, 11, 11, WHITE);
        m.box(3, 7, 4.8f, 13, 7.5f, 11.2f, BODY2);
        m.box(2.8f, 4, 5.5f, 3, 11, 10.5f, BODY2);
        m.box(13, 4, 5.5f, 13.2f, 11, 10.5f, BODY2);
        m.glow(7, 5.5f, 11, 9, 9.5f, 11.1f, RED);
        m.glow(5.5f, 6.5f, 11, 10.5f, 8.5f, 11.1f, RED);
        m.box(6, 11, 7.5f, 10, 12.5f, 8.5f, CORE);
        m.box(6, 11, 7.6f, 6.6f, 12.5f, 8.4f, TRIM);
        m.box(9.4f, 11, 7.6f, 10, 12.5f, 8.4f, TRIM);
        return m;
    }
}
