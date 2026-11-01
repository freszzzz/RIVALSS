package pl.rivals.combat;

import pl.rivals.match.Combatant;

/** Obiekt żyjący w trakcie rundy (pocisk, mina, tarcza, efekt statusu...). */
public interface MatchObject {

    /** @return false, gdy obiekt skończył działanie (zostanie usunięty). */
    boolean tick();

    /** Sprzątanie (usunięcie bytów, zdjęcie efektów). Wywoływane raz. */
    void remove();

    /** Uczestnik otrzymał obrażenia (np. rozbicie lodu). */
    default void onDamaged(Combatant victim, double damage) {
    }
}
