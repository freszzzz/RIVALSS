package pl.rivals.arena;

/** Pozycja bloku (względna wobec środka areny albo absolutna w świecie - zależnie od kontekstu). */
public record BlockVec(int x, int y, int z) {

    /** Odległość Czebyszewa w poziomie - numer "pierścienia" areny. */
    public int ring() {
        return Math.max(Math.abs(x), Math.abs(z));
    }

    public BlockVec offset(int dx, int dy, int dz) {
        return new BlockVec(x + dx, y + dy, z + dz);
    }

    public BlockVec mirrorZ() {
        return new BlockVec(x, y, -z);
    }

    public BlockVec mirrorX() {
        return new BlockVec(-x, y, z);
    }
}
