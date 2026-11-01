package pl.rivals.hud;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.TextComponent;
import pl.rivals.shared.FontWidths;
import pl.rivals.shared.Glyphs;
import pl.rivals.shared.RivalsItem;

import java.util.Locale;
import java.util.Map;

/** Komponenty HUD: glify z resource packa, odstępy, pikselowa czcionka, kolory stylu RIVALS. */
public final class Hud {

    private Hud() {
    }

    public static final Key ICONS = Key.key("rivals", "icons");
    public static final Key PIXEL = Key.key("rivals", "pixel");

    public static final TextColor ACCENT = TextColor.color(0x38E8F0);
    public static final TextColor ACCENT_BRIGHT = TextColor.color(0xB8FBFF);
    public static final TextColor ACCENT_DARK = TextColor.color(0x1F9BA3);
    public static final TextColor TEXT = TextColor.color(0xEEF2F6);
    public static final TextColor MUTED = TextColor.color(0x8A93A6);
    public static final TextColor RED = TextColor.color(0xFF4D5E);
    public static final TextColor GOLD = TextColor.color(0xFFC83D);
    public static final TextColor GREEN = TextColor.color(0x5CE18A);

    public static Key big(int frame) {
        return Key.key("rivals", "big" + Math.max(0, Math.min(Glyphs.BIG_HEIGHTS.length - 1, frame)));
    }

    /** Glif z czcionki rivals:icons - biały (bez przebarwienia) i bez cienia. */
    public static Component glyph(char c) {
        return Component.text(String.valueOf(c)).font(ICONS).color(NamedTextColor.WHITE)
                .shadowColor(ShadowColor.none()).decoration(TextDecoration.ITALIC, false);
    }

    public static Component icon(RivalsItem item) {
        return glyph(Glyphs.itemIcon(item));
    }

    /** Przesunięcie kursora o px (ujemne w lewo). */
    public static Component space(int px) {
        if (px == 0) return Component.empty();
        return Component.text(Glyphs.space(px)).font(ICONS);
    }

    /** Napis pikselową czcionką (wielkie litery, cień jest w teksturze). */
    public static TextComponent pixel(String text, TextColor color) {
        return Component.text(text.toUpperCase(Locale.ROOT)).font(PIXEL).color(color)
                .shadowColor(ShadowColor.none()).decoration(TextDecoration.ITALIC, false);
    }

    public static TextComponent text(String text, TextColor color) {
        return Component.text(text).color(color).decoration(TextDecoration.ITALIC, false);
    }

    /** Duży napis (tytuły) w klatce powiększenia 0..4. */
    public static Component bigText(String text, TextColor color, int frame) {
        return Component.text(text.toUpperCase(Locale.ROOT)).font(big(frame)).color(color).shadowColor(ShadowColor.none());
    }

    /**
     * Wiersz z liczoną szerokością (piksele GUI). Panel jest dopełniany odstępem do pełnej szerokości,
     * dzięki czemu sidebar ma zawsze szerokość panelu i nic nie wystaje poza ekran.
     */
    public static final class Row {
        private final TextComponent.Builder b = Component.text();
        private int width;

        public Row icon(char c) {
            b.append(glyph(c));
            width += 8; // zaniżone celowo – dopełnienie nigdy nie przekroczy panelu
            return this;
        }

        public Row text(String s, TextColor c) {
            b.append(Hud.text(s, c));
            width += FontWidths.vanilla(s);
            return this;
        }

        public Row pixel(String s, TextColor c) {
            b.append(Hud.pixel(s, c));
            width += FontWidths.pixel(s);
            return this;
        }

        public Row gap(int px) {
            b.append(space(px));
            width += px;
            return this;
        }

        public Row add(Component c, int approxWidth) {
            b.append(c);
            width += approxWidth;
            return this;
        }

        public int width() {
            return width;
        }

        public Component build() {
            return b.build();
        }
    }

    public static Row row() {
        return new Row();
    }

    /** Wiersz panelu dopełniony do pełnej szerokości panelu. */
    public static Component panelRow(char panelGlyph, Row content) {
        int inner = Glyphs.PANEL_WIDTH - 4;
        return Component.text()
                .append(glyph(panelGlyph))
                .append(space(-(Glyphs.PANEL_WIDTH + 1) + 4))
                .append(content.build())
                .append(space(Math.max(0, inner - content.width())))
                .build();
    }

    /** Nagłówek sekcji jako wiersz z szerokością. */
    public static Row headerRow(String name) {
        return row().add(glyph(Glyphs.HEADER_BAR), 5).gap(2).pixel(name, ACCENT);
    }

    /** Nagłówek sekcji: kreska + NAZWA wielkimi literami, turkusowo. */
    public static Component header(String name) {
        return Component.text()
                .append(glyph(Glyphs.HEADER_BAR))
                .append(space(2))
                .append(pixel(name, ACCENT))
                .build();
    }

    /** Tytuł okna GUI: tło z resource packa + tekst pikselową czcionką. */
    public static Component guiTitle(char background, String title) {
        return Component.text()
                .append(space(-8))
                .append(glyph(background))
                .append(space(-(Glyphs.GUI_WIDTH + 1) + 8))
                .append(glyph(Glyphs.HEADER_BAR))
                .append(space(2))
                .append(pixel(title, TextColor.color(0xEEF2F6)))
                .build();
    }

    // MiniMessage
    private static final Map<String, Character> ICON_NAMES = Map.ofEntries(
            Map.entry("clock", Glyphs.CLOCK), Map.entry("rank", Glyphs.RANK), Map.entry("trophy", Glyphs.TROPHY),
            Map.entry("coin", Glyphs.COIN_0), Map.entry("flame", Glyphs.FLAME), Map.entry("you", Glyphs.YOU),
            Map.entry("enemy", Glyphs.ENEMY), Map.entry("heart", Glyphs.HEART), Map.entry("map", Glyphs.MAP),
            Map.entry("queue", Glyphs.QUEUE), Map.entry("check", Glyphs.CHECK), Map.entry("ammo", Glyphs.AMMO),
            Map.entry("dot", Glyphs.DOT), Map.entry("lock", Glyphs.LOCK), Map.entry("bar", Glyphs.HEADER_BAR));

    /** Tag &lt;icon:nazwa&gt; w messages.yml (np. &lt;icon:coin&gt;, &lt;icon:cryo_grenade&gt;). */
    public static final TagResolver ICON_TAG = TagResolver.resolver("icon", (args, ctx) -> {
        String name = args.popOr("brak nazwy ikony").value().toLowerCase(Locale.ROOT);
        Character c = ICON_NAMES.get(name);
        if (c == null) {
            RivalsItem item = RivalsItem.byId(name);
            if (item != null) c = Glyphs.itemIcon(item);
        }
        return Tag.selfClosingInserting(c == null ? Component.text("?") : glyph(c));
    });
}
