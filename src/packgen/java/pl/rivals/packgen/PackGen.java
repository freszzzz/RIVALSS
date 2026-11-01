package pl.rivals.packgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pl.rivals.shared.Cosmetic;
import pl.rivals.shared.RivalsItem;
import pl.rivals.shared.SoundId;
import pl.rivals.shared.WorldModel;

import java.io.File;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static pl.rivals.packgen.Out.arr;
import static pl.rivals.packgen.Out.obj;
import static pl.rivals.packgen.Pal.*;

/**
 * Generator resource packa RIVALS. Uruchamiany przez Gradle (zadanie generateResourcePack).
 * Wszystko powstaje z kodu: modele low-poly (JSON zgodny z Blockbench), tekstury PNG w ustalonej
 * palecie, definicje przedmiotów, czcionki, panele, tła GUI i sounds.json.
 */
public final class PackGen {

    public static void main(String[] args) {
        File root = new File(args.length > 0 ? args[0] : "build/resourcepack");
        Out out = new Out(root);
        ModelWriter writer = new ModelWriter(out);

        // pack.mcmeta - obsługa 1.21.4 (46) do najnowszych (stary i nowy format pola wersji)
        out.json(out.file("pack.mcmeta"), obj("pack", obj(
                "description", "RIVALS · tryb 1v1",
                "pack_format", 46,
                "supported_formats", obj("min_inclusive", 46, "max_inclusive", 99),
                "min_format", 46,
                "max_format", 99)));
        packIcon().save(out.file("pack.png"));

        // przedmioty w ręce + skórki
        Map<RivalsItem, Img> icons = new EnumMap<>(RivalsItem.class);
        for (HandItems.ItemSet set : HandItems.all()) {
            writeSet(out, writer, set, set.id, null);
            RivalsItem item = RivalsItem.byId(set.id);
            if (item != null) icons.put(item, ModelWriter.icon(set.base));
        }
        for (Cosmetic c : Cosmetic.of(Cosmetic.Type.SKIN)) {
            HandItems.ItemSet set = HandItems.all().stream().filter(s -> s.id.equals(c.weapon.id)).findFirst().orElseThrow();
            writeSet(out, writer, set, c.weapon.id + "__" + c.id, c.color | 0xFF000000);
        }

        // modele świata i UI
        for (Map.Entry<WorldModel, Model> e : WorldModels.all().entrySet()) {
            String ref = writer.write(e.getValue());
            out.json(out.asset("rivals", "items/" + e.getKey().path + ".json"), obj("model", model(ref)));
        }

        PixelFont.generate(out);
        UiAssets.generate(out, icons);
        sounds(out);
        System.out.println("RIVALS: resource pack wygenerowany w " + root.getAbsolutePath());
    }

    // zestawy modeli
    /** Zapisuje modele zestawu (z opcjonalną skórką) i definicję przedmiotu items/&lt;name&gt;.json. */
    private static void writeSet(Out out, ModelWriter writer, HandItems.ItemSet set, String name, Integer skinColor) {
        Model base = skinColor == null ? set.base.copy(name) : recolor(set.base, name, skinColor);
        String baseRef = writer.write(base);
        String cdRef = writer.write(base.cooldownVariant());
        Map<String, String> v = new LinkedHashMap<>();
        for (Map.Entry<String, Model> e : set.variants.entrySet()) {
            String vn = name + "_" + e.getKey();
            Model vm = skinColor == null ? e.getValue().copy(vn) : recolor(e.getValue(), vn, skinColor);
            v.put(e.getKey(), writer.write(vm));
        }

        JsonObject cooldown = cooldown(baseRef, cdRef);
        JsonObject def = switch (set.def) {
            case BASIC -> cooldown;
            case BOW -> obj("type", "minecraft:condition", "property", "minecraft:using_item",
                    "on_true", range("minecraft:use_duration", 0.05f, model(v.get("pull0")),
                            0.45f, model(v.get("pull1")), 0.85f, model(v.get("pull2"))),
                    "on_false", cooldown);
            case HAMMER -> obj("type", "minecraft:condition", "property", "minecraft:using_item",
                    "on_true", range("minecraft:use_duration", 0.05f, model(v.get("c0")),
                            0.5f, model(v.get("c1")), 1.2f, model(v.get("c2"))),
                    "on_false", cooldown);
            case CROSSBOW -> obj("type", "minecraft:select", "property", "minecraft:charge_type",
                    "cases", arr(obj("when", "arrow", "model", model(v.get("charged"))),
                            obj("when", "rocket", "model", model(v.get("charged")))),
                    "fallback", obj("type", "minecraft:condition", "property", "minecraft:using_item",
                            "on_true", range("minecraft:crossbow/pull", 0, model(v.get("pull0")),
                                    0.58f, model(v.get("pull1")), 1.0f, model(v.get("pull2"))),
                            "on_false", cooldown));
            // bęben obraca się zaraz po strzale (cooldown 1.0 -> 0.9), potem przygaszony model
            case NET -> obj("type", "minecraft:range_dispatch", "property", "minecraft:cooldown",
                    "entries", arr(
                            obj("threshold", 0.01f, "model", model(cdRef)),
                            obj("threshold", 0.9f, "model", model(v.get("cd_rot45"))),
                            obj("threshold", 0.95f, "model", model(v.get("cd_rot22")))),
                    "fallback", model(baseRef));
        };
        out.json(out.asset("rivals", "items/" + name + ".json"), obj("model", def));
    }

