package pl.rivals.packgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Programowy podgląd modeli (narzędzie deweloperskie, nie trafia do pluginu): rysuje modele JSON
 * z teksturami tak jak gra - transformacje "display", obroty elementów, oświetlenie przedmiotów,
 * widok z pierwszej osoby (przesunięcie ręki jak w ItemInHandRenderer, FOV 70) i w ekwipunku.
 * Użycie: Preview &lt;katalog packa&gt; &lt;katalog vanilla&gt; &lt;katalog wyjściowy&gt; [modele...]
 */
public final class Preview {

    private record Quad(Vector3f[] p, float[][] uv, BufferedImage tex, boolean shade) {
    }

    private record ModelData(List<Quad> quads, JsonObject display) {
    }

    private final File pack;
    private final File vanilla;
    private final Map<String, BufferedImage> textures = new HashMap<>();

    Preview(File pack, File vanilla) {
        this.pack = pack;
        this.vanilla = vanilla;
    }

    public static void main(String[] args) throws Exception {
        File pack = new File(args[0]);
        File vanilla = new File(args[1]);
        File out = new File(args[2]);
        out.mkdirs();
        Preview pv = new Preview(pack, vanilla);
        List<String> models = new ArrayList<>();
        for (int i = 3; i < args.length; i++) models.add(args[i]);
        if (models.isEmpty()) {
            File dir = new File(pack, "assets/rivals/models/item");
            for (File f : dir.listFiles()) {
                String n = f.getName();
                if (n.endsWith(".json") && !n.contains("_cd") && !n.contains("__")) models.add(n.substring(0, n.length() - 5));
            }
            models.sort(String::compareTo);
            File ui = new File(pack, "assets/rivals/models/item/ui");
            if (ui.exists()) for (File f : ui.listFiles()) models.add("ui/" + f.getName().replace(".json", ""));
        }
        pv.sheet(models, new File(out, "hand.png"), true);
        pv.sheet(models, new File(out, "gui.png"), false);
        File wdir = new File(pack, "assets/rivals/models/item/world");
        List<String> world = new ArrayList<>();
        if (wdir.exists()) for (File f : wdir.listFiles()) world.add("world/" + f.getName().replace(".json", ""));
        world.sort(String::compareTo);
        pv.worldSheet(world, new File(out, "world.png"));
        System.out.println("Podgląd: " + out.getAbsolutePath());
    }

    // wczytywanie
    private BufferedImage texture(String ref) {
        return textures.computeIfAbsent(ref, r -> {
            String ns = r.contains(":") ? r.substring(0, r.indexOf(':')) : "minecraft";
            String path = r.contains(":") ? r.substring(r.indexOf(':') + 1) : r;
            File base = ns.equals("minecraft") ? vanilla : pack;
            File f = new File(base, "assets/" + ns + "/textures/" + path + ".png");
            try {
                BufferedImage img = ImageIO.read(f);
                // animowana: pierwsza klatka
                if (img.getHeight() > img.getWidth()) img = img.getSubimage(0, 0, img.getWidth(), img.getWidth());
                return img;
            } catch (IOException e) {
                BufferedImage miss = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
                miss.setRGB(0, 0, 0xFFFF00FF);
                miss.setRGB(1, 1, 0xFFFF00FF);
                return miss;
            }
        });
    }

    private JsonObject json(String ref) throws IOException {
        String ns = ref.contains(":") ? ref.substring(0, ref.indexOf(':')) : "minecraft";
        String path = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : ref;
        File base = ns.equals("minecraft") ? vanilla : pack;
        File f = new File(base, "assets/" + ns + "/models/" + path + ".json");
        return JsonParser.parseString(Files.readString(f.toPath())).getAsJsonObject();
    }

