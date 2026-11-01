package pl.rivals.packgen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/** Minimalny "pędzel" pikselowy na BufferedImage (ARGB). */
public final class Img {

    public final BufferedImage img;
    public final int w;
    public final int h;

    public Img(int w, int h) {
        this.w = w;
        this.h = h;
        this.img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    public Img set(int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < w && y < h) img.setRGB(x, y, argb);
        return this;
    }

    public int get(int x, int y) {
        return x >= 0 && y >= 0 && x < w && y < h ? img.getRGB(x, y) : 0;
    }

    public Img fill(int x, int y, int fw, int fh, int argb) {
        for (int i = x; i < x + fw; i++)
            for (int j = y; j < y + fh; j++) set(i, j, argb);
        return this;
    }

    public Img fillAll(int argb) {
        return fill(0, 0, w, h, argb);
    }

    /** Obrys prostokąta 1 px. */
    public Img rect(int x, int y, int rw, int rh, int argb) {
        for (int i = x; i < x + rw; i++) {
            set(i, y, argb);
            set(i, y + rh - 1, argb);
        }
        for (int j = y; j < y + rh; j++) {
            set(x, j, argb);
            set(x + rw - 1, j, argb);
        }
        return this;
    }

    /** Wypełniony prostokąt z zaokrąglonymi rogami (promień r, pikselowo). */
    public Img roundRect(int x, int y, int rw, int rh, int r, int fillArgb, int edgeArgb, boolean top, boolean bottom) {
        for (int j = 0; j < rh; j++) {
            for (int i = 0; i < rw; i++) {
                if (!inside(i, j, rw, rh, r, top, bottom)) continue;
                boolean edge = !inside(i - 1, j, rw, rh, r, top, bottom) || !inside(i + 1, j, rw, rh, r, top, bottom)
                        || (top && !inside(i, j - 1, rw, rh, r, true, bottom))
                        || (bottom && !inside(i, j + 1, rw, rh, r, top, true));
                set(x + i, y + j, edge && edgeArgb != 0 ? edgeArgb : fillArgb);
            }
        }
        return this;
    }

    private static boolean inside(int i, int j, int rw, int rh, int r, boolean top, boolean bottom) {
        if (i < 0 || i >= rw) return false;
        if (j < 0) return !top;
        if (j >= rh) return !bottom;
        int cx = -1, cy = -1;
        if (top && j < r) cy = r;
        if (bottom && j >= rh - r) cy = rh - 1 - r;
        if (i < r) cx = r;
        if (i >= rw - r) cx = rw - 1 - r;
        if (cx < 0 || cy < 0) return true;
        double dx = i - cx, dy = j - cy;
        return dx * dx + dy * dy <= (r + 0.35) * (r + 0.35);
    }

    /** Rysuje ascii-art: każdy znak to kolor z mapy ('.' = przezroczysty). */
    public Img pattern(int x, int y, String[] rows, Map<Character, Integer> colors) {
        for (int j = 0; j < rows.length; j++) {
            String row = rows[j];
            for (int i = 0; i < row.length(); i++) {
                Integer c = colors.get(row.charAt(i));
                if (c != null && c != 0) set(x + i, y + j, c);
            }
        }
        return this;
    }

    public Img blit(Img src, int x, int y) {
        for (int j = 0; j < src.h; j++)
            for (int i = 0; i < src.w; i++) {
                int c = src.get(i, j);
                if ((c >>> 24) != 0) set(x + i, y + j, c);
            }
        return this;
    }

    public void save(File file) {
        file.getParentFile().mkdirs();
        try {
            ImageIO.write(img, "png", file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
