package pl.rivals.packgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pl.rivals.shared.Glyphs;
import pl.rivals.shared.RivalsItem;

import java.util.HashMap;
import java.util.Map;

import static pl.rivals.packgen.Out.arr;
import static pl.rivals.packgen.Out.obj;
import static pl.rivals.packgen.Pal.*;

/** Ikony UI, panele, tła GUI, bossbar, hitmarker, błysk i animowane tekstury. */
public final class UiAssets {

    private UiAssets() {
    }

    static final Map<Character, Integer> C = new HashMap<>();

    static {
        C.put('k', INK); C.put('d', DARK); C.put('s', STEEL); C.put('g', GRAPHITE); C.put('m', MID);
        C.put('l', LIGHT); C.put('w', WHITE); C.put('a', ACCENT); C.put('A', ACCENT_DARK); C.put('i', ICE);
        C.put('I', ICE_DARK); C.put('r', RED); C.put('R', RED_DARK); C.put('y', GOLD); C.put('Y', GOLD_LIGHT);
        C.put('G', GOLD_DARK); C.put('o', ORANGE); C.put('e', GREEN); C.put('p', PURPLE);
    }

    // ikony UI 9x9
    static final Map<Character, String[]> UI_ICONS = new HashMap<>();

    static {
        UI_ICONS.put(Glyphs.CLOCK, new String[]{
                "..kkkkk..", ".kwwwwwk.", "kwwwkwwwk", "kwwwkwwwk", "kwwwkkkwk",
                "kwwwwwwwk", "kwwwwwwwk", ".kwwwwwk.", "..kkkkk.."});
        UI_ICONS.put(Glyphs.RANK, new String[]{
                ".kkkkkkk.", "kyyyyyyyk", "kyYYyYYyk", "kyyYYYyyk", "kyyyYyyyk",
                ".kyyyyyk.", "..kyyyk..", "...kyk...", "....k...."});
        UI_ICONS.put(Glyphs.TROPHY, new String[]{
                "kkkkkkkkk", "kyYyyyyyk", "kyYyyyyyk", ".kyyyyyk.", "..kyyyk..",
                "...kyk...", "...kyk...", "..kGGGk..", "..kkkkk.."});
        UI_ICONS.put(Glyphs.COIN_0, new String[]{
                "..kkkkk..", ".kYYYYYk.", "kYyyyyyGk", "kYyyGyyGk", "kYyGGGyGk",
                "kYyyGyyGk", "kYyyyyyGk", ".kGGGGGk.", "..kkkkk.."});
        UI_ICONS.put(Glyphs.COIN_1, new String[]{
                "...kkk...", "..kYyGk..", "..kYyGk..", "..kYGGk..", "..kYGGk..",
                "..kYGGk..", "..kYyGk..", "..kYyGk..", "...kkk..."});
        UI_ICONS.put(Glyphs.COIN_2, new String[]{
                "...kkk...", "...kYk...", "...kYk...", "...kGk...", "...kGk...",
                "...kGk...", "...kYk...", "...kGk...", "...kkk..."});
        UI_ICONS.put(Glyphs.COIN_3, new String[]{
                "...kkk...", "..kGyYk..", "..kGyYk..", "..kGGYk..", "..kGGYk..",
                "..kGGYk..", "..kGyYk..", "..kGyYk..", "...kkk..."});
        UI_ICONS.put(Glyphs.FLAME, new String[]{
                "....k....", "...kok...", "...kook..", "..kooyok.", ".kooyyok.",
                ".koyyYok.", ".koyYYok.", "..kooook.", "...kkkk.."});
        UI_ICONS.put(Glyphs.YOU, new String[]{
                "...kkk...", "..kaaak..", "..kaaak..", "...kkk...", "..kaaak..",
                ".kaaaaak.", ".kaaaaak.", ".kaaaaak.", ".kkkkkkk."});
        UI_ICONS.put(Glyphs.ENEMY, new String[]{
                "...kkk...", "..krrrk..", "..krrrk..", "...kkk...", "..krrrk..",
                ".krrrrrk.", ".krrrrrk.", ".krrrrrk.", ".kkkkkkk."});
        UI_ICONS.put(Glyphs.HEART, new String[]{
                ".kk...kk.", "kwrk.krrk", "krrrkrrrk", "krrrrrrrk", ".krrrrrk.",
                "..krrrk..", "...krk...", "....k....", "........."});
        UI_ICONS.put(Glyphs.MAP, new String[]{
                "..kkkkk..", ".kaaaaak.", "kaawwwaak", "kaawkwaak", "kaawwwaak",
                ".kaaaaak.", "..kaaak..", "...kak...", "....k...."});
        UI_ICONS.put(Glyphs.QUEUE, new String[]{
                "kkkkkkkkk", ".kwwwwwk.", "..kwwwk..", "...kak...", "....k....",
                "...kak...", "..kaaak..", ".kaaaaak.", "kkkkkkkkk"});
        UI_ICONS.put(Glyphs.CHECK, new String[]{
                ".........", ".......kk", "......kek", "k....kek.", "kek.kek..",
                ".kekek...", "..kek....", "...k.....", "........."});
        UI_ICONS.put(Glyphs.AMMO, new String[]{
                "...kkk...", "..kyyyk..", "..kyYyk..", "..kyYyk..", "..kyYyk..",
                "..kkkkk..", "..kGGGk..", "..kGGGk..", "..kkkkk.."});
        UI_ICONS.put(Glyphs.DOT, new String[]{
                ".........", ".........", ".........", "...kkk...", "..kwwwk..",
                "...kkk...", ".........", ".........", "........."});
        UI_ICONS.put(Glyphs.LOCK, new String[]{
                "..kkkkk..", ".kmk.kmk.", ".kmk.kmk.", "kkkkkkkkk", "kyyyyyyyk",
                "kyyykyyyk", "kyyykyyyk", "kyyyyyyyk", "kkkkkkkkk"});
    }

