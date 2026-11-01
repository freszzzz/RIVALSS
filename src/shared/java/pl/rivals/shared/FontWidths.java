package pl.rivals.shared;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/**
 * Szerokości znaków (w pikselach GUI) - WYGENEROWANE przez packgen FontTable z czcionki gry.
 * Pozwalają policzyć szerokość tekstu po stronie serwera (dopełnianie paneli scoreboardu).
 */
public final class FontWidths {

    private FontWidths() {
    }

    /** Czcionka vanilla (ascii.png), kody 0-255. */
    private static final int[] DEFAULT = {
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            4, 2, 4, 6, 6, 6, 6, 2, 4, 4, 4, 6, 2, 6, 2, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 2, 2, 5, 6, 5, 6,
            7, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 4, 6, 6,
            3, 6, 6, 6, 6, 6, 5, 6, 6, 2, 6, 5, 3, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 4, 2, 4, 7, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6, 1, 1, 6,
            1, 1, 1, 1, 1, 1, 5, 5, 1, 1, 6, 1, 1, 1, 7, 7, 8, 9, 9, 6, 6, 6, 8, 8, 6, 8, 8, 8, 8, 8, 6, 6,
            9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 6, 9, 9, 9, 5, 9, 9,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 8, 6, 1, 7, 6, 6, 6, 8, 5, 6, 7, 5, 6, 1, 7, 5, 5, 6, 1};

    /** Pikselowa czcionka RIVALS (z cieniem w teksturze). */
    private static final Map<Character, Integer> PIXEL = new HashMap<>();

    static {
        PIXEL.put('A', 7);
        PIXEL.put('B', 7);
        PIXEL.put('C', 7);
        PIXEL.put('D', 7);
        PIXEL.put('E', 7);
        PIXEL.put('F', 7);
        PIXEL.put('G', 7);
        PIXEL.put('H', 7);
        PIXEL.put('I', 5);
        PIXEL.put('J', 7);
        PIXEL.put('K', 7);
        PIXEL.put('L', 7);
        PIXEL.put('M', 7);
        PIXEL.put('N', 7);
        PIXEL.put('O', 7);
        PIXEL.put('P', 7);
        PIXEL.put('Q', 7);
        PIXEL.put('R', 7);
        PIXEL.put('S', 7);
        PIXEL.put('T', 7);
        PIXEL.put('U', 7);
        PIXEL.put('V', 7);
        PIXEL.put('W', 7);
        PIXEL.put('X', 7);
        PIXEL.put('Y', 7);
        PIXEL.put('Z', 7);
        PIXEL.put('Ą', 7);
        PIXEL.put('Ć', 7);
        PIXEL.put('Ę', 7);
        PIXEL.put('Ł', 7);
        PIXEL.put('Ń', 7);
        PIXEL.put('Ó', 7);
        PIXEL.put('Ś', 7);
        PIXEL.put('Ź', 7);
        PIXEL.put('Ż', 7);
        PIXEL.put('0', 7);
        PIXEL.put('1', 5);
        PIXEL.put('2', 7);
        PIXEL.put('3', 7);
        PIXEL.put('4', 7);
        PIXEL.put('5', 7);
        PIXEL.put('6', 7);
        PIXEL.put('7', 7);
        PIXEL.put('8', 7);
        PIXEL.put('9', 7);
        PIXEL.put('.', 3);
        PIXEL.put(',', 3);
        PIXEL.put(':', 3);
        PIXEL.put(';', 3);
        PIXEL.put('!', 3);
        PIXEL.put('?', 7);
        PIXEL.put('-', 5);
        PIXEL.put('+', 5);
        PIXEL.put('=', 6);
        PIXEL.put('/', 7);
        PIXEL.put('|', 3);
        PIXEL.put('%', 7);
        PIXEL.put('(', 4);
        PIXEL.put(')', 4);
        PIXEL.put('[', 4);
        PIXEL.put(']', 4);
        PIXEL.put('<', 6);
        PIXEL.put('>', 6);
        PIXEL.put('\'', 3);
        PIXEL.put('"', 5);
        PIXEL.put('_', 7);
        PIXEL.put('·', 3);
        PIXEL.put('#', 7);
        PIXEL.put('×', 7);
        PIXEL.put('*', 7);
    }

    /** Szerokość znaku czcionki vanilla; znaki spoza ASCII - jak litera bazowa. */
    public static int vanilla(int cp) {
        if (cp < 256) return DEFAULT[cp];
        String base = Normalizer.normalize(new String(Character.toChars(cp)).replace('ł', 'l').replace('Ł', 'L'),
                Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        if (!base.isEmpty() && base.codePointAt(0) < 256) return DEFAULT[base.codePointAt(0)];
        return 6;
    }

    public static int vanilla(String s) {
        int w = 0;
        for (int cp : s.codePoints().toArray()) w += vanilla(cp);
        return w;
    }

    /** Szerokość napisu pikselową czcionką (wielkie litery; spacja = 4). */
    public static int pixel(String s) {
        int w = 0;
        for (char c : s.toUpperCase().toCharArray()) {
            if (c == ' ') w += 4;
            else w += PIXEL.getOrDefault(c, vanilla(c));
        }
        return w;
    }
}
