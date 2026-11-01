package pl.rivals.arena;

import java.util.List;

/** Definicja areny z arenas.yml (bez położenia w świecie). */
public record ArenaTemplate(
        String id,
        String displayName,
        boolean enabled,
        double gravity,
        long time,
        int half,
        int spawnDistance,
        int wallWidth,
        int wallHeight,
        Blueprint blueprint,
        List<Decoration> decorations
) {

    /** Dekoracja (display entities) - współrzędne względem środka areny, środek bloku. */
    public record Decoration(String type, double x, double y, double z, float yaw) {
    }
}
