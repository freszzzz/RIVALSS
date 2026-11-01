package pl.rivals.packgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pl.rivals.shared.Glyphs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Własna pikselowa czcionka (~7 px wysokości wielkich liter) z cieniem wpisanym w teksturę.
 * Komórka 7x10: wiersz 0 = akcent, 1-7 = litera, 8 = ogonek, 9 = cień.
 * Ta sama tekstura służy też dużym klatkom (rivals:big0..4) - inna wysokość = skalowanie.
 */
public final class PixelFont {

    static final int CELL_W = 7;
    static final int CELL_H = 10;
    static final int COLS = 16;
    static final int SHADOW = 0xFF3B4250;

    /** Glify: 9 wierszy (0 akcent, 1-7 litera, 8 ogonek). Krótsze tablice = wiersze 1-7. */
    static final Map<Character, String[]> GLYPHS = new LinkedHashMap<>();

    private static void g(char c, String... rows) {
        GLYPHS.put(c, rows);
    }

    static {
        g('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
        g('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
        g('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        g('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
        g('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
        g('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####");
        g('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('I', "###", ".#.", ".#.", ".#.", ".#.", ".#.", "###");
        g('J', "..###", "...#.", "...#.", "...#.", "#..#.", "#..#.", ".##..");
        g('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
        g('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        g('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#");
        g('N', "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#");
        g('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
        g('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
        g('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
        g('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
        g('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        g('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
        g('W', "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#");
        g('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        g('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
        g('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");

        // polskie - litery z akcentem mają 6 wierszy, żeby akcent nie dotykał litery
        g('Ą', ".....", ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#", "....#");
        g('Ć', "...#.", ".....", ".###.", "#...#", "#....", "#....", "#...#", ".###.");
        g('Ę', ".....", "#####", "#....", "#....", "####.", "#....", "#....", "#####", "...#.");
        g('Ł', ".....", "#....", "#....", "#.#..", "##...", "#....", "#....", "#####");
        g('Ń', "...#.", ".....", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#");
        g('Ó', "...#.", ".....", ".###.", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('Ś', "...#.", ".....", ".####", "#....", ".###.", "....#", "....#", "####.");
        g('Ź', "...#.", ".....", "#####", "...#.", "..#..", ".#...", "#....", "#####");
        g('Ż', "..#..", ".....", "#####", "...#.", "..#..", ".#...", "#....", "#####");

        g('0', ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.");
        g('1', ".#.", "##.", ".#.", ".#.", ".#.", ".#.", "###");
        g('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
        g('3', "####.", "....#", "....#", ".###.", "....#", "....#", "####.");
        g('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.");
        g('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
        g('6', ".###.", "#....", "#....", "####.", "#...#", "#...#", ".###.");
        g('7', "#####", "....#", "...#.", "..#..", "..#..", "..#..", "..#..");
        g('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
        g('9', ".###.", "#...#", "#...#", ".####", "....#", "....#", ".###.");

        g('.', ".", ".", ".", ".", ".", ".", "#");
        g(',', ".", ".", ".", ".", ".", ".", "#", "#");
        g(':', ".", ".", "#", ".", ".", ".", "#");
        g(';', ".", ".", "#", ".", ".", ".", "#", "#");
        g('!', "#", "#", "#", "#", "#", ".", "#");
        g('?', ".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#..");
        g('-', "...", "...", "...", "###", "...", "...", "...");
        g('+', "...", "...", ".#.", "###", ".#.", "...", "...");
        g('=', "....", "....", "####", "....", "####", "....", "....");
        g('/', "....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#....");
        g('|', "#", "#", "#", "#", "#", "#", "#");
        g('%', "##..#", "##.#.", "...#.", "..#..", ".#...", ".#.##", "#..##");
        g('(', ".#", "#.", "#.", "#.", "#.", "#.", ".#");
        g(')', "#.", ".#", ".#", ".#", ".#", ".#", "#.");
        g('[', "##", "#.", "#.", "#.", "#.", "#.", "##");
        g(']', "##", ".#", ".#", ".#", ".#", ".#", "##");
        g('<', "...#", "..#.", ".#..", "#...", ".#..", "..#.", "...#");
        g('>', "#...", ".#..", "..#.", "...#", "..#.", ".#..", "#...");
        g('\'', "#", "#", ".", ".", ".", ".", ".");
        g('"', "#.#", "#.#", "...", "...", "...", "...", "...");
        g('_', ".....", ".....", ".....", ".....", ".....", ".....", "#####");
        g('·', ".", ".", ".", "#", ".", ".", ".");
        g('#', ".#.#.", "#####", ".#.#.", ".#.#.", ".#.#.", "#####", ".#.#.");
        g('×', ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", ".....");
        g('*', ".....", "#.#.#", ".###.", "#####", ".###.", "#.#.#", ".....");
    }

    private static String[] rows9(String[] src) {
        String[] out = new String[9];
        if (src.length >= 8) {
            for (int i = 0; i < 9; i++) out[i] = i < src.length ? src[i] : "";
        } else {
            out[0] = "";
            for (int i = 0; i < 7; i++) out[i + 1] = i < src.length ? src[i] : "";
            out[8] = "";
        }
        return out;
    }

    /** Rysuje glif w komórce (x,y) z cieniem. */
    static void drawGlyph(Img img, int x, int y, char c, int color, boolean shadow) {
        String[] rows = GLYPHS.get(Character.toUpperCase(c));
        if (rows == null) return;
        String[] r9 = rows9(rows);
        if (shadow) {
            for (int j = 0; j < 9; j++)
                for (int i = 0; i < r9[j].length(); i++)
                    if (r9[j].charAt(i) == '#') img.set(x + i + 1, y + j + 1, SHADOW);
        }
        for (int j = 0; j < 9; j++)
            for (int i = 0; i < r9[j].length(); i++)
                if (r9[j].charAt(i) == '#') img.set(x + i, y + j, color);
    }

    static int glyphWidth(char c) {
        if (c == ' ') return 3;
        String[] rows = GLYPHS.get(Character.toUpperCase(c));
        if (rows == null) return 0;
        int w = 0;
        for (String r : rows) w = Math.max(w, r.length());
        return w;
    }

    /** Szerokość napisu w pikselach tekstury (z odstępem 1 px między znakami). */
    static int width(String text) {
        int w = 0;
        for (char c : text.toCharArray()) w += glyphWidth(c) + (c == ' ' ? 1 : 2);
        return Math.max(0, w - 1);
    }

    /** Rysuje napis na obrazku (do tekstur GUI). y = górna krawędź komórki. */
    static void drawText(Img img, int x, int y, String text, int color) {
        int cx = x;
        for (char c : text.toCharArray()) {
            if (c == ' ') {
                cx += 4;
                continue;
            }
            drawGlyph(img, cx, y, c, color, true);
            cx += glyphWidth(c) + 2;
        }
    }

    // zapis
    static void generate(Out out) {
        StringBuilder all = new StringBuilder();
        for (char c : GLYPHS.keySet()) all.append(c);
        String chars = all.toString();
        int rowsCount = (chars.length() + COLS - 1) / COLS;

        Img tex = new Img(COLS * CELL_W, rowsCount * CELL_H);
        for (int i = 0; i < chars.length(); i++) {
            drawGlyph(tex, (i % COLS) * CELL_W, (i / COLS) * CELL_H, chars.charAt(i), Pal.WHITE | 0xFF000000, true);
        }
        tex.save(out.asset("rivals", "textures/font/pixel.png"));

        JsonArray upper = gridRows(chars, rowsCount, false);
        JsonArray lower = gridRows(chars, rowsCount, true);

        // rivals:pixel - rozmiar normalny
        out.json(out.asset("rivals", "font/pixel.json"), fontJson(upper, lower, CELL_H, 8, 4));

        // rivals:big0..4 - klatki powiększenia (środek glifu stały: ascent = h/2 + 2)
        for (int i = 0; i < Glyphs.BIG_HEIGHTS.length; i++) {
            int h = Glyphs.BIG_HEIGHTS[i];
            int ascent = Math.min(h, h / 2 + 2);
            int space = Math.max(2, Math.round(4f * h / CELL_H));
            out.json(out.asset("rivals", "font/big" + i + ".json"), fontJson(upper, lower, h, ascent, space));
        }
    }

    private static JsonObject fontJson(JsonArray upper, JsonArray lower, int height, int ascent, int space) {
        JsonArray providers = new JsonArray();
        providers.add(Out.obj("type", "space", "advances", Out.obj(" ", space, " ", space)));
        providers.add(Out.obj("type", "bitmap", "file", "rivals:font/pixel.png", "height", height, "ascent", ascent, "chars", upper));
        providers.add(Out.obj("type", "bitmap", "file", "rivals:font/pixel.png", "height", height, "ascent", ascent, "chars", lower));
        // znaki spoza zestawu (np. w nickach) - czcionka vanilla
        providers.add(Out.obj("type", "reference", "id", "minecraft:default"));
        return Out.obj("providers", providers);
    }

    private static JsonArray gridRows(String chars, int rowsCount, boolean lower) {
        JsonArray rows = new JsonArray();
        for (int r = 0; r < rowsCount; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < COLS; c++) {
                int idx = r * COLS + c;
                char ch = idx < chars.length() ? chars.charAt(idx) : '\u0000';
                if (lower && ch != '\u0000') {
                    char lc = Character.toLowerCase(ch);
                    // znak bez małej wersji (cyfry, interpunkcja) - w drugim providerze pusty slot
                    ch = lc != ch ? lc : '\u0000';
                }
                sb.append(ch);
            }
            rows.add(sb.toString());
        }
        return rows;
    }
}
