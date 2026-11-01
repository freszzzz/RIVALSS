package pl.rivals.packgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.rivals.shared.FontWidths;
import pl.rivals.shared.Glyphs;
import pl.rivals.shared.RivalsItem;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Makieta HUD (narzędzie deweloperskie): renderuje sidebar lobby i meczu, bossbar, odliczanie, actionbar
 * i okno zestawu z czcionek packa i czcionki vanilla - tymi samymi zasadami pozycjonowania co klient.
 * Użycie: HudMock &lt;katalog packa&gt; &lt;katalog vanilla&gt; &lt;plik wyjściowy&gt;
 */
public final class HudMock {

    private record Glyph(BufferedImage img, float scale, int ascent, int advance, boolean space) {
    }

    /** Fragment tekstu: treść, czcionka, kolor RGB, cień. */
    record Seg(String text, String font, int rgb, boolean shadow) {
    }

    private final File pack;
    private final File vanilla;
    private final Map<String, Map<Integer, Glyph>> fonts = new HashMap<>();

    HudMock(File pack, File vanilla) {
        this.pack = pack;
        this.vanilla = vanilla;
    }

    public static void main(String[] args) throws Exception {
        HudMock m = new HudMock(new File(args[0]), new File(args[1]));
        m.render(new File(args[2]));
        System.out.println("Makieta HUD: " + args[2]);
    }