    static final String[] SEG_FULL = {"kkkkk", "kaaak", "kaaak", "kaaak", "kaaak", "kaaak", "kkkkk"};
    static final String[] SEG_EMPTY = {"kkkkk", "kgggk", "kgggk", "kgggk", "kgggk", "kgggk", "kkkkk"};
    static final String[] SEG_READY = {"kkkkk", "kawak", "kawak", "kawak", "kawak", "kawak", "kkkkk"};

    static final String[] COIN_BIG = {
            "...kkkkk...", "..kYYYYYk..", ".kYyyyyyyGk.".substring(0, 11), "kYyyyGyyyGk", "kYyyGGGyyGk",
            "kYyyyGyyyGk", "kYyyyGyyyGk", "kYyyyyyyyGk", ".kGyyyyyGk.", "..kGGGGGk..", "...kkkkk..."};

    // generowanie
    static void generate(Out out, Map<RivalsItem, Img> itemIcons) {
        JsonArray providers = new JsonArray();

        // odstępy
        JsonObject adv = new JsonObject();
        for (int i = 0; i < Glyphs.SPACE_STEPS; i++) {
            adv.addProperty(String.valueOf(Glyphs.negative(i)), -(1 << i));
            adv.addProperty(String.valueOf(Glyphs.positive(i)), 1 << i);
        }
        providers.add(obj("type", "space", "advances", adv));

        // ikony UI
        for (Map.Entry<Character, String[]> e : UI_ICONS.entrySet()) {
            String name = "ui_" + Integer.toHexString(e.getKey());
            Img img = new Img(9, 9).pattern(0, 0, e.getValue(), C);
            providers.add(glyph(out, name, img, e.getKey(), 9, 8));
        }
        providers.add(glyph(out, "seg_full", new Img(5, 7).pattern(0, 0, SEG_FULL, C), Glyphs.SEG_FULL, 7, 7));
        providers.add(glyph(out, "seg_empty", new Img(5, 7).pattern(0, 0, SEG_EMPTY, C), Glyphs.SEG_EMPTY, 7, 7));
        providers.add(glyph(out, "seg_ready", new Img(5, 7).pattern(0, 0, SEG_READY, C), Glyphs.SEG_READY, 7, 7));
        providers.add(glyph(out, "coin_big", new Img(11, 11).pattern(0, 0, COIN_BIG, C), Glyphs.COIN_BIG, 11, 9));
        Img bar = new Img(7, 9).fill(0, 4, 5, 1, ACCENT).fill(0, 5, 5, 1, ACCENT_DARK);
        providers.add(glyph(out, "header_bar", bar, Glyphs.HEADER_BAR, 9, 8));

        // ikony przedmiotów (renderowane z modeli 3D)
        for (Map.Entry<RivalsItem, Img> e : itemIcons.entrySet()) {
            providers.add(glyph(out, "item_" + e.getKey().id, e.getValue(), Glyphs.itemIcon(e.getKey()), 9, 8));
        }

        // panele scoreboardu 150 x 9
        int pw = Glyphs.PANEL_WIDTH;
        providers.add(glyph(out, "panel_top", new Img(pw, 9).roundRect(0, 0, pw, 9, 3, PANEL, PANEL_EDGE, true, false), Glyphs.PANEL_TOP, 9, 8));
        providers.add(glyph(out, "panel_mid", new Img(pw, 9).roundRect(0, 0, pw, 9, 3, PANEL, PANEL_EDGE, false, false), Glyphs.PANEL_MID, 9, 8));
        providers.add(glyph(out, "panel_bot", new Img(pw, 9).roundRect(0, 0, pw, 9, 3, PANEL, PANEL_EDGE, false, true), Glyphs.PANEL_BOT, 9, 8));
        providers.add(glyph(out, "panel_one", new Img(pw, 9).roundRect(0, 0, pw, 9, 3, PANEL, PANEL_EDGE, true, true), Glyphs.PANEL_ONE, 9, 8));

        // hitmarker - w podtytule; tekstura wysoka (przezroczysty dół), żeby ascent przesunął krzyżyk na środek ekranu
        providers.add(glyph(out, "hitmarker", hitmarker(WHITE), Glyphs.HITMARKER, 16, 15));
        providers.add(glyph(out, "hitmarker_head", hitmarker(RED), Glyphs.HITMARKER_HEAD, 16, 15));

        // błysk granatu - pełny ekran w tytule (skala 4x): 40x16 przeskalowane do wysokości 256
        providers.add(glyph(out, "flash", new Img(40, 16).fillAll(0xFFFFFFFF), Glyphs.FLASH, 256, 125));

        // tła GUI (176 x 222), ascent 13 = górna krawędź okna
        providers.add(glyph(out, "gui_kit", kitBackground(), Glyphs.GUI_KIT, 222, 13));
        providers.add(glyph(out, "gui_shop", shopBackground(), Glyphs.GUI_SHOP, 222, 13));

        out.json(out.asset("rivals", "font/icons.json"), obj("providers", providers));

        bossBars(out);
        animTextures(out);
    }

