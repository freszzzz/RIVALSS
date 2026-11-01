package pl.rivals.packgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Model low-poly złożony z prostopadłościanów (format elementów Java/Blockbench).
 * Konwencja przedmiotów w ręce: przód = +X, góra = +Y, grubość wzdłuż Z (środek z = 8).
 */
public final class Model {

    public enum Kind {
        /** Broń/narzędzie trzymane poziomo, lufa w stronę +X. */
        GUN,
        /** Broń biała / przedmiot pionowy, ostrze w stronę +Y. */
        MELEE,
        /** Mały przedmiot trzymany w dłoni (granaty, kryształy). */
        SMALL,
        /** Model świata (display entity) - skala 1:1, środek (8,8,8) w punkcie bytu. */
        WORLD,
        /** Ikona GUI/lobby. */
        UI
    }

    public enum IconView { SIDE, TOP }

    public static final class Box {
        float x1, y1, z1, x2, y2, z2;
        int color;
        boolean glow;
        String anim;
        String rotAxis;
        float rotAngle;
        float ox, oy, oz;

        Box copy() {
            Box b = new Box();
            b.x1 = x1; b.y1 = y1; b.z1 = z1; b.x2 = x2; b.y2 = y2; b.z2 = z2;
            b.color = color; b.glow = glow; b.anim = anim;
            b.rotAxis = rotAxis; b.rotAngle = rotAngle; b.ox = ox; b.oy = oy; b.oz = oz;
            return b;
        }

        /** Obrót elementu (dozwolone kąty: -45, -22.5, 0, 22.5, 45). */
        public Box rot(String axis, float angle, float ox, float oy, float oz) {
            this.rotAxis = axis;
            this.rotAngle = angle;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            return this;
        }
    }

    public final String name;
    public final Kind kind;
    public final List<Box> boxes = new ArrayList<>();
    float gripX = 8, gripY = 8, gripZ = 8;
    IconView iconView = IconView.SIDE;
    /** Nadpisanie obrotu w GUI (null = domyślny dla rodzaju). */
    float[] guiRotation;
    /** Uchwyt wyrównany do środka sprite'a vanilla (łuk, małe przedmioty) zamiast rękojeści miecza. */
    boolean centerAnchor;

    public Model(String name, Kind kind) {
        this.name = name;
        this.kind = kind;
    }

    public Model grip(float x, float y, float z) {
        gripX = x;
        gripY = y;
        gripZ = z;
        return this;
    }

    public Model gui(float rx, float ry, float rz) {
        guiRotation = new float[]{rx, ry, rz};
        return this;
    }

    public Model centered() {
        centerAnchor = true;
        return this;
    }

    public Model iconTop() {
        iconView = IconView.TOP;
        return this;
    }

    public Box box(float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        Box b = new Box();
        b.x1 = Math.min(x1, x2); b.y1 = Math.min(y1, y2); b.z1 = Math.min(z1, z2);
        b.x2 = Math.max(x1, x2); b.y2 = Math.max(y1, y2); b.z2 = Math.max(z1, z2);
        b.color = color;
        boxes.add(b);
        return b;
    }

    /** Element świecący (light_emission 15) - kolor akcentu. */
    public Box glow(float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        Box b = box(x1, y1, z1, x2, y2, z2, color);
        b.glow = true;
        return b;
    }

    /** Element z animowaną teksturą (assets/rivals/textures/item/anim/&lt;anim&gt;.png). */
    public Box anim(float x1, float y1, float z1, float x2, float y2, float z2, String anim, int fallbackColor) {
        Box b = glow(x1, y1, z1, x2, y2, z2, fallbackColor);
        b.anim = anim;
        return b;
    }

    /** Pusta ramka w płaszczyźnie XZ (pierścień) na wysokości y1..y2. */
    public void ringXZ(float x1, float z1, float x2, float z2, float y1, float y2, float t, int color, boolean glow) {
        Box[] bs = {
                box(x1, y1, z1, x2, y2, z1 + t, color),
                box(x1, y1, z2 - t, x2, y2, z2, color),
                box(x1, y1, z1 + t, x1 + t, y2, z2 - t, color),
                box(x2 - t, y1, z1 + t, x2, y2, z2 - t, color)};
        for (Box b : bs) b.glow = glow;
    }

    /** Kopia z inną nazwą. */
    public Model copy(String newName) {
        Model m = new Model(newName, kind);
        for (Box b : boxes) m.boxes.add(b.copy());
        m.gripX = gripX; m.gripY = gripY; m.gripZ = gripZ;
        m.iconView = iconView;
        m.guiRotation = guiRotation;
        m.centerAnchor = centerAnchor;
        return m;
    }

    /** Wariant cooldownu: świecące elementy przygasają (bez emisji, przyciemniony kolor). */
    public Model cooldownVariant() {
        Model m = copy(name + "_cd");
        for (Box b : m.boxes) {
            if (b.glow) {
                b.glow = false;
                b.anim = null;
                b.color = Pal.mix(b.color, Pal.STEEL, 0.7) | (b.color & 0xFF000000);
            }
        }
        return m;
    }

    /** Podmiana kolorów (skórki). */
    public Model recolor(String newName, IntUnaryOperator map) {
        Model m = copy(newName);
        for (Box b : m.boxes) b.color = map.applyAsInt(b.color);
        return m;
    }
}
