package pl.rivals.packgen;

/** Ustalona paleta RIVALS (ARGB). Wszystkie tekstury powstają wyłącznie z tych kolorów. */
public final class Pal {

    private Pal() {
    }

    public static final int CLEAR = 0x00000000;

    public static final int INK = 0xFF0B0E14;
    public static final int DARK = 0xFF161B24;
    public static final int STEEL = 0xFF2A313D;
    public static final int GRAPHITE = 0xFF3D4655;
    public static final int MID = 0xFF5B6577;
    public static final int LIGHT = 0xFF9AA3B5;
    public static final int WHITE = 0xFFEEF2F6;

    public static final int ACCENT = 0xFF38E8F0;
    public static final int ACCENT_DARK = 0xFF1F9BA3;
    public static final int ICE = 0xFFA8ECFF;
    public static final int ICE_DARK = 0xFF5CB8E0;
    public static final int RED = 0xFFFF4D5E;
    public static final int RED_DARK = 0xFFA8273A;
    public static final int GOLD = 0xFFFFC83D;
    public static final int GOLD_LIGHT = 0xFFFFE79A;
    public static final int GOLD_DARK = 0xFFB8821C;
    public static final int ORANGE = 0xFFFF8A3D;
    public static final int GREEN = 0xFF5CE18A;
    public static final int PURPLE = 0xFFA77BFF;
    public static final int PINK = 0xFFFF8FB8;
    public static final int WOOD = 0xFF8A5A3C;

    // role w modelach: jasny korpus, drugi korpus, wykończenia, ciemne detale
    public static final int BODY = 0xFFE4E9EF;
    public static final int BODY2 = 0xFFB9C1CC;
    public static final int TRIM = 0xFF6B7584;
    public static final int CORE = 0xFF2C323C;

    /** Półprzezroczyste tło paneli UI. */
    public static final int PANEL = 0xDC0E1219;
    public static final int PANEL_EDGE = 0xF0232A36;

    public static int argb(int a, int rgb) {
        return (a & 0xFF) << 24 | (rgb & 0xFFFFFF);
    }

    /** Mieszanie liniowe dwóch kolorów (t=0 -> a, t=1 -> b). */
    public static int mix(int a, int b, double t) {
        int aa = a >>> 24, ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
        return (int) Math.round(aa + (ba - aa) * t) << 24
                | (int) Math.round(ar + (br - ar) * t) << 16
                | (int) Math.round(ag + (bg - ag) * t) << 8
                | (int) Math.round(ab + (bb - ab) * t);
    }

    public static int darker(int c, double amount) {
        return mix(c, c & 0xFF000000, amount);
    }

    public static int lighter(int c, double amount) {
        return mix(c, c | 0x00FFFFFF, amount);
    }

    public static int withAlpha(int c, int a) {
        return argb(a, c);
    }
}
