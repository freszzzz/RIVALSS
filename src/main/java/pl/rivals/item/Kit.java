package pl.rivals.item;

import pl.rivals.shared.RivalsItem;

/** Zestaw gracza: broń główna, dodatkowa, biała i gadżet. */
public record Kit(RivalsItem primary, RivalsItem secondary, RivalsItem melee, RivalsItem gadget) {

    public static final Kit FALLBACK = new Kit(RivalsItem.RIFLE, RivalsItem.PISTOL, RivalsItem.SWORD, RivalsItem.CRYO);

    public RivalsItem get(RivalsItem.Slot slot) {
        return switch (slot) {
            case PRIMARY -> primary;
            case SECONDARY -> secondary;
            case MELEE -> melee;
            case GADGET -> gadget;
        };
    }

    public Kit with(RivalsItem item) {
        return switch (item.slot) {
            case PRIMARY -> new Kit(item, secondary, melee, gadget);
            case SECONDARY -> new Kit(primary, item, melee, gadget);
            case MELEE -> new Kit(primary, secondary, item, gadget);
            case GADGET -> new Kit(primary, secondary, melee, item);
        };
    }

    public RivalsItem[] items() {
        return new RivalsItem[]{primary, secondary, melee, gadget};
    }

    public String serialize() {
        return primary.id + "," + secondary.id + "," + melee.id + "," + gadget.id;
    }

    /** Parsuje "główna,dodatkowa,biała,gadżet"; niepoprawne pozycje zastępuje domyślnymi. */
    public static Kit parse(String s, Kit fallback) {
        Kit def = fallback == null ? FALLBACK : fallback;
        if (s == null || s.isBlank()) return def;
        String[] p = s.split(",");
        Kit k = def;
        for (String part : p) {
            RivalsItem item = RivalsItem.byId(part.trim());
            if (item != null) k = k.with(item);
        }
        return k;
    }
}