    /** Skórka: akcent -> kolor skórki, jasne -> rozjaśniony, stal/grafit -> przyciemniony. */
    private static Model recolor(Model m, String name, int skin) {
        return m.recolor(name, c -> {
            int a = c & 0xFF000000;
            int rgb = c | 0xFF000000;
            int res;
            if (rgb == ACCENT || rgb == ACCENT_DARK || rgb == ICE) res = rgb == ACCENT_DARK ? Pal.darker(skin, 0.3) : skin;
            else if (rgb == LIGHT || rgb == WHITE || rgb == BODY) res = Pal.mix(skin, WHITE, 0.55);
            else if (rgb == BODY2) res = Pal.mix(skin, WHITE, 0.2);
            else if (rgb == TRIM) res = Pal.mix(skin, INK, 0.45);
            else if (rgb == STEEL) res = Pal.mix(skin, INK, 0.6);
            else if (rgb == GRAPHITE) res = Pal.mix(skin, INK, 0.75);
            else res = rgb;
            return (res & 0x00FFFFFF) | a;
        });
    }

    private static JsonObject model(String ref) {
        return obj("type", "minecraft:model", "model", ref);
    }

    private static JsonObject cooldown(String base, String cd) {
        return obj("type", "minecraft:range_dispatch", "property", "minecraft:cooldown",
                "entries", arr(obj("threshold", 0.01f, "model", model(cd))),
                "fallback", model(base));
    }

    /** range_dispatch: fallback + pary (próg, model). */
    private static JsonObject range(String property, float scale, JsonObject fallback, Object... entries) {
        JsonArray arr = new JsonArray();
        for (int i = 0; i + 1 < entries.length; i += 2) arr.add(obj("threshold", entries[i], "model", entries[i + 1]));
        JsonObject o = obj("type", "minecraft:range_dispatch", "property", property, "entries", arr, "fallback", fallback);
        if (scale != 0) o.addProperty("scale", scale);
        return o;
    }

    // dźwięki
    private static void sounds(Out out) {
        JsonObject root = new JsonObject();
        for (SoundId s : SoundId.values()) {
            root.add(s.id, obj("sounds", arr(obj("name", "minecraft:" + s.vanillaEvent, "type", "event",
                    "pitch", s.pitch, "volume", s.volume)), "subtitle", "subtitles.rivals." + s.id));
        }
        out.json(out.asset("rivals", "sounds.json"), root);
        JsonObject lang = new JsonObject();
        for (SoundId s : SoundId.values()) lang.addProperty("subtitles.rivals." + s.id, "RIVALS: " + s.id);
        out.json(out.asset("rivals", "lang/en_us.json"), lang);
        out.json(out.asset("rivals", "lang/pl_pl.json"), lang);
    }

    // ikona packa
    private static Img packIcon() {
        Img img = new Img(64, 64);
        img.roundRect(0, 0, 64, 64, 10, DARK, STEEL, true, true);
        String[] r = {
                "#####.", "#....#", "#....#", "#####.", "#..#..", "#...#.", "#....#"};
        for (int y = 0; y < r.length; y++)
            for (int x = 0; x < r[y].length(); x++)
                if (r[y].charAt(x) == '#') {
                    img.fill(14 + x * 6 + 2, 11 + y * 6 + 2, 6, 6, ACCENT_DARK);
                    img.fill(14 + x * 6, 11 + y * 6, 6, 6, ACCENT);
                }
        return img;
    }
}
