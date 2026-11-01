package pl.rivals.packgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Zapis plików packa + skróty do budowania JSON-a. */
public final class Out {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public final File root;

    public Out(File root) {
        this.root = root;
    }

    public File file(String path) {
        return new File(root, path);
    }

    /** assets/&lt;ns&gt;/&lt;path&gt; */
    public File asset(String ns, String path) {
        return new File(root, "assets/" + ns + "/" + path);
    }

    public void json(File f, JsonElement e) {
        text(f, GSON.toJson(e));
    }

    public void text(File f, String s) {
        f.getParentFile().mkdirs();
        try {
            Files.writeString(f.toPath(), s, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    // JSON
    public static JsonObject obj(Object... kv) {
        JsonObject o = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) put(o, (String) kv[i], kv[i + 1]);
        return o;
    }

    public static JsonArray arr(Object... values) {
        JsonArray a = new JsonArray();
        for (Object v : values) add(a, v);
        return a;
    }

    public static JsonArray nums(float... values) {
        JsonArray a = new JsonArray();
        for (float v : values) a.add(round(v));
        return a;
    }

    static double round(float v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    public static void put(JsonObject o, String k, Object v) {
        if (v == null) return;
        if (v instanceof JsonElement e) o.add(k, e);
        else if (v instanceof String s) o.addProperty(k, s);
        else if (v instanceof Float f) o.addProperty(k, round(f));
        else if (v instanceof Number n) o.addProperty(k, n);
        else if (v instanceof Boolean b) o.addProperty(k, b);
        else throw new IllegalArgumentException("Nieobsługiwany typ: " + v.getClass());
    }

    public static void add(JsonArray a, Object v) {
        if (v instanceof JsonElement e) a.add(e);
        else if (v instanceof String s) a.add(s);
        else if (v instanceof Float f) a.add(round(f));
        else if (v instanceof Number n) a.add(n);
        else if (v instanceof Boolean b) a.add(b);
        else throw new IllegalArgumentException("Nieobsługiwany typ: " + v);
    }
}
