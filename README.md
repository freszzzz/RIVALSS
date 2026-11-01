# RIVALS

Tryb 1v1 na serwery Minecraft Java: krótkie pojedynki na rundy, w których przed każdą rundą
wybierasz broń i gadżety. Plugin na Paper oraz generowany resource pack (modele 3D, HUD, GUI, dźwięki).

Serwer wysyła pack automatycznie, gracz nie musi niczego instalować ani mieć modów.

## Rozgrywka

- mecz do 5 wygranych rund, runda trwa 90 s,
- od 60. sekundy pierścienie areny zapadają się (sudden death),
- po rundzie killcam, po meczu podsumowanie, monety i zmiana ELO,
- 9 broni i 16 gadżetów, każdy z własnym modelem, wariantem cooldownu, ikoną, dźwiękiem i opisem,
- 3 areny: Laboratorium, Neonowy dach, Stacja orbitalna,
- kapsuła bonusowa i monety na arenie,
- sklep: skórki broni, efekty zabójstwa, ślady doskoku,
- lobby z rankingiem, postumentami i NPC.

## Wymagania

| | |
|---|---|
| Serwer | Paper 1.21.4 lub nowszy (sprawdzane do 1.21.11) |
| Java | 21 |
| Klient | Minecraft Java w wersji serwera |

## Budowanie

```bash
./gradlew build
```

Wynik:

- `build/libs/Rivals-<wersja>.jar` - plugin (z wbudowanym resource packiem),
- `build/resourcepack/` - wygenerowany pack.

Zgodność z nowszym API:

```bash
./gradlew compileJava "-PpaperApi=1.21.11-R0.1-SNAPSHOT"
```

## Instalacja

1. Wrzuć jar do `plugins/` i uruchom serwer.
2. Plugin udostępnia pack przez własny serwer HTTP na porcie `8164`. Port musi być osiągalny dla graczy.
   Za NAT-em albo na hostingu ustaw w `config.yml` zewnętrzny adres `resource-pack.url`
   (np. zip wgrany na mc-packs.net). Suma SHA-1 jest liczona automatycznie.
3. Ustaw lobby komendą `/rivals admin setlobby`.

## Komendy

| Komenda | Opis |
|---|---|
| `/rivals graj` | dołączenie do kolejki |
| `/rivals opusc` | wyjście z kolejki lub meczu |
| `/rivals statystyki [gracz]` | statystyki |
| `/rivals zestaw` | wybór zestawu |
| `/rivals sklep` | sklep |
| `/rivals pack` | ponowne wysłanie packa |
| `/rivals test` | mecz treningowy z botem (`rivals.test`) |
| `/rivals admin` | administracja: `setlobby`, `reload`, `forcestart`, `stop`, `arenas`, `lobby`, `coins`, `wallpreview` (`rivals.admin`) |

Alias: `/rv`.

## Konfiguracja

- `config.yml` - wszystkie wartości liczbowe: czasy, obrażenia, cooldowny, nagrody, limity wydajności,
- `arenas.yml` - areny opisane prostym językiem bloków (struktury i dekoracje),
- `messages.yml` - teksty, z tagami MiniMessage i `<icon:...>`.

Dane graczy są w SQLite: `plugins/Rivals/`.

## Struktura projektu

```
src/main/java      plugin
src/packgen/java   generator resource packa (nie trafia do jara)
src/shared/java    stałe wspólne: glify, lista przedmiotów, dźwięki, kosmetyki
src/main/resources plugin.yml i domyślne konfiguracje
```

Najważniejsze pakiety pluginu:

| Pakiet | Zawartość |
|---|---|
| `match` | maszyna stanów meczu, kolejka, ELO, bot treningowy |
| `combat` | obrażenia, cooldowny, symulowane pociski, efekty czasowe |
| `weapon`, `gadget` | bronie i gadżety |
| `arena` | budowanie aren w pustym świecie, dekoracje |
| `anim` | animacja niszczenia, także dla sudden death |
| `hud`, `gui` | sidebar, bossbar, tytuły, menu |
| `data` | SQLite na osobnym wątku, profile, rangi |
| `pack` | wysyłanie packa |

Resource pack w całości generuje kod: modele składane z prostopadłościanów, atlas tekstur z paletą kolorów,
automatyczne warianty cooldownu i skórek, definicje przedmiotów, czcionki z glifami HUD.

## Narzędzia deweloperskie

Wymagają katalogu `assets/minecraft` wypakowanego z klienta gry. Nie jest on częścią repozytorium.

```bash
./gradlew previewModels "-Pvanilla=<katalog>"
```

```bash
./gradlew hudMock "-Pvanilla=<katalog>"
```

## Technologie

Java 21, Gradle (Kotlin DSL), Paper API, Adventure, SQLite. Generator packa: Gson, JOML.
