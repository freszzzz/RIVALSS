package pl.rivals.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import pl.rivals.hud.Hud;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Wiadomości z messages.yml (MiniMessage). Brakujące klucze uzupełniane z pliku w jarze. */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Map<String, String> raw;

    private Messages(Map<String, String> raw) {
        this.raw = raw;
    }

    public static Messages load(Plugin plugin, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        InputStream in = plugin.getResource("messages.yml");
        if (in != null) {
            yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            yaml.options().copyDefaults(true);
        }
        Map<String, String> map = new HashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (yaml.isList(key)) map.put(key, String.join("\n", yaml.getStringList(key)));
            else if (yaml.isString(key)) map.put(key, yaml.getString(key));
        }
        return new Messages(map);
    }

    public static MiniMessage mm() {
        return MM;
    }

    public static TagResolver ph(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }

    public static TagResolver phc(String key, Component value) {
        return Placeholder.component(key, value);
    }

    public String raw(String key) {
        return raw.getOrDefault(key, "<red>" + key);
    }

    public Component get(String key, TagResolver... resolvers) {
        return MM.deserialize(raw(key), withIcons(resolvers));
    }

    public Component prefixed(String key, TagResolver... resolvers) {
        return get("prefix").append(get(key, resolvers));
    }

    public List<Component> lines(String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : raw(key).split("\n")) out.add(MM.deserialize(line, withIcons(resolvers)));
        return out;
    }

    private static TagResolver withIcons(TagResolver... resolvers) {
        return TagResolver.resolver(TagResolver.resolver(resolvers), Hud.ICON_TAG);
    }

    /** MiniMessage z tagiem <icon:...>. */
    public static Component parse(String mini, TagResolver... resolvers) {
        return MM.deserialize(mini, withIcons(resolvers));
    }

    public void send(Audience audience, String key, TagResolver... resolvers) {
        audience.sendMessage(prefixed(key, resolvers));
    }

    public void sendLines(Audience audience, String key, TagResolver... resolvers) {
        for (Component c : lines(key, resolvers)) audience.sendMessage(c);
    }
}