    private ModelData load(String ref) throws IOException {
        JsonObject m = json(ref);
        JsonObject display = m.has("display") ? m.getAsJsonObject("display") : new JsonObject();
        Map<String, String> tex = new HashMap<>();
        JsonObject cur = m;
        JsonArray elements = null;
        boolean generated = false;
        while (true) {
            if (cur.has("textures")) {
                for (var e : cur.getAsJsonObject("textures").entrySet()) tex.putIfAbsent(e.getKey(), e.getValue().getAsString());
            }
            if (elements == null && cur.has("elements")) elements = cur.getAsJsonArray("elements");
            if (cur.has("display")) {
                for (var e : cur.getAsJsonObject("display").entrySet()) if (!display.has(e.getKey())) display.add(e.getKey(), e.getValue());
            }
            if (!cur.has("parent")) break;
            String parent = cur.get("parent").getAsString();
            if (parent.endsWith("item/generated") || parent.equals("builtin/generated")) generated = true;
            if (parent.startsWith("builtin/")) break;
            cur = json(parent);
        }
        List<Quad> quads = new ArrayList<>();
        if (elements == null && generated) {
            BufferedImage t = texture(resolve(tex, "#layer0"));
            quads.add(new Quad(new Vector3f[]{v(0, 16, 8.5f), v(16, 16, 8.5f), v(16, 0, 8.5f), v(0, 0, 8.5f)},
                    new float[][]{{0, 0}, {16, 0}, {16, 16}, {0, 16}}, t, true));
            quads.add(new Quad(new Vector3f[]{v(16, 16, 7.5f), v(0, 16, 7.5f), v(0, 0, 7.5f), v(16, 0, 7.5f)},
                    new float[][]{{16, 0}, {0, 0}, {0, 16}, {16, 16}}, t, true));
            return new ModelData(quads, display);
        }
        if (elements == null) return new ModelData(quads, display);
        for (JsonElement ee : elements) {
            JsonObject el = ee.getAsJsonObject();
            float[] f = arr(el.getAsJsonArray("from"));
            float[] t = arr(el.getAsJsonArray("to"));
            boolean shade = !el.has("shade") || el.get("shade").getAsBoolean();
            if (el.has("light_emission") && el.get("light_emission").getAsInt() > 0) shade = false;
            Matrix4f rot = new Matrix4f();
            if (el.has("rotation")) {
                JsonObject r = el.getAsJsonObject("rotation");
                float[] o = arr(r.getAsJsonArray("origin"));
                float a = (float) Math.toRadians(r.get("angle").getAsFloat());
                Vector3f axis = switch (r.get("axis").getAsString()) {
                    case "x" -> new Vector3f(1, 0, 0);
                    case "y" -> new Vector3f(0, 1, 0);
                    default -> new Vector3f(0, 0, 1);
                };
                rot.translate(o[0], o[1], o[2]).rotate(a, axis).translate(-o[0], -o[1], -o[2]);
            }
            JsonObject faces = el.getAsJsonObject("faces");
            for (var fe : faces.entrySet()) {
                JsonObject face = fe.getValue().getAsJsonObject();
                float[] uv = face.has("uv") ? arr(face.getAsJsonArray("uv")) : new float[]{0, 0, 16, 16};
                BufferedImage img = texture(resolve(tex, face.get("texture").getAsString()));
                Vector3f[] p = corners(fe.getKey(), f, t);
                for (Vector3f q : p) rot.transformPosition(q);
                quads.add(new Quad(p, new float[][]{{uv[0], uv[1]}, {uv[2], uv[1]}, {uv[2], uv[3]}, {uv[0], uv[3]}}, img, shade));
            }
        }
        return new ModelData(quads, display);
    }

    private static String resolve(Map<String, String> tex, String ref) {
        String r = ref;
        for (int i = 0; i < 8 && r.startsWith("#"); i++) r = tex.getOrDefault(r.substring(1), "missing");
        return r;
    }

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private static float[] arr(JsonArray a) {
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
        return f;
    }

