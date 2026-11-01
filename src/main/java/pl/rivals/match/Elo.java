package pl.rivals.match;

/** Klasyczny ranking ELO. */
public final class Elo {

    private Elo() {
    }

    /** @return o ile punktów zmienia się ranking (zwycięzca +delta, przegrany -delta). */
    public static int delta(int winnerElo, int loserElo, int k) {
        double expected = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));
        return Math.max(1, (int) Math.round(k * (1.0 - expected)));
    }
}
