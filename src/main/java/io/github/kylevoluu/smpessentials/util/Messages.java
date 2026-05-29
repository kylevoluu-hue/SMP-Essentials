package io.github.kylevoluu.smpessentials.util;

import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;

/**
 * Reads message strings from {@code config.yml} (under {@code messages.}),
 * substitutes {@code {placeholder}} tokens, and returns Adventure components.
 */
public final class Messages {

    private final Plugin plugin;

    public Messages(Plugin plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "");
    }

    private String raw(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    private static String apply(String text, String... replacements) {
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return text;
    }

    /** Message with the configured prefix prepended. */
    public Component prefixed(String key, String... replacements) {
        return Text.of(prefix() + apply(raw(key), replacements));
    }

    /** Message without any prefix (e.g. action bar, item-related text). */
    public Component plain(String key, String... replacements) {
        return Text.of(apply(raw(key), replacements));
    }
}
