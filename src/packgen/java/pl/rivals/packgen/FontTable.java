package pl.rivals.packgen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Generuje src/shared/.../FontWidths.java: szerokości znaków czcionki vanilla (z ascii.png gry)
 * i czcionki pikselowej RIVALS - plugin liczy z nich szerokość tekstu (wyrównanie paneli scoreboardu).
 * Użycie: FontTable &lt;katalog vanilla&gt; &lt;plik wyjściowy .java&gt;
 */
public final class FontTable {

    public static void main(String[] args) throws Exception {
        BufferedImage ascii = ImageIO.read(new File(args[0], "assets/minecraft/textures/font/ascii.png"));
        int cw = ascii.getWidth() / 16, ch = ascii.getHeight() / 16;
        StringBuilder def = new StringBuilder();
        for (int cp = 0; cp < 256; cp++) {
            int col = cp % 16, row = cp / 16;
            int w = 0;
            for (int x = cw - 1; x >= 0 && w == 0; x--)
                for (int y = 0; y < ch; y++)
                    if ((ascii.getRGB(col * cw + x, row * ch + y) >>> 24) != 0) {
                        w = x + 1;
                        break;
                    }
            int adv = cp == ' ' ? 4 : w + 1;
            def.append(adv).append(cp % 32 == 31 ? ",\n            " : ", ");
        }
        StringBuilder pix = new StringBuilder();
        for (char c : PixelFont.GLYPHS.keySet()) {
            pix.append("        PIXEL.put('").append(c == '\'' ? "\\'" : String.valueOf(c)).append("', ")
                    .append(PixelFont.glyphWidth(c) + 2).append(");\n");
        }
        String src = """
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
                            %s};

                    /** Pikselowa czcionka RIVALS (z cieniem w teksturze). */
                    private static final Map<Character, Integer> PIXEL = new HashMap<>();

                    static {
                %s    }

                    /** Szerokość znaku czcionki vanilla; znaki spoza ASCII - jak litera bazowa. */
                    public static int vanilla(int cp) {
                        if (cp < 256) return DEFAULT[cp];
                        String base = Normalizer.normalize(new String(Character.toChars(cp)).replace('ł', 'l').replace('Ł', 'L'),
                                Normalizer.Form.NFD).replaceAll("\\\\p{M}", "");
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
                """.formatted(def.toString().trim().replaceAll(",$", ""), pix);
        Files.writeString(new File(args[1]).toPath(), src, StandardCharsets.UTF_8);
        System.out.println("FontWidths: " + args[1]);
    }
}
