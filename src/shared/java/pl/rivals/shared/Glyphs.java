package pl.rivals.shared;

/**
 * Znaki z prywatnego zakresu Unicode używane przez resource pack (generator) i plugin.
 * Jedno źródło prawdy - generator rysuje glify dokładnie pod tymi kodami.
 */
public final class Glyphs {

    private Glyphs() {
    }

    public static final String FONT_ICONS = "rivals:icons";
    public static final String FONT_PIXEL = "rivals:pixel";
    /** Klatki dużej czcionki (odliczanie, napis zwycięstwa): rivals:big0 ... rivals:big4. */
    public static final String FONT_BIG_PREFIX = "rivals:big";
    /** Wysokości klatek (big0 największa -> big4 rozmiar spoczynkowy). */
    public static final int[] BIG_HEIGHTS = {26, 21, 17, 14, 12};

    // ikony UI 9x9
    public static final char CLOCK = '';
    public static final char RANK = '';
    public static final char TROPHY = '';
    public static final char COIN_0 = '';
    public static final char COIN_1 = '';
    public static final char COIN_2 = '';
    public static final char COIN_3 = '';
    public static final char COIN_BIG = '';
    public static final char FLAME = '';
    public static final char YOU = '';
    public static final char ENEMY = '';
    public static final char HEART = '';
    public static final char MAP = '';
    public static final char QUEUE = '';
    public static final char SEG_FULL = '';
    public static final char SEG_EMPTY = '';
    public static final char SEG_READY = '';
    public static final char CHECK = '';
    public static final char AMMO = '';
    public static final char HEADER_BAR = '';
    public static final char DOT = '';
    public static final char LOCK = '';

    public static final char[] COIN_FRAMES = {COIN_0, COIN_1, COIN_2, COIN_3};

    // panele scoreboardu (150 x 9)
    public static final char PANEL_TOP = '';
    public static final char PANEL_MID = '';
    public static final char PANEL_BOT = '';
    public static final char PANEL_ONE = '';
    public static final int PANEL_WIDTH = 150;

    // HUD
    public static final char HITMARKER = '';
    public static final char HITMARKER_HEAD = '';
    public static final char FLASH = '';

    // tła GUI (176 x 222)
    public static final char GUI_KIT = '';
    public static final char GUI_SHOP = '';
    public static final int GUI_WIDTH = 176;

    /** Ikona przedmiotu (9x9) - kolejność jak w {@link RivalsItem}. */
    public static char itemIcon(RivalsItem item) {
        return (char) (0xE400 + item.ordinal());
    }

    // odstępy (provider "space"): ujemne F801.., dodatnie F821.. o wartościach 2^i
    public static final int SPACE_STEPS = 9; // 1..256

    public static char negative(int powerIndex) {
        return (char) (0xF801 + powerIndex);
    }

    public static char positive(int powerIndex) {
        return (char) (0xF821 + powerIndex);
    }

    /** Ciąg znaków przesuwający kursor o px (ujemne = w lewo). */
    public static String space(int px) {
        if (px == 0) return "";
        StringBuilder sb = new StringBuilder();
        boolean neg = px < 0;
        int left = Math.abs(px);
        for (int i = SPACE_STEPS - 1; i >= 0; i--) {
            int v = 1 << i;
            while (left >= v) {
                sb.append(neg ? negative(i) : positive(i));
                left -= v;
            }
        }
        return sb.toString();
    }
}