    /** Narożniki ściany: lewy-górny, prawy-górny, prawy-dolny, lewy-dolny (patrząc z zewnątrz). */
    private static Vector3f[] corners(String face, float[] a, float[] b) {
        float x1 = a[0], y1 = a[1], z1 = a[2], x2 = b[0], y2 = b[1], z2 = b[2];
        return switch (face) {
            case "north" -> new Vector3f[]{v(x2, y2, z1), v(x1, y2, z1), v(x1, y1, z1), v(x2, y1, z1)};
            case "south" -> new Vector3f[]{v(x1, y2, z2), v(x2, y2, z2), v(x2, y1, z2), v(x1, y1, z2)};
            case "east" -> new Vector3f[]{v(x2, y2, z2), v(x2, y2, z1), v(x2, y1, z1), v(x2, y1, z2)};
            case "west" -> new Vector3f[]{v(x1, y2, z1), v(x1, y2, z2), v(x1, y1, z2), v(x1, y1, z1)};
            case "up" -> new Vector3f[]{v(x1, y2, z1), v(x2, y2, z1), v(x2, y2, z2), v(x1, y2, z2)};
            default -> new Vector3f[]{v(x1, y1, z2), v(x2, y1, z2), v(x2, y1, z1), v(x1, y1, z1)};
        };
    }

    // transformacje
    /** Macierz: model (px) -> przestrzeń widoku dla danego kontekstu display. */
    private static Matrix4f displayMatrix(JsonObject display, String ctx) {
        float[] t = {0, 0, 0}, r = {0, 0, 0}, s = {1, 1, 1};
        if (display.has(ctx)) {
            JsonObject d = display.getAsJsonObject(ctx);
            if (d.has("translation")) t = arr(d.getAsJsonArray("translation"));
            if (d.has("rotation")) r = arr(d.getAsJsonArray("rotation"));
            if (d.has("scale")) s = arr(d.getAsJsonArray("scale"));
        }
        return new Matrix4f()
                .translate(t[0] / 16f, t[1] / 16f, t[2] / 16f)
                .rotateXYZ((float) Math.toRadians(r[0]), (float) Math.toRadians(r[1]), (float) Math.toRadians(r[2]))
                .scale(s[0], s[1], s[2])
                .translate(-0.5f, -0.5f, -0.5f)
                .scale(1 / 16f);
    }

    // rasteryzacja
    private static final Vector3f L0 = new Vector3f(0.2f, 1f, -0.7f).normalize();
    private static final Vector3f L1 = new Vector3f(-0.2f, 1f, 0.7f).normalize();

    /**
     * @param world   przekształcenie model -> przestrzeń kamery
     * @param persp   true = perspektywa (kamera w 0, patrzy na -Z), false = rzut prostokątny (x,y -> ekran)
     * @param scale   piksele na jednostkę (ortho) albo ogniskowa (persp)
     */
    private static void render(BufferedImage img, float[] zbuf, ModelData m, Matrix4f world, boolean persp,
                               float scale, float cx, float cy) {
        int w = img.getWidth(), h = img.getHeight();
        Matrix4f normalM = new Matrix4f(world).invert().transpose();
        for (Quad q : m.quads()) {
            Vector3f[] p = new Vector3f[4];
            float[][] s = new float[4][3];
            boolean behind = false;
            for (int i = 0; i < 4; i++) {
                p[i] = world.transformPosition(new Vector3f(q.p()[i]));
                if (persp) {
                    if (p[i].z > -0.01f) behind = true;
                    float iz = -1f / p[i].z;
                    s[i][0] = cx + p[i].x * iz * scale;
                    s[i][1] = cy - p[i].y * iz * scale;
                    s[i][2] = iz;
                } else {
                    s[i][0] = cx + p[i].x * scale;
                    s[i][1] = cy - p[i].y * scale;
                    s[i][2] = p[i].z;
                }
            }
            if (behind) continue;
            Vector3f n = new Vector3f(p[1]).sub(p[0]).cross(new Vector3f(p[3]).sub(p[0]));
            if (n.lengthSquared() < 1e-12f) continue;
            n.normalize();
            float light = 1f;
            if (q.shade()) {
                light = Math.min(1f, 0.4f + 0.6f * (Math.max(0, n.dot(L0)) + Math.max(0, n.dot(L1))));
            }
            tri(img, zbuf, w, h, s[0], s[1], s[2], q.uv()[0], q.uv()[1], q.uv()[2], q.tex(), light, persp);
            tri(img, zbuf, w, h, s[0], s[2], s[3], q.uv()[0], q.uv()[2], q.uv()[3], q.tex(), light, persp);
        }
    }

