package pl.rivals.config;

/** Parametry animacji niszczenia / odbudowy (sekcja animation w config.yml). */
public record AnimationSettings(
        double crackCenterSpeedup,
        double waveTicksPerBlock,
        int fragmentsMin,
        int fragmentsMax,
        int fragmentLife,
        float sizeMin,
        float sizeMax,
        float speed,
        float gravity,
        int keyframeTicks,
        int growTicks,
        int maxBreakSoundsPerTick,
        int maxFragments,
        int cleanupTicks,
        int maxRebuildDisplays
) {
}
