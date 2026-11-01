package pl.rivals.packgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static pl.rivals.packgen.Out.arr;
import static pl.rivals.packgen.Out.nums;
import static pl.rivals.packgen.Out.obj;

/**
 * Zapisuje model: teksturę-paletę 16x16 (próbki 4x4 z obrysem), JSON elementów (format Blockbench / Java)
 * oraz ustawienia "display" liczone tak, by uchwyt modelu leżał w dłoni tam, gdzie rękojeść vanilla miecza.
 */
public final class ModelWriter {

    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};

    private final Out out;

    public ModelWriter(Out out) {
        this.out = out;
    }

    /** Zapisuje model pod models/item/&lt;name&gt;.json (+ tekstura). Zwraca referencję rivals:item/&lt;name&gt;. */
    /** Fragment atlasu: kolor + wymiary ściany (identyczne ściany współdzielą fragment). */
    private record Region(int color, boolean glow, int w, int h) {
    }

    private static int[] faceSize(String face, Model.Box b) {
        float w, h;
        switch (face) {
            case "north", "south" -> {
                w = b.x2 - b.x1;
                h = b.y2 - b.y1;
            }
            case "east", "west" -> {
                w = b.z2 - b.z1;
                h = b.y2 - b.y1;
            }
            default -> {
                w = b.x2 - b.x1;
                h = b.z2 - b.z1;
            }
        }
        return new int[]{Math.max(1, Math.min(48, Math.round(w))), Math.max(1, Math.min(48, Math.round(h)))};
    }

    public String write(Model m) {
        // 1. atlas: każda ściana ma własny fragment 1:1 (gęstość 1 teksel = 1 piksel modelu) z marginesem 1 px
        Map<Region, int[]> placed = new LinkedHashMap<>();
        int atlasW = 64;
        int cx = 0, cy = 0, rowH = 0;
        for (Model.Box b : m.boxes) {
            if (b.anim != null) continue;
            for (String f : FACES) {
                int[] s = faceSize(f, b);
                Region r = new Region(b.color, b.glow, s[0], s[1]);
                if (placed.containsKey(r)) continue;
                int pw = s[0] + 2, ph = s[1] + 2;
                if (cx + pw > atlasW) {
                    cx = 0;
                    cy += rowH;
                    rowH = 0;
                }
                placed.put(r, new int[]{cx + 1, cy + 1});
                cx += pw;
                rowH = Math.max(rowH, ph);
            }
        }
        int needH = Math.max(16, cy + rowH);
        int atlasH = 16;
        while (atlasH < needH) atlasH *= 2;
        int size = Math.max(atlasW, atlasH);
        Img tex = new Img(size, size);
        for (Map.Entry<Region, int[]> e : placed.entrySet()) {
            Region r = e.getKey();
            paintFace(tex, e.getValue()[0], e.getValue()[1], r.w, r.h, r.color, r.glow);
        }
        String texRef = "rivals:item/" + m.name;
        tex.save(out.asset("rivals", "textures/item/" + m.name + ".png"));

        // 2. elementy
        JsonObject textures = obj("0", texRef, "particle", texRef);
        JsonArray elements = new JsonArray();
        int n = 0;
        float k = 16f / size;
        for (Model.Box b : m.boxes) {
            check(m, b);
            JsonObject faces = new JsonObject();
            for (String f : FACES) {
                if (b.anim != null) {
                    textures.addProperty("a_" + b.anim, "rivals:item/anim/" + b.anim);
                    faces.add(f, obj("uv", nums(0, 0, 16, 16), "texture", "#a_" + b.anim));
                    continue;
                }
                int[] s = faceSize(f, b);
                int[] p = placed.get(new Region(b.color, b.glow, s[0], s[1]));
                float in = 0.02f;
                faces.add(f, obj("uv", nums((p[0] + in) * k, (p[1] + in) * k, (p[0] + s[0] - in) * k, (p[1] + s[1] - in) * k),
                        "texture", "#0"));
            }
            JsonObject el = obj("name", "e" + (n++), "from", nums(b.x1, b.y1, b.z1), "to", nums(b.x2, b.y2, b.z2));
            if (b.rotAxis != null && b.rotAngle != 0) {
                el.add("rotation", obj("angle", b.rotAngle, "axis", b.rotAxis, "origin", nums(b.ox, b.oy, b.oz)));
            }
            if (b.glow) {
                el.addProperty("shade", false);
                el.addProperty("light_emission", 15);
            }
            el.add("faces", faces);
            elements.add(el);
        }

        JsonObject model = obj("credit", "RIVALS packgen (low-poly, paleta)", "texture_size", arr(size, size),
                "textures", textures, "elements", elements, "display", display(m));
        out.json(out.asset("rivals", "models/item/" + m.name + ".json"), model);
        return "rivals:item/" + m.name;
    }

    /**
     * Ściana: delikatny pionowy gradient + fazowanie krawędzi (jaśniejsza góra/lewa, ciemniejszy dół/prawa).
     * Świecące: jaśniejszy środek. Margines 1 px kopiuje krawędź (bez przebarwień na mipmapach).
     */
    private static void paintFace(Img img, int x, int y, int w, int h, int color, boolean glow) {
        int a = color & 0xFF000000;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int c = color | 0xFF000000;
                if (glow) {
                    boolean edge = w >= 3 && h >= 3 && (i == 0 || j == 0 || i == w - 1 || j == h - 1);
                    c = edge ? c : Pal.lighter(c, 0.22);
                } else {
                    double t = h > 1 ? j / (double) (h - 1) : 0.5;
                    c = t < 0.5 ? Pal.lighter(c, 0.06 * (0.5 - t) * 2) : Pal.darker(c, 0.08 * (t - 0.5) * 2);
                    if (h >= 3 && j == 0) c = Pal.lighter(c, 0.14);
                    if (h >= 3 && j == h - 1) c = Pal.darker(c, 0.16);
                    if (w >= 3 && i == 0) c = Pal.lighter(c, 0.06);
                    if (w >= 3 && i == w - 1) c = Pal.darker(c, 0.1);
                }
                img.set(x + i, y + j, (c & 0x00FFFFFF) | a);
            }
        }
        // margines
        for (int i = -1; i <= w; i++) {
            img.set(x + i, y - 1, img.get(x + Math.max(0, Math.min(w - 1, i)), y));
            img.set(x + i, y + h, img.get(x + Math.max(0, Math.min(w - 1, i)), y + h - 1));
        }
        for (int j = 0; j < h; j++) {
            img.set(x - 1, y + j, img.get(x, y + j));
            img.set(x + w, y + j, img.get(x + w - 1, y + j));
        }
    }

    private static void check(Model m, Model.Box b) {
        float[] v = {b.x1, b.y1, b.z1, b.x2, b.y2, b.z2};
        for (float f : v) {
            if (f < -16 || f > 32) throw new IllegalStateException("Model " + m.name + ": element poza zakresem -16..32");
        }
        if (b.rotAxis != null) {
            float a = b.rotAngle;
            if (a != -45 && a != -22.5f && a != 0 && a != 22.5f && a != 45) {
                throw new IllegalStateException("Model " + m.name + ": niedozwolony kąt " + a);
            }
        }
    }

    // display
    private static final float[] SWORD_FP_T = {1.13f, 3.2f, 1.13f};
    private static final float[] SWORD_FP_R = {0, -90, 25};
    private static final float SWORD_FP_S = 0.68f;
    private static final float[] SWORD_TP_T = {0, 4, 0.5f};
    private static final float[] SWORD_TP_R = {0, -90, 55};
    private static final float SWORD_TP_S = 0.85f;
    /** Rękojeść vanilla miecza (sprite 16x16) względem środka modelu. */
    private static final Vector3f SWORD_GRIP = new Vector3f(-5.5f, -5.5f, 0);

    private JsonObject display(Model m) {
        JsonObject d = new JsonObject();
        Vector3f grip = new Vector3f(m.gripX - 8, m.gripY - 8, m.gripZ - 8);
        Vector3f anchor = m.centerAnchor || m.kind == Model.Kind.SMALL ? new Vector3f() : SWORD_GRIP;
        switch (m.kind) {
            case GUN -> {
                d.add("firstperson_righthand", hand(SWORD_FP_T, SWORD_FP_R, SWORD_FP_S, new float[]{0, -90, 5}, 0.68f, grip, anchor));
                d.add("thirdperson_righthand", hand(SWORD_TP_T, SWORD_TP_R, SWORD_TP_S, new float[]{0, -90, 55}, 0.85f, grip, anchor));
                d.add("gui", fit(m, m.guiRotation != null ? m.guiRotation : new float[]{10, -25, 20}, 15f, 1.6f));
            }
            case MELEE -> {
                d.add("firstperson_righthand", hand(SWORD_FP_T, SWORD_FP_R, SWORD_FP_S, new float[]{0, -90, -20}, 0.68f, grip, anchor));
                d.add("thirdperson_righthand", hand(SWORD_TP_T, SWORD_TP_R, SWORD_TP_S, new float[]{0, -90, 10}, 0.85f, grip, anchor));
                d.add("gui", fit(m, m.guiRotation != null ? m.guiRotation : new float[]{10, -25, -45}, 15f, 1.6f));
            }
            case SMALL -> {
                // jak zwykły przedmiot (śnieżka): środek modelu w środku sprite'a, lekko obrócony, żeby było widać bryłę
                float ss = Math.min(0.6f, 4.8f / maxDim(m));
                d.add("firstperson_righthand", hand(SWORD_FP_T, SWORD_FP_R, SWORD_FP_S, new float[]{0, -45, 15}, ss, grip, anchor));
                d.add("thirdperson_righthand", hand(SWORD_TP_T, SWORD_TP_R, SWORD_TP_S, new float[]{0, -45, 0}, ss, grip, anchor));
                d.add("gui", fit(m, m.guiRotation != null ? m.guiRotation : new float[]{20, -30, 0}, 13f, 2f));
            }
            case WORLD -> d.add("gui", fit(m, m.guiRotation != null ? m.guiRotation : new float[]{20, -30, 0}, 15f, 1.2f));
            case UI -> d.add("gui", fit(m, m.guiRotation != null ? m.guiRotation : new float[]{15, -25, 0}, 15f, 2f));
        }
        if (m.kind == Model.Kind.WORLD) {
            d.add("fixed", obj("rotation", nums(0, 0, 0), "translation", nums(0, 0, 0), "scale", nums(1, 1, 1)));
        } else {
            d.add("fixed", fit(m, new float[]{0, 0, 0}, 12f, 1f));
            d.add("ground", fit(m, new float[]{0, 0, 0}, 7f, 0.5f));
            d.add("head", obj("rotation", nums(0, 0, 0), "translation", nums(0, 0, 0), "scale", nums(1, 1, 1)));
        }
        return d;
    }

    /** Ustawienie w dłoni: uchwyt modelu ląduje tam, gdzie rękojeść vanilla miecza. */
    private static JsonObject hand(float[] tv, float[] rv, float sv, float[] ro, float so, Vector3f grip, Vector3f anchor) {
        Vector3f a = rot(rv).transform(new Vector3f(anchor).mul(sv));
        Vector3f b = rot(ro).transform(new Vector3f(grip).mul(so));
        Vector3f t = new Vector3f(tv[0], tv[1], tv[2]).add(a).sub(b);
        return obj("rotation", nums(ro), "translation", nums(clamp(t.x), clamp(t.y), clamp(t.z)), "scale", nums(so, so, so));
    }

    private static float maxDim(Model m) {
        float minX = 99, minY = 99, minZ = 99, maxX = -99, maxY = -99, maxZ = -99;
        for (Model.Box b : m.boxes) {
            minX = Math.min(minX, b.x1); minY = Math.min(minY, b.y1); minZ = Math.min(minZ, b.z1);
            maxX = Math.max(maxX, b.x2); maxY = Math.max(maxY, b.y2); maxZ = Math.max(maxZ, b.z2);
        }
        return Math.max(1f, Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)));
    }

    /** Dopasowanie do kwadratu target px (GUI/ziemia/ramka) z wyśrodkowaniem. */
    private static JsonObject fit(Model m, float[] r, float target, float maxScale) {
        Quaternionf q = rot(r);
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (Model.Box b : m.boxes) {
            for (int i = 0; i < 8; i++) {
                Vector3f p = new Vector3f((i & 1) == 0 ? b.x1 : b.x2, (i & 2) == 0 ? b.y1 : b.y2, (i & 4) == 0 ? b.z1 : b.z2).sub(8, 8, 8);
                q.transform(p);
                minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
                minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
            }
        }
        float size = Math.max(0.01f, Math.max(maxX - minX, maxY - minY));
        float s = Math.min(maxScale, target / size);
        Vector3f c = new Vector3f((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2).mul(s);
        return obj("rotation", nums(r), "translation", nums(clamp(-c.x), clamp(-c.y), clamp(-c.z)), "scale", nums(s, s, s));
    }

    static Quaternionf rot(float[] deg) {
        return new Quaternionf().rotationXYZ((float) Math.toRadians(deg[0]), (float) Math.toRadians(deg[1]), (float) Math.toRadians(deg[2]));
    }

    private static float clamp(float v) {
        return Math.max(-80, Math.min(80, v));
    }

    // ikony 9x9
    /**
     * Renderuje ikonę 9x9 z modelu (rzut boczny albo z góry), z obrysem 1 px w kolorze INK.
     * Każdy element (również obrócony) jest rzutowany jako wielokąt wypukły.
     */
    public static Img icon(Model m) {
        record Poly(float[] xs, float[] ys, float depth, int color) {
        }
        List<Poly> polys = new ArrayList<>();
        float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (Model.Box b : m.boxes) {
            float[][] pts = new float[8][];
            for (int i = 0; i < 8; i++) {
                Vector3f p = new Vector3f((i & 1) == 0 ? b.x1 : b.x2, (i & 2) == 0 ? b.y1 : b.y2, (i & 4) == 0 ? b.z1 : b.z2);
                if (b.rotAxis != null && b.rotAngle != 0) {
                    Vector3f o = new Vector3f(b.ox, b.oy, b.oz);
                    p.sub(o);
                    float a = (float) Math.toRadians(b.rotAngle);
                    switch (b.rotAxis) {
                        case "x" -> p.rotateX(a);
                        case "y" -> p.rotateY(a);
                        default -> p.rotateZ(a);
                    }
                    p.add(o);
                }
                // bronie białe - po skosie (czubek w prawy górny róg), jak vanilla miecz
                if (m.kind == Model.Kind.MELEE) {
                    p.sub(8, 8, 8).rotateZ((float) Math.toRadians(-45)).add(8, 8, 8);
                }
                // SIDE: patrzymy od +Z (u = x, v = y); TOP: od +Y (u = x, v = -z)
                pts[i] = m.iconView == Model.IconView.SIDE ? new float[]{p.x, p.y, p.z} : new float[]{p.x, -p.z, p.y};
            }
            float[][] hull = hull(pts);
            float[] xs = new float[hull.length], ys = new float[hull.length];
            float depth = -Float.MAX_VALUE;
            for (int i = 0; i < hull.length; i++) {
                xs[i] = hull[i][0];
                ys[i] = hull[i][1];
                minU = Math.min(minU, xs[i]); maxU = Math.max(maxU, xs[i]);
                minV = Math.min(minV, ys[i]); maxV = Math.max(maxV, ys[i]);
            }
            for (float[] p : pts) depth = Math.max(depth, p[2]);
            int color = b.color;
            if ((color >>> 24) < 0xFF) color = Pal.mix(Pal.STEEL, color | 0xFF000000, (color >>> 24) / 255.0) | 0xFF000000;
            polys.add(new Poly(xs, ys, depth, color));
        }
        polys.sort((p1, p2) -> Float.compare(p1.depth(), p2.depth()));

        int area = 7;
        float size = Math.max(maxU - minU, maxV - minV);
        float scale = area / Math.max(0.01f, size);
        float offU = (area - (maxU - minU) * scale) / 2f;
        float offV = (area - (maxV - minV) * scale) / 2f;

        Img img = new Img(9, 9);
        int ss = 4;
        for (int py = 0; py < area; py++) {
            for (int px = 0; px < area; px++) {
                Map<Integer, Integer> votes = new LinkedHashMap<>();
                int covered = 0;
                for (int sy = 0; sy < ss; sy++) {
                    for (int sx = 0; sx < ss; sx++) {
                        float u = minU + ((px + (sx + 0.5f) / ss) - offU) / scale;
                        float v = maxV - ((py + (sy + 0.5f) / ss) - offV) / scale;
                        Integer col = null;
                        for (Poly p : polys) if (inside(p.xs(), p.ys(), u, v)) col = p.color();
                        if (col != null) {
                            covered++;
                            votes.merge(col, 1, Integer::sum);
                        }
                    }
                }
                if (covered >= 4) {
                    int best = 0, bestN = -1;
                    for (Map.Entry<Integer, Integer> e : votes.entrySet()) {
                        if (e.getValue() > bestN) {
                            best = e.getKey();
                            bestN = e.getValue();
                        }
                    }
                    img.set(px + 1, py + 1, best);
                }
            }
        }
        // obrys
        Img outImg = new Img(9, 9);
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                int c = img.get(x, y);
                if ((c >>> 24) != 0) {
                    outImg.set(x, y, c);
                } else if (filled(img, x - 1, y) || filled(img, x + 1, y) || filled(img, x, y - 1) || filled(img, x, y + 1)) {
                    outImg.set(x, y, Pal.INK);
                }
            }
        }
        return outImg;
    }

    private static boolean filled(Img img, int x, int y) {
        return (img.get(x, y) >>> 24) != 0;
    }

    private static boolean inside(float[] xs, float[] ys, float x, float y) {
        int n = xs.length;
        if (n < 3) return false;
        boolean pos = false, neg = false;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float cross = (xs[j] - xs[i]) * (y - ys[i]) - (ys[j] - ys[i]) * (x - xs[i]);
            if (cross > 1e-6) pos = true;
            if (cross < -1e-6) neg = true;
            if (pos && neg) return false;
        }
        return true;
    }

    /** Otoczka wypukła (Andrew monotone chain) punktów 2D. */
    private static float[][] hull(float[][] pts) {
        float[][] p = pts.clone();
        Arrays.sort(p, (a, b) -> a[0] != b[0] ? Float.compare(a[0], b[0]) : Float.compare(a[1], b[1]));
        int n = p.length, k = 0;
        float[][] h = new float[2 * n][];
        for (int i = 0; i < n; i++) {
            while (k >= 2 && cross(h[k - 2], h[k - 1], p[i]) <= 0) k--;
            h[k++] = p[i];
        }
        for (int i = n - 2, t = k + 1; i >= 0; i--) {
            while (k >= t && cross(h[k - 2], h[k - 1], p[i]) <= 0) k--;
            h[k++] = p[i];
        }
        float[][] res = new float[Math.max(1, k - 1)][];
        System.arraycopy(h, 0, res, 0, res.length);
        return res;
    }

    private static float cross(float[] o, float[] a, float[] b) {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }
}