    private static void tri(BufferedImage img, float[] zb, int w, int h, float[] a, float[] b, float[] c,
                            float[] ua, float[] ub, float[] uc, BufferedImage tex, float light, boolean persp) {
        int minX = (int) Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int maxX = (int) Math.min(w - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int minY = (int) Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int maxY = (int) Math.min(h - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        float area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);
        if (Math.abs(area) < 1e-6f) return;
        int tw = tex.getWidth(), th = tex.getHeight();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f, py = y + 0.5f;
                float w0 = ((b[0] - px) * (c[1] - py) - (c[0] - px) * (b[1] - py)) / area;
                float w1 = ((c[0] - px) * (a[1] - py) - (a[0] - px) * (c[1] - py)) / area;
                float w2 = 1 - w0 - w1;
                if (w0 < -1e-4f || w1 < -1e-4f || w2 < -1e-4f) continue;
                float z = w0 * a[2] + w1 * b[2] + w2 * c[2];
                int idx = y * w + x;
                if (z <= zb[idx]) continue;
                float u, vv;
                if (persp) {
                    float iz = z;
                    u = (w0 * ua[0] * a[2] + w1 * ub[0] * b[2] + w2 * uc[0] * c[2]) / iz;
                    vv = (w0 * ua[1] * a[2] + w1 * ub[1] * b[2] + w2 * uc[1] * c[2]) / iz;
                } else {
                    u = w0 * ua[0] + w1 * ub[0] + w2 * uc[0];
                    vv = w0 * ua[1] + w1 * ub[1] + w2 * uc[1];
                }
                int tx = Math.max(0, Math.min(tw - 1, (int) Math.floor(u / 16f * tw)));
                int ty = Math.max(0, Math.min(th - 1, (int) Math.floor(vv / 16f * th)));
                int col = tex.getRGB(tx, ty);
                int al = col >>> 24;
                if (al < 25) continue;
                int r = (int) ((col >> 16 & 0xFF) * light), g = (int) ((col >> 8 & 0xFF) * light), bl = (int) ((col & 0xFF) * light);
                if (al < 250) {
                    int dst = img.getRGB(x, y);
                    float k = al / 255f;
                    r = (int) (r * k + (dst >> 16 & 0xFF) * (1 - k));
                    g = (int) (g * k + (dst >> 8 & 0xFF) * (1 - k));
                    bl = (int) (bl * k + (dst & 0xFF) * (1 - k));
                } else {
                    zb[idx] = z;
                }
                img.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | bl);
            }
        }
    }

    // arkusze
    /** Pierwsza osoba (prawa ręka) albo GUI; pierwsza komórka: vanilla miecz jako wzorzec. */
    void sheet(List<String> models, File out, boolean hand) throws IOException {
        List<String> all = new ArrayList<>();
        all.add("minecraft:item/diamond_sword_ref");
        for (String m : models) all.add("rivals:item/" + m);
        int cw = hand ? 480 : 168, ch = hand ? 270 : 182, cols = hand ? 4 : 8;
        int rows = (all.size() + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(cols * cw, rows * ch, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, hand ? 14 : 10));
        for (int i = 0; i < all.size(); i++) {
            String ref = all.get(i);
            ModelData md;
            try {
                md = ref.endsWith("diamond_sword_ref") ? swordRef() : load(ref);
            } catch (Exception e) {
                continue;
            }
            BufferedImage cell = new BufferedImage(cw, ch - (hand ? 0 : 14), BufferedImage.TYPE_INT_RGB);
            Graphics2D cg = cell.createGraphics();
            cg.setColor(hand ? new Color(0x87A8C8) : new Color(0x8B8B8B));
            cg.fillRect(0, 0, cell.getWidth(), cell.getHeight());
            if (hand) {
                cg.setColor(new Color(0x5E7D3A));
                cg.fillRect(0, cell.getHeight() / 2 + 20, cell.getWidth(), cell.getHeight());
            }
            cg.dispose();
            float[] zb = new float[cell.getWidth() * cell.getHeight()];
            Arrays.fill(zb, -Float.MAX_VALUE);
            if (hand) {
                // ItemInHandRenderer: przesunięcie prawej ręki (0.56, -0.52, -0.72), FOV 70°
                // kalibracja do gry: ostrze vanilla miecza ma iść w głąb ekranu (lustro Z w przestrzeni dłoni)
                Matrix4f world = new Matrix4f().translate(0.56f, -0.52f, -0.72f).scale(1, 1, -1)
                        .mul(displayMatrix(md.display(), "firstperson_righthand"));
                float focal = (cell.getHeight() / 2f) / (float) Math.tan(Math.toRadians(35));
                render(cell, zb, md, world, true, focal, cell.getWidth() / 2f, cell.getHeight() / 2f);
            } else {
                Matrix4f world = displayMatrix(md.display(), "gui");
                render(cell, zb, md, world, false, 144, cell.getWidth() / 2f, cell.getHeight() / 2f);
                Graphics2D sg = cell.createGraphics();
                sg.setColor(new Color(0x373737));
                sg.drawRect(cell.getWidth() / 2 - 72, cell.getHeight() / 2 - 72, 144, 144);
                sg.dispose();
            }
            int x = (i % cols) * cw, y = (i / cols) * ch;
            g.drawImage(cell, x, y, null);
            g.setColor(Color.WHITE);
            g.drawString(ref.substring(ref.lastIndexOf('/') + 1), x + 4, y + (hand ? 16 : ch - 3));
        }
        g.dispose();
        ImageIO.write(sheet, "png", out);
    }

    /** Modele świata w rzucie izometrycznym (skala 1:1 - 1 blok = 64 px). */
    void worldSheet(List<String> models, File out) throws IOException {
        int cw = 200, ch = 200, cols = 7;
        int rows = Math.max(1, (models.size() + cols - 1) / cols);
        BufferedImage sheet = new BufferedImage(cols * cw, rows * ch, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        for (int i = 0; i < models.size(); i++) {
            ModelData md;
            try {
                md = load("rivals:item/" + models.get(i));
            } catch (Exception e) {
                continue;
            }
            BufferedImage cell = new BufferedImage(cw, ch - 14, BufferedImage.TYPE_INT_RGB);
            Graphics2D cg = cell.createGraphics();
            cg.setColor(new Color(0x30343C));
            cg.fillRect(0, 0, cw, ch);
            cg.dispose();
            float[] zb = new float[cell.getWidth() * cell.getHeight()];
            Arrays.fill(zb, -Float.MAX_VALUE);
            Matrix4f world = new Matrix4f().rotateX((float) Math.toRadians(30)).rotateY((float) Math.toRadians(-35))
                    .translate(-0.5f, -0.5f, -0.5f).scale(1 / 16f);
            render(cell, zb, md, world, false, 48, cw / 2f, (ch - 14) / 2f);
            int x = (i % cols) * cw, y = (i / cols) * ch;
            g.drawImage(cell, x, y, null);
            g.setColor(Color.WHITE);
            g.drawString(models.get(i).replace("world/", ""), x + 4, y + ch - 3);
        }
        g.dispose();
        ImageIO.write(sheet, "png", out);
    }

    private ModelData swordRef() throws IOException {
        JsonObject handheld = json("minecraft:item/handheld");
        BufferedImage t = texture("minecraft:item/diamond_sword");
        List<Quad> q = new ArrayList<>();
        q.add(new Quad(new Vector3f[]{v(0, 16, 8.5f), v(16, 16, 8.5f), v(16, 0, 8.5f), v(0, 0, 8.5f)},
                new float[][]{{0, 0}, {16, 0}, {16, 16}, {0, 16}}, t, true));
        q.add(new Quad(new Vector3f[]{v(16, 16, 7.5f), v(0, 16, 7.5f), v(0, 0, 7.5f), v(16, 0, 7.5f)},
                new float[][]{{16, 0}, {0, 0}, {0, 16}, {16, 16}}, t, true));
        JsonObject display = handheld.getAsJsonObject("display");
        JsonObject gui = new JsonObject();
        gui.add("rotation", Out.nums(0, 0, 0));
        gui.add("translation", Out.nums(0, 0, 0));
        gui.add("scale", Out.nums(1, 1, 1));
        display.add("gui", gui);
        return new ModelData(q, display);
    }
}
