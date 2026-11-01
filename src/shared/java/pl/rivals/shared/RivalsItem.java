package pl.rivals.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Wszystkie przedmioty trybu - wspólne dla pluginu i generatora resource packa. */
public enum RivalsItem {

    ENERGY_BOW("energy_bow", Slot.PRIMARY, null, "Łuk energetyczny", "Naciągnij i wypuść pocisk energii."),
    CROSSBOW("crossbow", Slot.PRIMARY, null, "Kusza", "Wolny, ale bardzo celny bełt."),
    RIFLE("rifle", Slot.PRIMARY, null, "Karabin", "Strzał natychmiastowy. Trafienie w głowę ×1.5."),
    SHOTGUN("shotgun", Slot.PRIMARY, null, "Strzelba", "Rozrzut śrutu – zabójcza z bliska."),

    PISTOL("pistol", Slot.SECONDARY, null, "Pistolet", "Szybki i lekki, duży magazynek."),
    SLINGSHOT("slingshot", Slot.SECONDARY, null, "Proca", "Kamień lecący łukiem, mocny odrzut."),

    SWORD("sword", Slot.MELEE, null, "Miecz", "Zbalansowane cięcia."),
    KATANA("katana", Slot.MELEE, null, "Katana", "Szybkie cięcia, bonus przy sprincie."),
    HAMMER("hammer", Slot.MELEE, null, "Młot sejsmiczny", "Przytrzymaj PPM i puść – fala uderzeniowa."),

    CRYO("cryo_grenade", Slot.GADGET, Group.CONTROL, "Kriogranat", "Zamraża przeciwnika w bryle lodu."),
    CAGE("cage_mine", Slot.GADGET, Group.CONTROL, "Klatka-mina", "Ukryta mina zamyka wroga w klatce."),
    NET("net_launcher", Slot.GADGET, Group.CONTROL, "Wyrzutnia sieci", "Sieć spowalnia i blokuje skok."),
    FLASH("flashbang", Slot.GADGET, Group.CONTROL, "Granat oślepiający", "Oślepia, jeśli wróg patrzy w błysk."),
    ANCHOR("gravity_anchor", Slot.GADGET, Group.CONTROL, "Kotwica grawitacyjna", "Przyciąga wroga i ściąga z powietrza."),

    GRAPPLE("grapple", Slot.GADGET, Group.MOVEMENT, "Hak z linką", "Przyciąga cię do trafionego bloku."),
    TRAMPOLINE("trampoline", Slot.GADGET, Group.MOVEMENT, "Rzucana trampolina", "Rozkłada się i wyrzuca w górę."),
    RECALL("recall_crystal", Slot.GADGET, Group.MOVEMENT, "Kryształ powrotu", "Znacznik, potem powrót i leczenie."),
    DASH("dash_gauntlet", Slot.GADGET, Group.MOVEMENT, "Rękawica doskoku", "Błyskawiczny doskok o 5 bloków."),

    BOOMERANG("boomerang", Slot.GADGET, Group.DAMAGE, "Bumerang-ostrze", "Leci łukiem i wraca. Trafia dwa razy."),
    STICKY("sticky_grenade", Slot.GADGET, Group.DAMAGE, "Lepki granat", "Przykleja się i wybucha po 2 s."),
    ROCKET("rocket_launcher", Slot.GADGET, Group.DAMAGE, "Wyrzutnia rakiet", "Obrażenia obszarowe. Rocket jump!"),

    SHIELD("shield_disc", Slot.GADGET, Group.DEFENSE, "Rozkładana tarcza", "Ściana energii blokuje pociski."),
    DECOY("decoy", Slot.GADGET, Group.DEFENSE, "Atrapa", "Klon biegnie, a ty znikasz na chwilę."),
    MIRROR("mirror", Slot.GADGET, Group.DEFENSE, "Lustro zamiany", "Zamienia cię miejscami z wrogiem."),
    MEDKIT("medkit", Slot.GADGET, Group.DEFENSE, "Apteczka", "Leczy, ale musisz stać 1 s.");

    public enum Slot {
        PRIMARY("BROŃ GŁÓWNA"), SECONDARY("BROŃ DODATKOWA"), MELEE("BROŃ BIAŁA"), GADGET("GADŻET");

        public final String label;

        Slot(String label) {
            this.label = label;
        }
    }

    public enum Group {
        CONTROL("KONTROLA"), MOVEMENT("RUCH"), DAMAGE("OBRAŻENIA"), DEFENSE("OBRONA");

        public final String label;

        Group(String label) {
            this.label = label;
        }
    }

    public final String id;
    public final Slot slot;
    public final Group group;
    public final String displayName;
    public final String description;

    RivalsItem(String id, Slot slot, Group group, String displayName, String description) {
        this.id = id;
        this.slot = slot;
        this.group = group;
        this.displayName = displayName;
        this.description = description;
    }

    /** Klucz item_model: rivals:&lt;id&gt; (definicja w assets/rivals/items/). */
    public String modelKey() {
        return "rivals:" + id;
    }

    public static RivalsItem byId(String id) {
        if (id == null) return null;
        String s = id.toLowerCase(Locale.ROOT);
        for (RivalsItem i : values()) if (i.id.equals(s)) return i;
        return null;
    }

    public static List<RivalsItem> of(Slot slot) {
        List<RivalsItem> out = new ArrayList<>();
        for (RivalsItem i : values()) if (i.slot == slot) out.add(i);
        return out;
    }
}
