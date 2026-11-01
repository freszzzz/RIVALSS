package pl.rivals.anim;

import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pojedynczy odłamek (block_display). Byt stoi w miejscu - ruch to tylko zmiana transformacji.
 * Fizyka (grawitacja, odbicie od podłogi, obrót) liczona w pluginie, ale wysyłana co
 * keyframe ticków jako stan docelowy; klient interpoluje płynnie pomiędzy klatkami.
 */
final class Fragment {

    private final BlockDisplay display;
    private final World world;
    private final double bx;
    private final double by;
    private final double bz;
    private final Vector3f pos;
    private final Vector3f vel;
    private final Quaternionf rot;
    private final Vector3f axis;
    private float spin;
    private final float size;
    private final int life;
    private final int keyframe;
    private final float gravity;
    private int age;

    Fragment(BlockDisplay display, Vector3f offset, Vector3f velocity, Quaternionf rotation, Vector3f axis,
             float spin, float size, int life, int keyframe, float gravity) {
        this.display = display;
        this.world = display.getWorld();
        this.bx = display.getLocation().getX();
        this.by = display.getLocation().getY();
        this.bz = display.getLocation().getZ();
        this.pos = new Vector3f(offset);
        this.vel = new Vector3f(velocity);
        this.rot = new Quaternionf(rotation);
        this.axis = new Vector3f(axis);
        this.spin = spin;
        this.size = size;
        this.life = life;
        this.keyframe = keyframe;
        this.gravity = gravity;
    }

    /** @return false, gdy odłamek skończył życie i został usunięty. */
    boolean tick() {
        age++;
        if (!display.isValid()) return false;
        if (age > life) {
            display.remove();
            return false;
        }
        if ((age - 1) % keyframe == 0) {
            int steps = Math.max(1, Math.min(keyframe, life - age + 1));
            for (int i = 0; i < steps; i++) step();
            int t = age - 1 + steps;
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(steps);
            display.setTransformation(transform(pos, rot, size * scaleAt(t)));
        }
        return true;
    }

    void remove() {
        if (display.isValid()) display.remove();
    }

    /** Pełny rozmiar przez ~45% życia, potem liniowo do zera. */
    private float scaleAt(int t) {
        float start = life * 0.45f;
        if (t <= start) return 1f;
        return Math.max(0f, 1f - (t - start) / (life - start));
    }

    private void step() {
        vel.y -= gravity;
        vel.mul(0.985f);
        float nx = pos.x + vel.x;
        float ny = pos.y + vel.y;
        float nz = pos.z + vel.z;
        if (vel.y < 0 && solid(bx + nx, by + ny - size * 0.5, bz + nz)) {
            ny = pos.y;
            vel.y = -vel.y * 0.3f;
            vel.x *= 0.55f;
            vel.z *= 0.55f;
            spin *= 0.6f;
        }
        pos.set(nx, ny, nz);
        new Quaternionf().fromAxisAngleRad(axis, spin).mul(rot, rot);
    }

    private boolean solid(double x, double y, double z) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iz = (int) Math.floor(z);
        if (!world.isChunkLoaded(ix >> 4, iz >> 4)) return false;
        return world.getBlockAt(ix, iy, iz).getType().isSolid();
    }

    /** Transformacja kostki o boku s obróconej wokół własnego środka, który leży w pos. */
    static Transformation transform(Vector3f pos, Quaternionf rot, float s) {
        Vector3f half = new Vector3f(s * 0.5f);
        rot.transform(half);
        return new Transformation(new Vector3f(pos).sub(half), new Quaternionf(rot), new Vector3f(s), new Quaternionf());
    }
}