    private static JsonObject glyph(Out out, String name, Img img, char c, int height, int ascent) {
        img.save(out.asset("rivals", "textures/font/" + name + ".png"));
        return obj("type", "bitmap", "file", "rivals:font/" + name + ".png", "height", height, "ascent", ascent,
                "chars", arr(String.valueOf(c)));
    }

    private static Img hitmarker(int color) {
        Img img = new Img(7, 16);
        int[][] px = {{0, 0}, {1, 1}, {5, 1}, {6, 0}, {0, 6}, {1, 5}, {5, 5}, {6, 6}};
        for (int[] p : px) img.set(p[0], p[1], color);
        img.set(2, 2, Pal.withAlpha(color, 0x90));
        img.set(4, 2, Pal.withAlpha(color, 0x90));
        img.set(2, 4, Pal.withAlpha(color, 0x90));
        img.set(4, 4, Pal.withAlpha(color, 0x90));
        return img;
    }

    // tła GUI
    private static Img guiBase(String title) {
        Img img = new Img(176, 222);
        img.roundRect(0, 0, 176, 222, 4, 0xF20E1219, 0xFF2A313D, true, true);
        img.fill(4, 15, 168, 1, ACCENT_DARK);
        // ekwipunek gracza (okno 6-rzędowe): 3 rzędy od y=139 + pasek szybkiego wyboru y=197
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) plainSlot(img, 7 + c * 18, 138 + r * 18);
        for (int c = 0; c < 9; c++) plainSlot(img, 7 + c * 18, 196);
        img.fill(9, 133, 158, 1, 0xFF2A313D);
        return img;
    }

    private static void plainSlot(Img img, int x, int y) {
        img.fill(x, y, 18, 18, 0xFF121620);
        img.rect(x, y, 18, 18, 0xFF2A313D);
    }

    /** Ramka slotu (18x18) w kolorze kategorii. */
    private static void slot(Img img, int col, int row, int color) {
        int x = 7 + col * 18, y = 17 + row * 18;
        img.fill(x, y, 18, 18, 0xFF161B24);
        img.rect(x, y, 18, 18, Pal.mix(color, 0xFF161B24, 0.45));
        img.fill(x + 1, y + 17, 16, 1, Pal.mix(color, 0xFF161B24, 0.15));
    }

    private static Img kitBackground() {
        Img img = guiBase("ZESTAW");
        for (int c = 0; c < 4; c++) slot(img, c, 0, ACCENT);
        for (int c = 5; c < 7; c++) slot(img, c, 0, ICE_DARK);
        for (int c = 0; c < 3; c++) slot(img, c, 1, RED);
        for (int c = 0; c < 9; c++) slot(img, c, 2, GOLD);
        for (int c = 0; c < 7; c++) slot(img, c, 3, GOLD);
        img.fill(9, 17 + 4 * 18 + 8, 158, 1, 0xFF2A313D);
        for (int c = 0; c < 4; c++) slot(img, c, 5, WHITE);
        slot(img, 8, 5, GREEN);
        PixelFont.drawText(img, 4 * 18 + 10, 17 + 1 * 18 + 5, "GADŻETY", MID);
        PixelFont.drawText(img, 4 * 18 + 10, 17 + 5 * 18 + 5, "ZESTAW", LIGHT);
        return img;
    }

    private static Img shopBackground() {
        Img img = guiBase("SKLEP");
        for (int c = 0; c < 3; c++) slot(img, c, 0, ACCENT);
        slot(img, 8, 0, GOLD);
        for (int r = 1; r <= 4; r++) for (int c = 0; c < 9; c++) slot(img, c, r, GRAPHITE);
        img.fill(9, 17 + 5 * 18 + 8, 158, 1, 0xFF2A313D);
        PixelFont.drawText(img, 12, 17 + 5 * 18 + 5, "KLIKNIJ: KUP / ZAŁÓŻ", MID);
        return img;
    }

    // bossbar
    private static void bossBars(Out out) {
        Map<String, Integer> colors = Map.of("pink", PINK, "blue", ACCENT, "red", RED, "green", GREEN,
                "yellow", GOLD, "purple", PURPLE, "white", WHITE);
        for (Map.Entry<String, Integer> e : colors.entrySet()) {
            Img bg = new Img(182, 5).roundRect(0, 0, 182, 5, 2, 0xE0161B24, 0xF02A313D, true, true);
            bg.save(out.asset("minecraft", "textures/gui/sprites/boss_bar/" + e.getKey() + "_background.png"));
            int c = e.getValue();
            Img fg = new Img(182, 5).roundRect(0, 0, 182, 5, 2, c, Pal.darker(c, 0.35) | 0xFF000000, true, true);
            fg.fill(2, 1, 178, 1, Pal.lighter(c, 0.45));
            fg.save(out.asset("minecraft", "textures/gui/sprites/boss_bar/" + e.getKey() + "_progress.png"));
        }
    }

    // animowane tekstury
    private static void animTextures(Out out) {
        // pulsujący rdzeń kriogranatu
        int frames = 8;
        Img core = new Img(16, 16 * frames);
        for (int f = 0; f < frames; f++) {
            double t = (Math.sin(f / (double) frames * Math.PI * 2) + 1) / 2;
            int c = Pal.mix(ICE_DARK, WHITE, t * 0.8);
            core.fill(0, f * 16, 16, 16, c);
            core.rect(0, f * 16, 16, 16, Pal.mix(c, ACCENT, 0.5));
        }
        saveAnim(out, "core_pulse", core, 2, true);

        // dioda (miga)
        Img led = new Img(16, 32);
        led.fill(0, 0, 16, 16, RED);
        led.fill(0, 16, 16, 16, RED_DARK);
        saveAnim(out, "led_blink", led, 10, false);

        // energia tarczy - przesuwające się linie
        Img energy = new Img(16, 16 * frames);
        Img crack = new Img(16, 16 * 4);
        for (int f = 0; f < frames; f++) {
            for (int y = 0; y < 16; y++) {
                boolean line = (y + f * 2) % 8 == 0;
                int c = line ? Pal.withAlpha(WHITE, 0xC0) : Pal.withAlpha(ACCENT, 0x70 + ((y + f) % 4) * 8);
                energy.fill(0, f * 16 + y, 16, 1, c);
            }
            energy.rect(0, f * 16, 16, 16, Pal.withAlpha(ACCENT, 0xE0));
        }
        saveAnim(out, "energy", energy, 2, true);
        for (int f = 0; f < 4; f++) {
            crack.fill(0, f * 16, 16, 16, Pal.withAlpha(f % 2 == 0 ? ACCENT : RED, 0x60));
            int[][] cr = {{3, 2}, {4, 3}, {5, 4}, {5, 5}, {6, 6}, {7, 7}, {8, 7}, {9, 8}, {10, 9}, {10, 10}, {11, 11}, {12, 12},
                    {7, 8}, {6, 9}, {5, 10}, {4, 12}, {9, 6}, {10, 5}, {12, 4}};
            for (int[] p : cr) crack.set(p[0], f * 16 + p[1], WHITE);
            crack.rect(0, f * 16, 16, 16, Pal.withAlpha(RED, 0xE0));
        }
        saveAnim(out, "energy_crack", crack, 2, false);

        // ekran z wykresem
        Img screen = new Img(16, 16 * frames);
        for (int f = 0; f < frames; f++) {
            screen.fill(0, f * 16, 16, 16, 0xFF0E1A22);
            for (int x = 1; x < 15; x += 2) {
                int h = 3 + (int) Math.round((Math.sin((x + f * 2) * 0.7) + 1) * 4.5);
                screen.fill(x, f * 16 + 15 - h, 1, h, x % 4 == 1 ? ACCENT : ACCENT_DARK);
            }
            screen.fill(1, f * 16 + 1, 6, 1, WHITE);
            screen.fill(1, f * 16 + 3, 4, 1, MID);
        }
        saveAnim(out, "screen", screen, 4, false);
    }

    private static void saveAnim(Out out, String name, Img img, int frametime, boolean interpolate) {
        img.save(out.asset("rivals", "textures/item/anim/" + name + ".png"));
        out.json(out.asset("rivals", "textures/item/anim/" + name + ".png.mcmeta"),
                obj("animation", obj("frametime", frametime, "interpolate", interpolate)));
    }
}