    // czcionki
    private Map<Integer, Glyph> font(String id) {
        Map<Integer, Glyph> cached = fonts.get(id);
        if (cached != null) return cached;
        String k = id;
        Map<Integer, Glyph> result = ((Supplier<Map<Integer, Glyph>>) () -> {
            Map<Integer, Glyph> map = new HashMap<>();
            try {
                if (k.equals("minecraft:default")) {
                    loadBitmap(map, ImageIO.read(new File(vanilla, "assets/minecraft/textures/font/ascii.png")), null, 8, 7);
                    return map;
                }
                String path = k.substring(k.indexOf(':') + 1);
                File fontFile = new File(pack, "assets/rivals/font/" + path + ".json");
                JsonObject json = JsonParser.parseString(Files.readString(fontFile.toPath())).getAsJsonObject();
                for (JsonElement pe : json.getAsJsonArray("providers")) {
                    JsonObject p = pe.getAsJsonObject();
                    switch (p.get("type").getAsString()) {
                        case "space" -> {
                            for (var e : p.getAsJsonObject("advances").entrySet()) {
                                map.putIfAbsent(e.getKey().codePointAt(0), new Glyph(null, 1, 0, e.getValue().getAsInt(), true));
                            }
                        }
                        case "bitmap" -> {
                            String file = p.get("file").getAsString();
                            String fpath = file.substring(file.indexOf(':') + 1);
                            BufferedImage img = ImageIO.read(new File(pack, "assets/rivals/textures/" + fpath));
                            List<String> rows = new ArrayList<>();
                            for (JsonElement r : p.getAsJsonArray("chars")) rows.add(r.getAsString());
                            loadBitmap(map, img, rows, p.get("height").getAsInt(), p.get("ascent").getAsInt());
                        }
                        case "reference" -> {
                            for (var e : font("minecraft:default").entrySet()) map.putIfAbsent(e.getKey(), e.getValue());
                        }
                        default -> {
                        }
                    }
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return map;
        }).get();
        fonts.put(id, result);
        return result;
    }

    /** rows == null: siatka 16x16 kodów 0..255 (ascii.png). */
    private static void loadBitmap(Map<Integer, Glyph> map, BufferedImage img, List<String> rows, int height, int ascent) {
        int cols = rows == null ? 16 : rows.get(0).codePointCount(0, rows.get(0).length());
        int rowCount = rows == null ? 16 : rows.size();
        int cw = img.getWidth() / cols, ch = img.getHeight() / rowCount;
        float scale = height / (float) ch;
        for (int r = 0; r < rowCount; r++) {
            int[] cps = rows == null ? null : rows.get(r).codePoints().toArray();
            for (int c = 0; c < cols; c++) {
                int cp = rows == null ? r * 16 + c : cps[c];
                if (cp == 0 || map.containsKey(cp)) continue;
                BufferedImage cell = img.getSubimage(c * cw, r * ch, cw, ch);
                int w = 0;
                for (int x = cw - 1; x >= 0 && w == 0; x--)
                    for (int y = 0; y < ch; y++)
                        if ((cell.getRGB(x, y) >>> 24) != 0) {
                            w = x + 1;
                            break;
                        }
                if (cp == ' ') w = 3;
                map.put(cp, new Glyph(cell, scale, ascent, (int) (0.5f + w * scale) + 1, false));
            }
        }
    }

    private static String fold(String s) {
        return Normalizer.normalize(s.replace('ł', 'l').replace('Ł', 'L'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private Glyph glyph(String font, int cp) {
        Glyph g = font(font).get(cp);
        if (g == null && font.equals("minecraft:default")) {
            String f = fold(new String(Character.toChars(cp)));
            if (!f.isEmpty()) g = font(font).get(f.codePointAt(0));
        }
        return g;
    }

    int width(List<Seg> segs) {
        int w = 0;
        for (Seg s : segs) for (int cp : s.text.codePoints().toArray()) {
            Glyph g = glyph(s.font, cp);
            if (g != null) w += g.advance;
        }
        return w;
    }

    /** Rysuje tekst (x,y = lewy górny róg linii, jednostki GUI) w skali sc. */
    int draw(BufferedImage out, List<Seg> segs, int x, int y, int sc) {
        int cx = x;
        for (Seg s : segs) {
            for (int cp : s.text.codePoints().toArray()) {
                Glyph g = glyph(s.font, cp);
                if (g == null) continue;
                if (!g.space && g.img != null) {
                    int top = y + 7 - g.ascent;
                    if (s.shadow) blit(out, g, cx + 1, top + 1, sc, darken(s.rgb));
                    blit(out, g, cx, top, sc, s.rgb);
                }
                cx += g.advance;
            }
        }
        return cx;
    }

    private static int darken(int rgb) {
        return ((rgb >> 16 & 0xFF) / 4) << 16 | ((rgb >> 8 & 0xFF) / 4) << 8 | (rgb & 0xFF) / 4;
    }

    private static void blit(BufferedImage out, Glyph g, int gx, int gy, int sc, int rgb) {
        BufferedImage img = g.img;
        float s = g.scale;
        int w = Math.round(img.getWidth() * s), h = Math.round(img.getHeight() * s);
        for (int j = 0; j < h * sc; j++) {
            for (int i = 0; i < w * sc; i++) {
                int sx = Math.min(img.getWidth() - 1, (int) (i / (float) sc / s));
                int sy = Math.min(img.getHeight() - 1, (int) (j / (float) sc / s));
                int c = img.getRGB(sx, sy);
                int a = c >>> 24;
                if (a == 0) continue;
                int r = (c >> 16 & 0xFF) * (rgb >> 16 & 0xFF) / 255;
                int gg = (c >> 8 & 0xFF) * (rgb >> 8 & 0xFF) / 255;
                int b = (c & 0xFF) * (rgb & 0xFF) / 255;
                int px = gx * sc + i, py = gy * sc + j;
                if (px < 0 || py < 0 || px >= out.getWidth() || py >= out.getHeight()) continue;
                int d = out.getRGB(px, py);
                float k = a / 255f;
                out.setRGB(px, py, 0xFF000000
                        | (int) (r * k + (d >> 16 & 0xFF) * (1 - k)) << 16
                        | (int) (gg * k + (d >> 8 & 0xFF) * (1 - k)) << 8
                        | (int) (b * k + (d & 0xFF) * (1 - k)));
            }
        }
    }

    private static void fillA(BufferedImage out, int x0, int y0, int x1, int y1, int argb, int sc) {
        float k = (argb >>> 24) / 255f;
        for (int y = y0 * sc; y < y1 * sc; y++)
            for (int x = x0 * sc; x < x1 * sc; x++) {
                if (x < 0 || y < 0 || x >= out.getWidth() || y >= out.getHeight()) continue;
                int d = out.getRGB(x, y);
                out.setRGB(x, y, 0xFF000000
                        | (int) ((argb >> 16 & 0xFF) * k + (d >> 16 & 0xFF) * (1 - k)) << 16
                        | (int) ((argb >> 8 & 0xFF) * k + (d >> 8 & 0xFF) * (1 - k)) << 8
                        | (int) ((argb & 0xFF) * k + (d & 0xFF) * (1 - k)));
            }
    }

    // składniki (jak w HudManager)
    static final String D = "minecraft:default", I = Glyphs.FONT_ICONS, P = Glyphs.FONT_PIXEL;
    static final int TEXT = 0xEEF2F6, MUTED = 0x8A93A6, ACCENT = 0x38E8F0, RED = 0xFF4D5E, GOLD = 0xFFC83D, WHITE = 0xFFFFFF;

    static Seg g(char c) {
        return new Seg(String.valueOf(c), I, WHITE, false);
    }

    static Seg sp(int px) {
        return new Seg(Glyphs.space(px), I, WHITE, false);
    }

    static Seg t(String s, int rgb) {
        return new Seg(s, D, rgb, false);
    }

    static Seg px(String s, int rgb) {
        return new Seg(s.toUpperCase(), P, rgb, false);
    }

    static List<Seg> L(Seg... s) {
        return new ArrayList<>(List.of(s));
    }

    /** Szacowana szerokość - identycznie jak Hud.Row w pluginie (ikony 8, kreska nagłówka 5). */
    static int est(List<Seg> segs) {
        int w = 0;
        for (Seg s : segs) {
            if (s.font.equals(D)) w += FontWidths.vanilla(s.text);
            else if (s.font.equals(P)) w += FontWidths.pixel(s.text);
            else if (s.text.length() == 1 && s.text.charAt(0) == Glyphs.HEADER_BAR) w += 5;
            else if (s.text.length() == 1 && s.text.charAt(0) < 0xF800) w += 8;
            else {
                for (char c : s.text.toCharArray()) {
                    if (c >= 0xF801 && c <= 0xF809) w -= 1 << (c - 0xF801);
                    else if (c >= 0xF821 && c <= 0xF829) w += 1 << (c - 0xF821);
                }
            }
        }
        return w;
    }

    static final int INNER = Glyphs.PANEL_WIDTH - 4;

    static List<Seg> panel(char p, List<Seg> content) {
        List<Seg> l = L(g(p), sp(-(Glyphs.PANEL_WIDTH + 1) + 4));
        l.addAll(content);
        l.add(sp(Math.max(0, INNER - est(content))));
        return l;
    }

    static List<Seg> header(String name) {
        return L(g(Glyphs.HEADER_BAR), sp(2), px(name, ACCENT));
    }

    static List<Seg> row(char p, char icon, String label, String value, int vc) {
        List<Seg> c = L(g(icon), sp(2), t(label, TEXT));
        int vw = FontWidths.vanilla(value);
        c.add(sp(Math.max(4, INNER - 4 - est(c) - vw)));
        c.add(t(value, vc));
        return panel(p, c);
    }

    static List<Seg> title() {
        return L(px("RIV", ACCENT), px("A", 0xE6FFFF), px("LS", ACCENT));
    }

    static List<List<Seg>> lobby() {
        List<List<Seg>> l = new ArrayList<>();
        l.add(L(g(Glyphs.CLOCK), sp(2), t("21/10/2026", TEXT), t(" | ", MUTED), t("00:22", TEXT)));
        l.add(L(t(" ", TEXT)));
        l.add(panel(Glyphs.PANEL_TOP, header("PROFIL")));
        l.add(row(Glyphs.PANEL_MID, Glyphs.RANK, "Ranga", "Złoto II", GOLD));
        l.add(row(Glyphs.PANEL_MID, Glyphs.TROPHY, "ELO", "1240", TEXT));
        l.add(row(Glyphs.PANEL_MID, Glyphs.COIN_0, "Monety", "350", GOLD));
        l.add(row(Glyphs.PANEL_BOT, Glyphs.FLAME, "Seria", "3", TEXT));
        l.add(L(t(" ", TEXT)));
        l.add(panel(Glyphs.PANEL_TOP, header("KOLEJKA")));
        l.add(panel(Glyphs.PANEL_MID, L(t("Wpisz ", TEXT), t("/rivals graj", ACCENT))));
        l.add(panel(Glyphs.PANEL_BOT, L(t("lub podejdź do NPC", TEXT))));
        l.add(L(t(" ", TEXT)));
        l.add(L(px("play.rivals.pl", ACCENT)));
        return l;
    }

    static List<List<Seg>> match() {
        List<List<Seg>> l = new ArrayList<>();
        l.add(L(g(Glyphs.CLOCK), sp(2), t("Runda 4", TEXT), t(" | ", MUTED), t("0:47", TEXT)));
        l.add(L(t(" ", TEXT)));
        l.add(panel(Glyphs.PANEL_TOP, header("WYNIK")));
        List<Seg> sc0 = L(g(Glyphs.YOU), sp(2), t("Ty ", TEXT), t("3", ACCENT), t(" : ", MUTED), t("2", RED));
        sc0.add(sp(Math.max(4, INNER - 4 - est(sc0) - (FontWidths.vanilla("Notch") + 10))));
        sc0.add(g(Glyphs.ENEMY)); sc0.add(sp(2)); sc0.add(t("Notch", RED));
        l.add(panel(Glyphs.PANEL_MID, sc0));
        l.add(row(Glyphs.PANEL_BOT, Glyphs.HEART, "HP przeciwnika", "14", RED));
        l.add(L(t(" ", TEXT)));
        l.add(panel(Glyphs.PANEL_TOP, header("ZESTAW")));
        List<Seg> gd = L(g(Glyphs.itemIcon(RivalsItem.CRYO)), sp(2), t("Kriogranat", TEXT));
        gd.add(sp(Math.max(4, INNER - 4 - est(gd) - 30)));
        gd.addAll(L(g(Glyphs.SEG_FULL), g(Glyphs.SEG_FULL), g(Glyphs.SEG_FULL), g(Glyphs.SEG_EMPTY), g(Glyphs.SEG_EMPTY)));
        l.add(panel(Glyphs.PANEL_BOT, gd));
        l.add(L(t(" ", TEXT)));
        l.add(panel(Glyphs.PANEL_TOP, header("ARENA")));
        l.add(row(Glyphs.PANEL_MID, Glyphs.MAP, "Laboratorium", "", TEXT));
        l.add(row(Glyphs.PANEL_BOT, Glyphs.COIN_0, "Zdobyte", "+18", GOLD));
        l.add(L(t(" ", TEXT)));
        l.add(L(px("play.rivals.pl", ACCENT)));
        return l;
    }

    // składanie ekranu
    /** Sidebar jak w Gui.displayScoreboardSidebar (bez numerów, tła wierszy 30%/40% czerni). */
    private void sidebar(BufferedImage out, int screenW, int screenH, List<Seg> title, List<List<Seg>> lines, int sc) {
        int maxW = width(title);
        for (List<Seg> l : lines) maxW = Math.max(maxW, width(l));
        int total = lines.size() * 9;
        int bottom = screenH / 2 + total / 3;
        int x0 = screenW - maxW - 3;
        int x1 = screenW - 3 + 2;
        int top = bottom - total;
        fillA(out, x0 - 2, top - 9 - 1, x1, top - 1, 0x66000000, sc);
        fillA(out, x0 - 2, top - 1, x1, top, 0x4D000000, sc);
        draw(out, title, x0 + maxW / 2 - width(title) / 2, top - 9, sc);
        for (int i = 0; i < lines.size(); i++) {
            int y = top + i * 9;
            fillA(out, x0 - 2, y, x1, y + 9, 0x4D000000, sc);
            draw(out, lines.get(i), x0, y, sc);
        }
    }

    void render(File outFile) throws IOException {
        int sc = 3, w = 480, h = 270;
        BufferedImage out = new BufferedImage(w * 2 * sc, h * sc, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(0x6E8FB3), 0, h * sc, new Color(0x3C5A2E)));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setColor(new Color(0x7A7F87));
        g.fillRect(w * sc, h * sc * 3 / 5, w * sc, h * sc);
        g.dispose();

        // lewy ekran: lobby
        BufferedImage left = out.getSubimage(0, 0, w * sc, h * sc);
        sidebar(left, w, h, title(), lobby(), sc);
        draw(left, L(new Seg("Lobby – sidebar", D, WHITE, true)), 4, 4, sc);

        // prawy ekran: mecz
        BufferedImage right = out.getSubimage(w * sc, 0, w * sc, h * sc);
        sidebar(right, w, h, title(), match(), sc);
        // bossbar
        BufferedImage bg = ImageIO.read(new File(pack, "assets/minecraft/textures/gui/sprites/boss_bar/blue_background.png"));
        BufferedImage fg = ImageIO.read(new File(pack, "assets/minecraft/textures/gui/sprites/boss_bar/blue_progress.png"));
        int bx = w / 2 - 91, by = 12;
        Graphics2D rg = right.createGraphics();
        rg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        rg.drawImage(bg, bx * sc, by * sc, 182 * sc, 5 * sc, null);
        rg.drawImage(fg.getSubimage(0, 0, 120, 5), bx * sc, by * sc, 120 * sc, 5 * sc, null);
        rg.dispose();
        List<Seg> bossName = L(px("Steve ", TEXT), px("3:2", ACCENT), px(" Notch  ", TEXT),
                px("·", MUTED), px("  RUNDA 4  ", TEXT), px("·", MUTED), px("  0:47", TEXT));
        draw(right, bossName.stream().map(s -> new Seg(s.text, s.font, s.rgb, true)).toList(), w / 2 - width(bossName) / 2, by - 9, sc);
        // odliczanie (tytuł w skali 4): duża czcionka, klatka spoczynkowa
        List<Seg> num = L(new Seg("3", "rivals:big4", ACCENT, false));
        BufferedImage big = new BufferedImage(w * sc, h * sc, BufferedImage.TYPE_INT_ARGB);
        int tsc = sc * 4;
        int tw = width(num);
        draw(big, num, (w / 2) / 4 - tw / 2, (h / 2) / 4 - 10, tsc);
        Graphics2D bg2 = right.createGraphics();
        bg2.drawImage(big, 0, 0, null);
        bg2.dispose();
        // actionbar
        List<Seg> ab = new ArrayList<>();
        RivalsItem[] kit = {RivalsItem.RIFLE, RivalsItem.PISTOL, RivalsItem.SWORD, RivalsItem.CRYO};
        String[] st = {"8/8", "12/12", "", "3.4s"};
        for (int i = 0; i < 4; i++) {
            if (i > 0) ab.add(sp(10));
            ab.add(g(Glyphs.itemIcon(kit[i])));
            ab.add(sp(2));
            if (st[i].isEmpty()) ab.add(g(Glyphs.DOT));
            else ab.add(new Seg(st[i], D, i == 0 ? TEXT : MUTED, true));
        }
        draw(right, ab, w / 2 - width(ab) / 2, h - 59, sc);
        // hitmarker w podtytule (skala 2): y = 5 w jednostkach podtytułu
        BufferedImage sub = new BufferedImage(w * sc, h * sc, BufferedImage.TYPE_INT_ARGB);
        List<Seg> hm = L(g(Glyphs.HITMARKER));
        draw(sub, hm, (w / 2) / 2 - width(hm) / 2 + 30, (h / 2) / 2 + 5, sc * 2);
        Graphics2D sg = right.createGraphics();
        sg.drawImage(sub, 0, 0, null);
        sg.setColor(new Color(255, 255, 255, 120));
        sg.drawLine(w / 2 * sc - 6 + 60 * 3, h / 2 * sc, w / 2 * sc + 6 + 60 * 3, h / 2 * sc);
        sg.drawLine(w / 2 * sc + 60 * 3, h / 2 * sc - 6, w / 2 * sc + 60 * 3, h / 2 * sc + 6);
        sg.dispose();
        String caption = "Mecz – sidebar, bossbar, odliczanie, actionbar, hitmarker (z prawej: znacznik środka)";
        draw(right, L(new Seg(caption, D, WHITE, true)), 4, h - 12, sc);

        ImageIO.write(out, "png", outFile);

        // okno zestawu: tło GUI + tytuł
        BufferedImage gui = new BufferedImage(200 * sc, 240 * sc, BufferedImage.TYPE_INT_RGB);
        Graphics2D gg = gui.createGraphics();
        gg.setColor(new Color(0x3A3F48));
        gg.fillRect(0, 0, gui.getWidth(), gui.getHeight());
        gg.dispose();
        List<Seg> guiTitle = L(sp(-8), g(Glyphs.GUI_KIT), sp(-(Glyphs.GUI_WIDTH + 1) + 8), g(Glyphs.HEADER_BAR), sp(2), px("WYBÓR ZESTAWU", TEXT));
        draw(gui, guiTitle, 12 + 8, 8 + 6, sc);
        ImageIO.write(gui, "png", new File(outFile.getParentFile(), "gui_kit.png"));
    }
}
