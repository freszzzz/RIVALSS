package pl.rivals.shared;

import java.util.ArrayList;
import java.util.List;

/** Przedmioty sklepu: skórki broni, efekty zabójstwa, kolory śladu doskoku. Ceny w config.yml. */
public enum Cosmetic {

    // skórki (inny model tego samego przedmiotu: rivals:<broń>__<skórka>)
    SWORD_NEON(Type.SKIN, "sword_neon", RivalsItem.SWORD, "Miecz Neon", 0xB47CFF),
    SWORD_GOLD(Type.SKIN, "sword_gold", RivalsItem.SWORD, "Miecz Złoty", 0xFFC83D),
    KATANA_SAKURA(Type.SKIN, "katana_sakura", RivalsItem.KATANA, "Katana Sakura", 0xFF8FB8),
    HAMMER_MAGMA(Type.SKIN, "hammer_magma", RivalsItem.HAMMER, "Młot Magma", 0xFF8A3D),
    RIFLE_ARCTIC(Type.SKIN, "rifle_arctic", RivalsItem.RIFLE, "Karabin Arktyka", 0xA8ECFF),
    PISTOL_GOLD(Type.SKIN, "pistol_gold", RivalsItem.PISTOL, "Pistolet Złoty", 0xFFC83D),
    SHOTGUN_TOXIC(Type.SKIN, "shotgun_toxic", RivalsItem.SHOTGUN, "Strzelba Toksyna", 0x5CE18A),
    BOW_VOID(Type.SKIN, "bow_void", RivalsItem.ENERGY_BOW, "Łuk Pustki", 0xA77BFF),

    // efekty zabójstwa
    KILL_ICE(Type.KILL_EFFECT, "kill_ice", null, "Kostki lodu", 0xA8ECFF),
    KILL_CONFETTI(Type.KILL_EFFECT, "kill_confetti", null, "Konfetti", 0xFFC83D),
    KILL_LIGHTNING(Type.KILL_EFFECT, "kill_lightning", null, "Błyskawica", 0xEEF2F6),
    KILL_PIXEL(Type.KILL_EFFECT, "kill_pixel", null, "Pikselowy rozpad", 0x38E8F0),

    // ślady doskoku
    TRAIL_GOLD(Type.TRAIL, "trail_gold", null, "Ślad: złoto", 0xFFC83D),
    TRAIL_RED(Type.TRAIL, "trail_red", null, "Ślad: czerwień", 0xFF4D5E),
    TRAIL_PURPLE(Type.TRAIL, "trail_purple", null, "Ślad: fiolet", 0xA77BFF),
    TRAIL_GREEN(Type.TRAIL, "trail_green", null, "Ślad: zieleń", 0x5CE18A),
    TRAIL_WHITE(Type.TRAIL, "trail_white", null, "Ślad: biel", 0xEEF2F6);

    public enum Type {
        SKIN("SKÓRKI"), KILL_EFFECT("EFEKTY ZABÓJSTWA"), TRAIL("ŚLADY DOSKOKU");

        public final String label;

        Type(String label) {
            this.label = label;
        }
    }

    public final Type type;
    public final String id;
    public final RivalsItem weapon;
    public final String displayName;
    public final int color;

    Cosmetic(Type type, String id, RivalsItem weapon, String displayName, int color) {
        this.type = type;
        this.id = id;
        this.weapon = weapon;
        this.displayName = displayName;
        this.color = color;
    }

    /** Model skórki: rivals:&lt;broń&gt;__&lt;skórka&gt;. */
    public String modelKey() {
        return "rivals:" + weapon.id + "__" + id;
    }

    public static Cosmetic byId(String id) {
        for (Cosmetic c : values()) if (c.id.equals(id)) return c;
        return null;
    }

    public static List<Cosmetic> of(Type type) {
        List<Cosmetic> out = new ArrayList<>();
        for (Cosmetic c : values()) if (c.type == type) out.add(c);
        return out;
    }
}
